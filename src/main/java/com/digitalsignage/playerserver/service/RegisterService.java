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

        // 用 device_sn 作为稳定的 device_code，实现按设备幂等：同一台设备反复注册复用同一条 screen
        String deviceCode = (req.getDeviceSn() != null && !req.getDeviceSn().isEmpty())
                ? req.getDeviceSn()
                : UUID.randomUUID().toString();
        Long organizationId = parseOrganizationId(req.getTenantId());

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

        // 幂等：已存在则复用，不存在则新建
        Screen screen = screenRepository.findByDeviceCode(deviceCode).orElseGet(Screen::new);
        boolean isNew = screen.getId() == null;

        if (isNew) {
            screen.setDeviceCode(deviceCode);
            screen.setDeviceToken(generateRandomHexToken());
            screen.setCreatedAt(LocalDateTime.now());
        }
        screen.setOrganizationId(organizationId);
        screen.setName(req.getDeviceName());
        screen.setActivationCode(req.getActivationCode());
        screen.setActivationStatus("activated");
        screen.setStatus("active");
        screen.setAppVersion(req.getAppVersion());
        if (resolutionWidth != null) {
            screen.setResolutionWidth(resolutionWidth);
        }
        if (resolutionHeight != null) {
            screen.setResolutionHeight(resolutionHeight);
        }
        screen.setLastHeartbeatAt(LocalDateTime.now());
        screen.setUpdatedAt(LocalDateTime.now());

        screenRepository.save(screen);

        // 复用已有 PlayerConfig，没有才创建默认配置
        PlayerConfig config = playerConfigRepository.findByScreenId(screen.getId())
                .orElseGet(() -> {
                    PlayerConfig c = new PlayerConfig();
                    c.setScreenId(screen.getId());
                    c.setHeartbeatIntervalSec(30);
                    c.setManifestSyncIntervalSec(60);
                    c.setEventFlushIntervalSec(30);
                    c.setMaxCacheSizeMb(2048);
                    c.setAssetDownloadConcurrency(3);
                    c.setEnableOfflineMode(true);
                    c.setEnableWatchdog(true);
                    c.setEnableScreenshot(false);
                    c.setLogLevel("info");
                    c.setSupportedAssetTypesJson("[\"ASSET_VIDEO\",\"ASSET_IMAGE\"]");
                    c.setCreatedAt(LocalDateTime.now());
                    c.setUpdatedAt(LocalDateTime.now());
                    return playerConfigRepository.save(c);
                });

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
