package com.digitalsignage.playerserver.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String registeredDeviceId;
    private static String accessToken;

    @Test
    @Order(1)
    @DisplayName("GET /health - 健康检查（无需鉴权）")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/v1/player/register - 设备注册（无需鉴权）")
    void registerDevice() throws Exception {
        String body = """
                {
                    "device_sn": "SN_INTEG_001",
                    "activation_code": "ACTIVATE_001",
                    "device_name": "Integration Test Device",
                    "platform": "web",
                    "app_version": "1.0.0",
                    "os_version": "Chrome 120",
                    "screen_resolution": "1920x1080",
                    "timezone": "UTC",
                    "mac_address": "AA:BB:CC:DD:EE:01",
                    "ip_address": "192.168.1.100"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/player/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.device_id").isNotEmpty())
                .andExpect(jsonPath("$.data.access_token").isNotEmpty())
                .andExpect(jsonPath("$.data.config.heartbeat_interval_sec").value(30))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        var tree = objectMapper.readTree(json);
        registeredDeviceId = tree.path("data").path("device_id").asText();
        accessToken = tree.path("data").path("access_token").asText();
    }

    @Test
    @Order(3)
    @DisplayName("无 token 访问受保护接口 - 返回 401")
    void noToken_returns401() throws Exception {
        String body = """
                {"device_id": "test", "app_version": "1.0.0", "platform": "web"}
                """;

        mockMvc.perform(post("/api/v1/player/manifest/pull")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/v1/player/manifest/pull - 带 token 无 manifest 返回 NO_UPDATE")
    void pullManifest_noManifest() throws Exception {
        String body = String.format("""
                {
                    "device_id": "%s",
                    "app_version": "1.0.0",
                    "platform": "web"
                }
                """, registeredDeviceId != null ? registeredDeviceId : "non-existent-device");

        mockMvc.perform(post("/api/v1/player/manifest/pull")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.update_type").value("MANIFEST_NO_UPDATE"));
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/v1/assets/batch-url - 带 token 空资源列表")
    void batchUrl_emptyList() throws Exception {
        String body = """
                {
                    "device_id": "any_device",
                    "manifest_id": "m1",
                    "manifest_version": 1,
                    "asset_ids": []
                }
                """;

        mockMvc.perform(post("/api/v1/assets/batch-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assets").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/v1/player/heartbeat - 带 token 心跳上报")
    void heartbeat() throws Exception {
        String deviceId = registeredDeviceId != null ? registeredDeviceId : "test-device";
        String body = String.format("""
                {
                    "device_id": "%s",
                    "app_version": "1.0.0",
                    "manifest_id": "m1",
                    "manifest_version": 1,
                    "timestamp": %d,
                    "playback": {"state": "PLAYING"},
                    "health": {"cpu_percent": 25.0},
                    "cache": {"cached_asset_count": 5},
                    "network": {"online": true}
                }
                """, deviceId, System.currentTimeMillis());

        mockMvc.perform(post("/api/v1/player/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.next_interval_sec").isNumber());
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/v1/player/events - 带 token 事件上报")
    void reportEvents() throws Exception {
        String deviceId = registeredDeviceId != null ? registeredDeviceId : "test-device-001";
        String body = String.format("""
                {
                    "device_id": "%s",
                    "events": [
                        {
                            "event_id": "evt_integ_001",
                            "event_type": "PLAYBACK_STARTED",
                            "timestamp": 1700000000000,
                            "manifest_id": "m1",
                            "manifest_version": 1
                        },
                        {
                            "event_id": "evt_integ_002",
                            "event_type": "PLAYBACK_COMPLETED",
                            "timestamp": 1700000015000
                        }
                    ]
                }
                """, deviceId);

        mockMvc.perform(post("/api/v1/player/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accepted_count").value(2))
                .andExpect(jsonPath("$.data.rejected_count").value(0));
    }

    @Test
    @Order(8)
    @DisplayName("POST /api/v1/player/commands/ack - 带 token 命令确认")
    void commandAck() throws Exception {
        String deviceId = registeredDeviceId != null ? registeredDeviceId : "test-device-001";
        String body = String.format("""
                {
                    "device_id": "%s",
                    "command_id": "cmd_integ_001",
                    "type": "REFRESH_MANIFEST",
                    "success": true,
                    "executed_at": 1700000020000
                }
                """, deviceId);

        mockMvc.perform(post("/api/v1/player/commands/ack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true));
    }
}
