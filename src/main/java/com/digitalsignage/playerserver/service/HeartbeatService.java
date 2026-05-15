package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.HeartbeatRequest;
import com.digitalsignage.playerserver.dto.response.HeartbeatResponse;
import com.digitalsignage.playerserver.entity.Command;
import com.digitalsignage.playerserver.entity.PlayerConfig;
import com.digitalsignage.playerserver.repository.CommandRepository;
import com.digitalsignage.playerserver.repository.PlayerConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class HeartbeatService {

    private final PlayerConfigRepository playerConfigRepository;
    private final CommandRepository commandRepository;
    private final RedisOperations<String, Object> redisOperations;
    private final ObjectMapper objectMapper;

    public HeartbeatService(PlayerConfigRepository playerConfigRepository,
                            CommandRepository commandRepository,
                            RedisOperations<String, Object> redisOperations,
                            ObjectMapper objectMapper) {
        this.playerConfigRepository = playerConfigRepository;
        this.commandRepository = commandRepository;
        this.redisOperations = redisOperations;
        this.objectMapper = objectMapper;
    }

    public HeartbeatResponse reportHeartbeat(HeartbeatRequest req) {
        long now = System.currentTimeMillis();

        // Get heartbeat interval from config (needed for TTL calculation)
        PlayerConfig config = playerConfigRepository.findByDeviceId(req.getDeviceId()).orElse(null);
        int nextIntervalSec = config != null ? config.getHeartbeatIntervalSec() : 30;

        // Save heartbeat to Redis with TTL = 3x heartbeat interval
        // If no heartbeat arrives within this window, the key expires → device is considered offline
        String hbKey = "heartbeat:" + req.getDeviceId();
        Map<String, Object> hbData = new LinkedHashMap<>();
        hbData.put("deviceId", req.getDeviceId());
        hbData.put("lastHeartbeatAt", req.getTimestamp());
        hbData.put("appVersion", req.getAppVersion());
        hbData.put("manifestId", req.getManifestId());
        hbData.put("manifestVersion", req.getManifestVersion());
        hbData.put("updatedAt", now);
        try {
            if (req.getPlayback() != null) hbData.put("playback", objectMapper.writeValueAsString(req.getPlayback()));
            if (req.getHealth() != null) hbData.put("health", objectMapper.writeValueAsString(req.getHealth()));
            if (req.getCache() != null) hbData.put("cache", objectMapper.writeValueAsString(req.getCache()));
            if (req.getNetwork() != null) hbData.put("network", objectMapper.writeValueAsString(req.getNetwork()));
        } catch (Exception ignored) {}
        redisOperations.opsForHash().putAll(hbKey, hbData);
        redisOperations.expire(hbKey, nextIntervalSec * 3L, TimeUnit.SECONDS);

        // Update sync state (no TTL, persistent until device is deregistered)
        String syncKey = "sync_state:" + req.getDeviceId();
        Map<Object, Object> current = redisOperations.opsForHash().entries(syncKey);
        Map<String, Object> syncState = new HashMap<>();
        syncState.put("deviceId", req.getDeviceId());
        syncState.put("manifestVersion", req.getManifestVersion());
        syncState.put("lastOnlineAt", req.getTimestamp());
        if (current.containsKey("lastSyncAt")) {
            syncState.put("lastSyncAt", current.get("lastSyncAt"));
        }
        redisOperations.opsForHash().putAll(syncKey, syncState);

        // Mark device online status
        String onlineKey = "device:online:" + req.getDeviceId();
        redisOperations.opsForValue().set(onlineKey, String.valueOf(now));
        redisOperations.expire(onlineKey, nextIntervalSec * 3L, TimeUnit.SECONDS);

        // Get pending commands
        List<Command> pending = commandRepository
                .findByDeviceIdAndStatusAndExpireAtGreaterThan(req.getDeviceId(), "pending", now);

        // Build response
        HeartbeatResponse resp = new HeartbeatResponse();
        resp.setSuccess(true);
        resp.setNextIntervalSec(nextIntervalSec);

        List<HeartbeatResponse.CommandItem> cmdItems = new ArrayList<>();
        for (Command cmd : pending) {
            HeartbeatResponse.CommandItem item = new HeartbeatResponse.CommandItem();
            item.setCommandId(cmd.getCommandId());
            item.setType(cmd.getType());
            item.setIssuedAt(cmd.getIssuedAt());
            item.setExpireAt(cmd.getExpireAt());
            try {
                item.setPayloadJson(objectMapper.readValue(cmd.getPayloadJson(), Object.class));
            } catch (Exception e) {
                item.setPayloadJson(Map.of());
            }
            cmdItems.add(item);
        }
        resp.setCommands(cmdItems);

        return resp;
    }

    /**
     * 判断设备是否在线（Redis key 是否存在）
     */
    public boolean isDeviceOnline(String deviceId) {
        String onlineKey = "device:online:" + deviceId;
        return Boolean.TRUE.equals(redisOperations.hasKey(onlineKey));
    }
}
