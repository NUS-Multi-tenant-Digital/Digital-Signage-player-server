package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.ReportEventsRequest;
import com.digitalsignage.playerserver.dto.response.ReportEventsResponse;
import com.digitalsignage.playerserver.entity.DeviceEvent;
import com.digitalsignage.playerserver.entity.Screen;
import com.digitalsignage.playerserver.repository.DeviceEventRepository;
import com.digitalsignage.playerserver.repository.ScreenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventServiceTest {

    @Mock private DeviceEventRepository deviceEventRepository;
    @Mock private ScreenRepository screenRepository;
    @Mock private RedisOperations<String, Object> redisOperations;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    private EventService eventService;

    private Screen testScreen;

    @BeforeEach
    void setUp() {
        when(redisOperations.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(new HashMap<>());

        testScreen = new Screen();
        testScreen.setId(1L);
        testScreen.setDeviceCode("d1");

        when(screenRepository.findByDeviceCode("d1")).thenReturn(Optional.of(testScreen));

        eventService = new EventService(deviceEventRepository, screenRepository, redisOperations, new ObjectMapper());
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

        // Verify that saved events use the new message field
        ArgumentCaptor<DeviceEvent> captor = ArgumentCaptor.forClass(DeviceEvent.class);
        verify(deviceEventRepository, times(2)).save(captor.capture());
        for (DeviceEvent saved : captor.getAllValues()) {
            assertThat(saved.getMessage()).isNotNull();
            assertThat(saved.getEventLevel()).isEqualTo("INFO");
            assertThat(saved.getCreatedAt()).isNotNull();
        }
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
