package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.ReportEventsRequest;
import com.digitalsignage.playerserver.dto.response.ReportEventsResponse;
import com.digitalsignage.playerserver.entity.DeviceEvent;
import com.digitalsignage.playerserver.entity.Screen;
import com.digitalsignage.playerserver.repository.DeviceEventRepository;
import com.digitalsignage.playerserver.repository.ScreenRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EventService {

    private final DeviceEventRepository deviceEventRepository;
    private final ScreenRepository screenRepository;
    private final RedisOperations<String, Object> redisOperations;
    private final ObjectMapper objectMapper;

    public EventService(DeviceEventRepository deviceEventRepository,
                        ScreenRepository screenRepository,
                        RedisOperations<String, Object> redisOperations,
                        ObjectMapper objectMapper) {
        this.deviceEventRepository = deviceEventRepository;
        this.screenRepository = screenRepository;
        this.redisOperations = redisOperations;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReportEventsResponse reportEvents(ReportEventsRequest req) {
        // Resolve deviceCode (external device_id) to screen
        Screen screen = screenRepository.findByDeviceCode(req.getDeviceId()).orElse(null);
        if (screen == null) {
            ReportEventsResponse resp = new ReportEventsResponse();
            resp.setSuccess(false);
            resp.setAcceptedCount(0);
            resp.setRejectedCount(req.getEvents() != null ? req.getEvents().size() : 0);
            return resp;
        }

        Long screenId = screen.getId();
        ReportEventsRequest.EventItem lastEvent = null;

        for (ReportEventsRequest.EventItem item : req.getEvents()) {
            DeviceEvent event = new DeviceEvent();
            event.setScreenId(screenId);
            event.setEventType(item.getEventType());
            event.setCreatedAt(LocalDateTime.now());

            // Determine event level
            if (item.getErrorCode() != null && !item.getErrorCode().isEmpty()) {
                event.setEventLevel("ERROR");
            } else {
                event.setEventLevel("INFO");
            }

            // Serialize extra fields into message JSON
            event.setMessage(buildMessageJson(item));

            deviceEventRepository.save(event);
            lastEvent = item;
        }

        // Update sync state if last event has manifest version
        if (lastEvent != null && lastEvent.getManifestVersion() != null) {
            String syncKey = "sync_state:" + screen.getDeviceCode();
            Map<Object, Object> current = redisOperations.opsForHash().entries(syncKey);
            Map<String, Object> syncState = new HashMap<>();
            syncState.put("deviceCode", screen.getDeviceCode());
            syncState.put("screenId", screenId);
            syncState.put("manifestVersion", lastEvent.getManifestVersion());
            syncState.put("lastSyncAt", lastEvent.getTimestamp());
            if (current.containsKey("lastOnlineAt")) {
                syncState.put("lastOnlineAt", current.get("lastOnlineAt"));
            }
            redisOperations.opsForHash().putAll(syncKey, syncState);
        }

        ReportEventsResponse resp = new ReportEventsResponse();
        resp.setSuccess(true);
        resp.setAcceptedCount(req.getEvents().size());
        resp.setRejectedCount(0);
        return resp;
    }

    private String buildMessageJson(ReportEventsRequest.EventItem item) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (item.getEventId() != null) {
            data.put("eventId", item.getEventId());
        }
        if (item.getManifestId() != null) {
            data.put("manifestId", item.getManifestId());
        }
        if (item.getManifestVersion() != null) {
            data.put("manifestVersion", item.getManifestVersion());
        }
        if (item.getAssetId() != null) {
            data.put("assetId", item.getAssetId());
        }
        if (item.getPlaylistItemId() != null) {
            data.put("playlistItemId", item.getPlaylistItemId());
        }
        if (item.getErrorCode() != null) {
            data.put("errorCode", item.getErrorCode());
        }
        if (item.getErrorMessage() != null) {
            data.put("errorMessage", item.getErrorMessage());
        }
        if (item.getExtraJson() != null) {
            data.put("extraJson", item.getExtraJson());
        }
        if (item.getTimestamp() != 0) {
            data.put("timestamp", item.getTimestamp());
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
