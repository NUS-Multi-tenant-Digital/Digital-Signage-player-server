package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.RegisterPlayerRequest;
import com.digitalsignage.playerserver.dto.response.RegisterPlayerResponse;
import com.digitalsignage.playerserver.entity.Device;
import com.digitalsignage.playerserver.entity.PlayerConfig;
import com.digitalsignage.playerserver.repository.DeviceRepository;
import com.digitalsignage.playerserver.repository.PlayerConfigRepository;
import com.digitalsignage.playerserver.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RegisterService {

    private final DeviceRepository deviceRepository;
    private final PlayerConfigRepository playerConfigRepository;
    private final RedisOperations<String, Object> redisOperations;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;

    public RegisterService(DeviceRepository deviceRepository,
                           PlayerConfigRepository playerConfigRepository,
                           RedisOperations<String, Object> redisOperations,
                           ObjectMapper objectMapper,
                           JwtService jwtService) {
        this.deviceRepository = deviceRepository;
        this.playerConfigRepository = playerConfigRepository;
        this.redisOperations = redisOperations;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterPlayerResponse register(RegisterPlayerRequest req) {
        long now = System.currentTimeMillis();

        String deviceId = UUID.randomUUID().toString();
        String tenantId = req.getTenantId() != null ? req.getTenantId() : "default_tenant";

        // Generate JWT token
        String accessToken = jwtService.generateToken(deviceId, tenantId);
        long tokenExpireAt = now + jwtService.getTokenValidityMs();

        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setTenantId(tenantId);
        device.setLocationId(req.getLocationId() != null ? req.getLocationId() : "default_location");
        device.setDeviceSn(req.getDeviceSn());
        device.setActivationCode(req.getActivationCode());
        device.setDeviceName(req.getDeviceName());
        device.setPlatform(req.getPlatform());
        device.setAppVersion(req.getAppVersion());
        device.setOsVersion(req.getOsVersion());
        device.setFirmwareVersion(req.getFirmwareVersion());
        device.setScreenResolution(req.getScreenResolution());
        device.setTimezone(req.getTimezone());
        device.setMacAddress(req.getMacAddress());
        device.setIpAddress(req.getIpAddress());
        try {
            device.setCapabilitiesJson(objectMapper.writeValueAsString(
                    req.getCapabilities() != null ? req.getCapabilities() : Map.of()));
        } catch (Exception e) {
            device.setCapabilitiesJson("{}");
        }
        device.setAccessToken(accessToken);
        device.setTokenExpireAt(tokenExpireAt);
        device.setStatus("active");
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());

        deviceRepository.save(device);

        PlayerConfig config = new PlayerConfig();
        config.setDeviceId(device.getDeviceId());
        config.setTenantId(device.getTenantId());
        config.setLocationId(device.getLocationId());
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
        String syncKey = "sync_state:" + device.getDeviceId();
        Map<String, Object> syncState = new HashMap<>();
        syncState.put("deviceId", device.getDeviceId());
        syncState.put("manifestVersion", 0);
        syncState.put("lastOnlineAt", now);
        redisOperations.opsForHash().putAll(syncKey, syncState);

        // Build response
        RegisterPlayerResponse resp = new RegisterPlayerResponse();
        resp.setDeviceId(device.getDeviceId());
        resp.setTenantId(device.getTenantId());
        resp.setLocationId(device.getLocationId());
        resp.setAccessToken(device.getAccessToken());
        resp.setTokenExpireAt(device.getTokenExpireAt());

        RegisterPlayerResponse.PlayerConfigDto configDto = new RegisterPlayerResponse.PlayerConfigDto();
        configDto.setDeviceId(config.getDeviceId());
        configDto.setTenantId(config.getTenantId());
        configDto.setLocationId(config.getLocationId());
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
}
