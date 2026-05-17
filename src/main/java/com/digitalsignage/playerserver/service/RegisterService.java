package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.RegisterPlayerRequest;
import com.digitalsignage.playerserver.dto.response.RegisterPlayerResponse;
import com.digitalsignage.playerserver.entity.Screen;
import com.digitalsignage.playerserver.entity.PlayerConfig;
import com.digitalsignage.playerserver.repository.ScreenRepository;
import com.digitalsignage.playerserver.repository.PlayerConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RegisterService {

    private final ScreenRepository screenRepository;
    private final PlayerConfigRepository playerConfigRepository;
    private final RedisOperations<String, Object> redisOperations;
    private final ObjectMapper objectMapper;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public RegisterService(ScreenRepository screenRepository,
                           PlayerConfigRepository playerConfigRepository,
                           RedisOperations<String, Object> redisOperations,
                           ObjectMapper objectMapper) {
        this.screenRepository = screenRepository;
        this.playerConfigRepository = playerConfigRepository;
        this.redisOperations = redisOperations;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RegisterPlayerResponse register(RegisterPlayerRequest req) {
        long now = System.currentTimeMillis();

        String deviceCode = UUID.randomUUID().toString();
        Long organizationId = parseOrganizationId(req.getTenantId());

        // Generate random 64-char hex token (32 random bytes → hex string)
        String deviceToken = generateRandomHexToken();

        // Parse resolution from "WIDTHxHEIGHT" format
        Integer resolutionWidth = null;
        Integer resolutionHeight = null;
        if (req.getScreenResolution() != null && req.getScreenResolution().contains("x")) {
            String[] parts = req.getScreenResolution().split("x");
            try {
                resolutionWidth = Integer.parseInt(parts[0]);
                resolutionHeight = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {}
        }

        Screen screen = new Screen();
        screen.setDeviceCode(deviceCode);
        screen.setOrganizationId(organizationId);
        screen.setName(req.getDeviceName());
        screen.setActivationCode(req.getActivationCode());
        screen.setActivationStatus("activated");
        screen.setDeviceToken(deviceToken);
        screen.setStatus("active");
        screen.setAppVersion(req.getAppVersion());
        screen.setResolutionWidth(resolutionWidth);
        screen.setResolutionHeight(resolutionHeight);
        screen.setLastHeartbeatAt(LocalDateTime.now());
        screen.setCreatedAt(LocalDateTime.now());
        screen.setUpdatedAt(LocalDateTime.now());

        screenRepository.save(screen);

        PlayerConfig config = new PlayerConfig();
        config.setScreenId(screen.getId());
        config.setHeartbeatIntervalSec(30);
        config.setManifestSyncIntervalSec(60);
        config.setEventFlushIntervalSec(30);
        config.setMaxCacheSizeMb(2048);
        config.setAssetDownloadConcurrency(3);
        config.setEnableOfflineMode(true);
        config.setEnableWatchdog(true);
        config.setEnableScreenshot(false);
        config.setLogLevel("info");
        config.setSupportedAssetTypesJson("[\"ASSET_VIDEO\",\"ASSET_IMAGE\"]");
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());

        playerConfigRepository.save(config);

        // Save initial sync state to Redis
        String syncKey = "sync_state:" + screen.getDeviceCode();
        Map<String, Object> syncState = new HashMap<>();
        syncState.put("deviceCode", screen.getDeviceCode());
        syncState.put("screenId", screen.getId());
        syncState.put("manifestVersion", 0);
        syncState.put("lastOnlineAt", now);
        redisOperations.opsForHash().putAll(syncKey, syncState);

        // Build response - keep external API contract (device_id maps to deviceCode)
        RegisterPlayerResponse resp = new RegisterPlayerResponse();
        resp.setDeviceId(screen.getDeviceCode());
        resp.setTenantId(String.valueOf(screen.getOrganizationId()));
        resp.setAccessToken(screen.getDeviceToken());
        resp.setTokenExpireAt(0);

        RegisterPlayerResponse.PlayerConfigDto configDto = new RegisterPlayerResponse.PlayerConfigDto();
        configDto.setDeviceId(screen.getDeviceCode());
        configDto.setHeartbeatIntervalSec(config.getHeartbeatIntervalSec());
        configDto.setManifestSyncIntervalSec(config.getManifestSyncIntervalSec());
        configDto.setEventFlushIntervalSec(config.getEventFlushIntervalSec());
        configDto.setMaxCacheSizeMb((int) config.getMaxCacheSizeMb());
        configDto.setAssetDownloadConcurrency(config.getAssetDownloadConcurrency());
        configDto.setEnableOfflineMode(config.isEnableOfflineMode());
        configDto.setEnableWatchdog(config.isEnableWatchdog());
        configDto.setEnableScreenshot(config.isEnableScreenshot());
        configDto.setLogLevel(config.getLogLevel());
        configDto.setSupportedAssetTypes(Arrays.asList("ASSET_VIDEO", "ASSET_IMAGE"));
        resp.setConfig(configDto);

        return resp;
    }

    private static String generateRandomHexToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private Long parseOrganizationId(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return 1L;
        }
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            return 1L;
        }
    }
}
