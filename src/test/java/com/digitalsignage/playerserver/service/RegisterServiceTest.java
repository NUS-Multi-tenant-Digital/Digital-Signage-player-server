package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.RegisterPlayerRequest;
import com.digitalsignage.playerserver.dto.response.RegisterPlayerResponse;
import com.digitalsignage.playerserver.entity.Screen;
import com.digitalsignage.playerserver.entity.PlayerConfig;
import com.digitalsignage.playerserver.repository.ScreenRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegisterServiceTest {

    @Mock
    private ScreenRepository screenRepository;
    @Mock
    private PlayerConfigRepository playerConfigRepository;
    @Mock
    private RedisOperations<String, Object> redisOperations;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private RegisterService registerService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(redisOperations.opsForHash()).thenReturn(hashOperations);
        registerService = new RegisterService(screenRepository, playerConfigRepository, redisOperations, objectMapper);
    }

    @Test
    @DisplayName("注册成功 - 返回 device_id 和 access_token")
    void register_success() {
        when(screenRepository.save(any(Screen.class))).thenAnswer(inv -> {
            Screen s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(playerConfigRepository.save(any(PlayerConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterPlayerRequest req = new RegisterPlayerRequest();
        req.setDeviceSn("SN001");
        req.setActivationCode("CODE001");
        req.setDeviceName("Test Device");
        req.setPlatform("web");
        req.setAppVersion("1.0.0");
        req.setOsVersion("Chrome 120");
        req.setScreenResolution("1920x1080");
        req.setTimezone("UTC");
        req.setMacAddress("00:11:22:33:44:55");
        req.setIpAddress("192.168.1.1");

        RegisterPlayerResponse resp = registerService.register(req);

        assertThat(resp.getDeviceId()).isNotNull().isNotEmpty();
        assertThat(resp.getAccessToken()).isNotNull().hasSize(64); // 32 bytes -> 64 hex chars
        assertThat(resp.getAccessToken()).matches("[0-9a-f]{64}");
        assertThat(resp.getTenantId()).isEqualTo("1"); // default organization_id
        assertThat(resp.getConfig()).isNotNull();
        assertThat(resp.getConfig().getHeartbeatIntervalSec()).isEqualTo(30);

        verify(screenRepository).save(any(Screen.class));
        verify(playerConfigRepository).save(any(PlayerConfig.class));
    }

    @Test
    @DisplayName("注册时使用自定义 organization_id")
    void register_with_custom_organization() {
        when(screenRepository.save(any(Screen.class))).thenAnswer(inv -> {
            Screen s = inv.getArgument(0);
            s.setId(2L);
            return s;
        });
        when(playerConfigRepository.save(any(PlayerConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterPlayerRequest req = new RegisterPlayerRequest();
        req.setDeviceSn("SN002");
        req.setActivationCode("CODE002");
        req.setDeviceName("Custom Device");
        req.setPlatform("android");
        req.setAppVersion("2.0.0");
        req.setOsVersion("Android 13");
        req.setScreenResolution("2560x1440");
        req.setTimezone("Asia/Shanghai");
        req.setMacAddress("AA:BB:CC:DD:EE:FF");
        req.setIpAddress("10.0.0.1");
        req.setTenantId("42");

        RegisterPlayerResponse resp = registerService.register(req);

        assertThat(resp.getTenantId()).isEqualTo("42");
    }

    @Test
    @DisplayName("注册后 Redis 中保存了初始同步状态")
    void register_saves_sync_state_to_redis() {
        when(screenRepository.save(any(Screen.class))).thenAnswer(inv -> {
            Screen s = inv.getArgument(0);
            s.setId(3L);
            return s;
        });
        when(playerConfigRepository.save(any(PlayerConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterPlayerRequest req = new RegisterPlayerRequest();
        req.setDeviceSn("SN003");
        req.setActivationCode("CODE003");
        req.setDeviceName("Redis Test");
        req.setPlatform("web");
        req.setAppVersion("1.0.0");
        req.setOsVersion("Chrome");
        req.setScreenResolution("1080x720");
        req.setTimezone("UTC");
        req.setMacAddress("11:22:33:44:55:66");
        req.setIpAddress("1.2.3.4");

        registerService.register(req);

        verify(hashOperations).putAll(startsWith("sync_state:"), anyMap());
    }
}
