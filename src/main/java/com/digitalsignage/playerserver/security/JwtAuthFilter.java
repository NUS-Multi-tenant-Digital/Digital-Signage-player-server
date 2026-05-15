package com.digitalsignage.playerserver.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    // 不需要鉴权的路径
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/health",
            "/api/v1/player/register"
    );

    public JwtAuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // 公开接口直接放行
        if (PUBLIC_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 提取 Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, 401, "UNAUTHORIZED", "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        Claims claims = jwtService.validateToken(token);

        if (claims == null) {
            sendError(response, 401, "TOKEN_INVALID", "Token is invalid or expired");
            return;
        }

        // 将设备信息放入 request attributes，供后续 controller 使用
        request.setAttribute("deviceId", claims.getSubject());
        request.setAttribute("tenantId", claims.get("tenant_id", String.class));

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
                "success", false,
                "error", Map.of("code", code, "message", message)
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
