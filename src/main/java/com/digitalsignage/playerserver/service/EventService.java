package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.ReportEventsRequest;
import com.digitalsignage.playerserver.dto.response.ReportEventsResponse;
import com.digitalsignage.playerserver.entity.DeviceEvent;
import com.digitalsignage.playerserver.entity.Screen;
import com.digitalsignage.playerserver.repository.DeviceEventRepository;
import com.digitalsignage.playerserver.repository.ScreenRepository;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class EventService {

    private final DeviceEventRepository deviceEventRepository;
    private final ScreenRepository screenRepository;
    private final RedisOperations<String, Object> redisOperations;

    public EventService(DeviceEventRepository deviceEventRepository,
                        ScreenRepository screenRepository,
                        RedisOperations<String, Object> redisOperations) {
        this.deviceEventRepository = deviceEventRepository;
        this.screenRepository = screenRepository;
        this.redisOperations = redisOperations;
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
            event.setEventId(item.getEventId());
            event.setScreenId(screenId);
            event.setEventType(item.getEventType());
            event.setEventTimestamp(item.getTimestamp());
            event.setManifestId(item.getManifestId());
            event.setManifestVersion(item.getManifestVersion());
            // media_id from asset_id in request (parse as Long)
            if (item.getAssetId() != null && !item.getAssetId().isEmpty()) {
                try {
                    event.setMediaId(Long.parseLong(item.getAssetId()));
                } catch (NumberFormatException ignored) {}
            }
            event.setPlaylistItemId(item.getPlaylistItemId());
            event.setErrorCode(item.getErrorCode());
            event.setErrorMessage(item.getErrorMessage());
            event.setExtraJson(item.getExtraJson());
            event.setCreatedAt(LocalDateTime.now());
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
}
