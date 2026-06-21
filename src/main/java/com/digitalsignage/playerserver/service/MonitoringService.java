package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.entity.Screen;
import com.digitalsignage.playerserver.repository.ScreenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MonitoringService {

    private final ScreenRepository screenRepository;
    private final RedisOperations<String, Object> redisOperations;
    private final ObjectMapper objectMapper;

    public MonitoringService(ScreenRepository screenRepository,
                             RedisOperations<String, Object> redisOperations,
                             ObjectMapper objectMapper) {
        this.screenRepository = screenRepository;
        this.redisOperations = redisOperations;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getDeviceMonitoring(String deviceCode) {
        Screen screen = screenRepository.findByDeviceCode(deviceCode).orElse(null);
        if (screen == null) {
            return null;
        }

        Map<Object, Object> hb = redisOperations.opsForHash().entries("heartbeat:" + deviceCode);
        Map<Object, Object> sync = redisOperations.opsForHash().entries("sync_state:" + deviceCode);
        boolean online = Boolean.TRUE.equals(redisOperations.hasKey("device:online:" + deviceCode));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("device_id", deviceCode);
        result.put("screen_id", screen.getId());
        result.put("name", screen.getName());
        result.put("online", online);
        result.put("app_version", screen.getAppVersion());
        result.put("last_heartbeat_at", hb.get("lastHeartbeatAt"));
        result.put("manifest_id", hb.get("manifestId"));
        result.put("manifest_version", hb.get("manifestVersion"));

        result.put("health", parseJson(hb.get("health")));
        result.put("cache", parseJson(hb.get("cache")));
        result.put("network", parseJson(hb.get("network")));
        result.put("playback", parseJson(hb.get("playback")));

        result.put("last_content_sync_at", sync.get("lastSyncAt"));
        result.put("last_online_at", sync.get("lastOnlineAt"));

        // Screenshot preview is not implemented yet (no capture/storage pipeline)
        result.put("screenshot", null);

        return result;
    }

    private Object parseJson(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String s) {
            try {
                return objectMapper.readValue(s, Object.class);
            } catch (Exception e) {
                return s;
            }
        }
        return raw;
    }
}
