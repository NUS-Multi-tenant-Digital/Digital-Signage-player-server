package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.ReportEventsRequest;
import com.digitalsignage.playerserver.dto.response.ReportEventsResponse;
import com.digitalsignage.playerserver.entity.DeviceEvent;
import com.digitalsignage.playerserver.repository.DeviceEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventServiceTest {

    @Mock private DeviceEventRepository deviceEventRepository;
    @Mock private RedisOperations<String, Object> redisOperations;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        when(redisOperations.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(new HashMap<>());
        eventService = new EventService(deviceEventRepository, redisOperations);
    }

    @Test
    @DisplayName("上报事件 - 全部接受")
    void reportEvents_allAccepted() {
        when(deviceEventRepository.save(any(DeviceEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        ReportEventsRequest.EventItem e1 = new ReportEventsRequest.EventItem();
        e1.setEventId("evt_001");
        e1.setEventType("PLAYBACK_STARTED");
        e1.setTimestamp(System.currentTimeMillis());

        ReportEventsRequest.EventItem e2 = new ReportEventsRequest.EventItem();
        e2.setEventId("evt_002");
        e2.setEventType("PLAYBACK_COMPLETED");
        e2.setTimestamp(System.currentTimeMillis());

        ReportEventsRequest req = new ReportEventsRequest();
        req.setDeviceId("d1");
        req.setEvents(Arrays.asList(e1, e2));

        ReportEventsResponse resp = eventService.reportEvents(req);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getAcceptedCount()).isEqualTo(2);
        assertThat(resp.getRejectedCount()).isEqualTo(0);
        verify(deviceEventRepository, times(2)).save(any(DeviceEvent.class));
    }

    @Test
    @DisplayName("上报事件 - 更新 Redis 同步状态")
    void reportEvents_updatesSyncState() {
        when(deviceEventRepository.save(any(DeviceEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        ReportEventsRequest.EventItem e1 = new ReportEventsRequest.EventItem();
        e1.setEventId("evt_003");
        e1.setEventType("ASSET_LOADED");
        e1.setTimestamp(1000L);
        e1.setManifestVersion(3L);

        ReportEventsRequest req = new ReportEventsRequest();
        req.setDeviceId("d1");
        req.setEvents(List.of(e1));

        eventService.reportEvents(req);

        verify(hashOperations).putAll(eq("sync_state:d1"), anyMap());
    }
}
