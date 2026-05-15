package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.HeartbeatRequest;
import com.digitalsignage.playerserver.dto.response.HeartbeatResponse;
import com.digitalsignage.playerserver.entity.Command;
import com.digitalsignage.playerserver.entity.PlayerConfig;
import com.digitalsignage.playerserver.repository.CommandRepository;
import com.digitalsignage.playerserver.repository.PlayerConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HeartbeatServiceTest {

    @Mock private PlayerConfigRepository playerConfigRepository;
    @Mock private CommandRepository commandRepository;
    @Mock private RedisOperations<String, Object> redisOperations;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private ValueOperations<String, Object> valueOperations;

    private HeartbeatService heartbeatService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(redisOperations.opsForHash()).thenReturn(hashOperations);
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(hashOperations.entries(anyString())).thenReturn(new HashMap<>());
        heartbeatService = new HeartbeatService(playerConfigRepository, commandRepository, redisOperations, objectMapper);
    }

    @Test
    @DisplayName("心跳正常 - 无 pending commands")
    void heartbeat_noPendingCommands() {
        when(commandRepository.findByDeviceIdAndStatusAndExpireAtGreaterThan(eq("d1"), eq("pending"), anyLong()))
                .thenReturn(Collections.emptyList());

        HeartbeatRequest req = new HeartbeatRequest();
        req.setDeviceId("d1");
        req.setAppVersion("1.0.0");
        req.setManifestId("m1");
        req.setManifestVersion(1);
        req.setTimestamp(System.currentTimeMillis());

        HeartbeatResponse resp = heartbeatService.reportHeartbeat(req);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getCommands()).isEmpty();
        assertThat(resp.getNextIntervalSec()).isEqualTo(30);
    }

    @Test
    @DisplayName("心跳返回 pending commands")
    void heartbeat_withPendingCommands() {
        Command cmd = new Command();
        cmd.setCommandId("cmd_001");
        cmd.setType("REFRESH_MANIFEST");
        cmd.setIssuedAt(System.currentTimeMillis());
        cmd.setExpireAt(System.currentTimeMillis() + 60000);
        cmd.setPayloadJson("{}");

        when(commandRepository.findByDeviceIdAndStatusAndExpireAtGreaterThan(eq("d1"), eq("pending"), anyLong()))
                .thenReturn(List.of(cmd));

        HeartbeatRequest req = new HeartbeatRequest();
        req.setDeviceId("d1");
        req.setAppVersion("1.0.0");
        req.setManifestId("m1");
        req.setManifestVersion(1);
        req.setTimestamp(System.currentTimeMillis());

        HeartbeatResponse resp = heartbeatService.reportHeartbeat(req);

        assertThat(resp.getCommands()).hasSize(1);
        assertThat(resp.getCommands().get(0).getCommandId()).isEqualTo("cmd_001");
        assertThat(resp.getCommands().get(0).getType()).isEqualTo("REFRESH_MANIFEST");
    }

    @Test
    @DisplayName("使用设备配置的心跳间隔")
    void heartbeat_usesConfigInterval() {
        PlayerConfig config = new PlayerConfig();
        config.setHeartbeatIntervalSec(45);
        when(playerConfigRepository.findByDeviceId("d1")).thenReturn(Optional.of(config));
        when(commandRepository.findByDeviceIdAndStatusAndExpireAtGreaterThan(eq("d1"), eq("pending"), anyLong()))
                .thenReturn(Collections.emptyList());

        HeartbeatRequest req = new HeartbeatRequest();
        req.setDeviceId("d1");
        req.setAppVersion("1.0.0");
        req.setManifestId("m1");
        req.setManifestVersion(1);
        req.setTimestamp(System.currentTimeMillis());

        HeartbeatResponse resp = heartbeatService.reportHeartbeat(req);

        assertThat(resp.getNextIntervalSec()).isEqualTo(45);
    }
}
