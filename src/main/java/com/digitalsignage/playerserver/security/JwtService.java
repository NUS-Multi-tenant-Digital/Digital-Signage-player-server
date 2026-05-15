package com.digitalsignage.playerserver.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long tokenValidityMs;

    public JwtService(@Value("${app.jwt-secret:0123456789abcdef0123456789abcdef}") String secret,
                      @Value("${app.jwt-expiration-days:7}") int expirationDays) {
        // Ensure key is at least 32 bytes for HS256
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.tokenValidityMs = expirationDays * 24L * 60 * 60 * 1000;
    }

    /**
     * 生成 JWT token
     */
    public String generateToken(String deviceId, String tenantId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(deviceId)
                .claims(Map.of(
                        "tenant_id", tenantId,
                        "type", "device"
                ))
                .issuedAt(new Date(now))
                .expiration(new Date(now + tokenValidityMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 验证并解析 token，返回 Claims；无效则返回 null
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 从 token 中提取 device_id
     */
    public String getDeviceId(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 获取 token 过期时间戳
     */
    public long getTokenExpireAt(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.getExpiration().getTime() : 0;
    }

    public long getTokenValidityMs() {
        return tokenValidityMs;
    }
}
