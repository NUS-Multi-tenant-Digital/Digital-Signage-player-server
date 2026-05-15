package com.digitalsignage.playerserver.stress;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("压力测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StressTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int CONCURRENT_USERS = 50;
    private static final int REQUESTS_PER_USER = 20;

    // Shared token for authenticated tests
    private static String sharedToken;

    /**
     * Helper: register a device and return access_token
     */
    private String registerAndGetToken(String suffix) throws Exception {
        String body = String.format("""
                {
                    "device_sn": "TOKEN_SN_%s",
                    "activation_code": "TOKEN_CODE_%s",
                    "device_name": "Token Device %s",
                    "platform": "web",
                    "app_version": "1.0.0",
                    "os_version": "Chrome",
                    "screen_resolution": "1920x1080",
                    "timezone": "UTC",
                    "mac_address": "AA:BB:CC:DD:EE:FF",
                    "ip_address": "10.0.0.1"
                }
                """, suffix, suffix, suffix);

        var result = mockMvc.perform(post("/api/v1/player/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        var tree = objectMapper.readTree(result.getResponse().getContentAsString());
        return tree.path("data").path("access_token").asText();
    }

    @Test
    @Order(1)
    @DisplayName("并发注册压力测试 - 50 并发 x 20 请求")
    void stressTest_register() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < REQUESTS_PER_USER; j++) {
                        long reqStart = System.currentTimeMillis();
                        try {
                            String body = String.format("""
                                    {
                                        "device_sn": "STRESS_SN_%d_%d",
                                        "activation_code": "CODE_%d_%d",
                                        "device_name": "Stress Device %d-%d",
                                        "platform": "web",
                                        "app_version": "1.0.0",
                                        "os_version": "Chrome",
                                        "screen_resolution": "1920x1080",
                                        "timezone": "UTC",
                                        "mac_address": "00:00:00:%02X:%02X:%02X",
                                        "ip_address": "10.%d.%d.%d"
                                    }
                                    """, userId, j, userId, j, userId, j,
                                    userId % 256, j % 256, (userId * j) % 256,
                                    userId % 256, j % 256, (userId + j) % 256);

                            mockMvc.perform(post("/api/v1/player/register")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(body))
                                    .andExpect(status().isOk());
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                        }
                        responseTimes.add(System.currentTimeMillis() - reqStart);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(120, TimeUnit.SECONDS);
        executor.shutdown();

        long totalTime = System.currentTimeMillis() - startTime;
        int totalRequests = CONCURRENT_USERS * REQUESTS_PER_USER;

        Collections.sort(responseTimes);
        double avgMs = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long p50 = responseTimes.get(responseTimes.size() / 2);
        long p95 = responseTimes.get((int) (responseTimes.size() * 0.95));
        long p99 = responseTimes.get((int) (responseTimes.size() * 0.99));
        double rps = totalRequests * 1000.0 / totalTime;

        System.out.println("========== 注册压力测试结果 ==========");
        System.out.println("总请求数: " + totalRequests);
        System.out.println("成功: " + successCount.get());
        System.out.println("失败: " + failCount.get());
        System.out.printf("总耗时: %d ms%n", totalTime);
        System.out.printf("QPS: %.2f%n", rps);
        System.out.printf("平均响应时间: %.2f ms%n", avgMs);
        System.out.printf("P50: %d ms | P95: %d ms | P99: %d ms%n", p50, p95, p99);
        System.out.println("======================================");

        double successRate = successCount.get() * 100.0 / totalRequests;
        assertThat(successRate).as("成功率应 >= 95%%").isGreaterThanOrEqualTo(95.0);

        // Save a token for subsequent tests
        sharedToken = registerAndGetToken("shared");
    }

    @Test
    @Order(2)
    @DisplayName("并发心跳压力测试 - 50 并发 x 20 请求（带 JWT）")
    void stressTest_heartbeat() throws Exception {
        // Ensure we have a token
        if (sharedToken == null) {
            sharedToken = registerAndGetToken("heartbeat_setup");
        }

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < REQUESTS_PER_USER; j++) {
                        long reqStart = System.currentTimeMillis();
                        try {
                            String body = String.format("""
                                    {
                                        "device_id": "stress_device_%d",
                                        "app_version": "1.0.0",
                                        "manifest_id": "m1",
                                        "manifest_version": 1,
                                        "timestamp": %d,
                                        "playback": {"state": "PLAYING"},
                                        "health": {"cpu_percent": %.1f},
                                        "cache": {"cached_asset_count": %d},
                                        "network": {"online": true}
                                    }
                                    """, userId, System.currentTimeMillis(),
                                    (double) (userId * 5 % 100), j);

                            mockMvc.perform(post("/api/v1/player/heartbeat")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .header("Authorization", "Bearer " + sharedToken)
                                            .content(body))
                                    .andExpect(status().isOk());
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                        }
                        responseTimes.add(System.currentTimeMillis() - reqStart);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(120, TimeUnit.SECONDS);
        executor.shutdown();

        long totalTime = System.currentTimeMillis() - startTime;
        int totalRequests = CONCURRENT_USERS * REQUESTS_PER_USER;

        Collections.sort(responseTimes);
        double avgMs = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long p50 = responseTimes.get(responseTimes.size() / 2);
        long p95 = responseTimes.get((int) (responseTimes.size() * 0.95));
        long p99 = responseTimes.get((int) (responseTimes.size() * 0.99));
        double rps = totalRequests * 1000.0 / totalTime;

        System.out.println("========== 心跳压力测试结果 ==========");
        System.out.println("总请求数: " + totalRequests);
        System.out.println("成功: " + successCount.get());
        System.out.println("失败: " + failCount.get());
        System.out.printf("总耗时: %d ms%n", totalTime);
        System.out.printf("QPS: %.2f%n", rps);
        System.out.printf("平均响应时间: %.2f ms%n", avgMs);
        System.out.printf("P50: %d ms | P95: %d ms | P99: %d ms%n", p50, p95, p99);
        System.out.println("======================================");

        double successRate = successCount.get() * 100.0 / totalRequests;
        assertThat(successRate).as("成功率应 >= 95%%").isGreaterThanOrEqualTo(95.0);
    }

    @Test
    @Order(3)
    @DisplayName("混合接口压力测试 - 模拟真实设备行为")
    void stressTest_mixedWorkload() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    // Step 1: Register (no auth needed)
                    String registerBody = String.format("""
                            {
                                "device_sn": "MIX_SN_%d",
                                "activation_code": "MIX_CODE_%d",
                                "device_name": "Mix Device %d",
                                "platform": "web",
                                "app_version": "1.0.0",
                                "os_version": "Chrome",
                                "screen_resolution": "1920x1080",
                                "timezone": "UTC",
                                "mac_address": "FF:00:00:00:%02X:%02X",
                                "ip_address": "172.16.%d.%d"
                            }
                            """, userId, userId, userId, userId % 256, userId / 256, userId % 256, userId / 256 + 1);

                    long reqStart = System.currentTimeMillis();
                    try {
                        var result = mockMvc.perform(post("/api/v1/player/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(registerBody))
                                .andExpect(status().isOk())
                                .andReturn();
                        successCount.incrementAndGet();
                        responseTimes.add(System.currentTimeMillis() - reqStart);

                        var tree = objectMapper.readTree(result.getResponse().getContentAsString());
                        String deviceId = tree.path("data").path("device_id").asText();
                        String token = tree.path("data").path("access_token").asText();

                        // Step 2: Multiple heartbeats (with token)
                        for (int j = 0; j < 5; j++) {
                            reqStart = System.currentTimeMillis();
                            String hbBody = String.format("""
                                    {
                                        "device_id": "%s",
                                        "app_version": "1.0.0",
                                        "manifest_id": "m1",
                                        "manifest_version": 1,
                                        "timestamp": %d,
                                        "playback": {"state": "PLAYING"},
                                        "network": {"online": true}
                                    }
                                    """, deviceId, System.currentTimeMillis());

                            mockMvc.perform(post("/api/v1/player/heartbeat")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .header("Authorization", "Bearer " + token)
                                            .content(hbBody))
                                    .andExpect(status().isOk());
                            successCount.incrementAndGet();
                            responseTimes.add(System.currentTimeMillis() - reqStart);
                        }

                        // Step 3: Pull manifest (with token)
                        reqStart = System.currentTimeMillis();
                        String pullBody = String.format("""
                                {"device_id": "%s", "app_version": "1.0.0", "platform": "web"}
                                """, deviceId);
                        mockMvc.perform(post("/api/v1/player/manifest/pull")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", "Bearer " + token)
                                        .content(pullBody))
                                .andExpect(status().isOk());
                        successCount.incrementAndGet();
                        responseTimes.add(System.currentTimeMillis() - reqStart);

                        // Step 4: Report events (with token)
                        reqStart = System.currentTimeMillis();
                        String eventsBody = String.format("""
                                {
                                    "device_id": "%s",
                                    "events": [
                                        {"event_id": "evt_%d_1", "event_type": "PLAYBACK_STARTED", "timestamp": %d},
                                        {"event_id": "evt_%d_2", "event_type": "ASSET_LOADED", "timestamp": %d}
                                    ]
                                }
                                """, deviceId, userId, System.currentTimeMillis(), userId, System.currentTimeMillis());
                        mockMvc.perform(post("/api/v1/player/events")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", "Bearer " + token)
                                        .content(eventsBody))
                                .andExpect(status().isOk());
                        successCount.incrementAndGet();
                        responseTimes.add(System.currentTimeMillis() - reqStart);

                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        responseTimes.add(System.currentTimeMillis() - reqStart);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(180, TimeUnit.SECONDS);
        executor.shutdown();

        long totalTime = System.currentTimeMillis() - startTime;
        int totalSuccess = successCount.get();
        int totalFail = failCount.get();
        int totalRequests = totalSuccess + totalFail;

        Collections.sort(responseTimes);
        double avgMs = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long p50 = responseTimes.isEmpty() ? 0 : responseTimes.get(responseTimes.size() / 2);
        long p95 = responseTimes.isEmpty() ? 0 : responseTimes.get((int) (responseTimes.size() * 0.95));
        long p99 = responseTimes.isEmpty() ? 0 : responseTimes.get((int) (responseTimes.size() * 0.99));
        double rps = totalRequests * 1000.0 / totalTime;

        System.out.println("========== 混合负载压力测试结果 ==========");
        System.out.printf("模拟设备数: %d%n", CONCURRENT_USERS);
        System.out.printf("总请求数: %d (注册+心跳+manifest+events)%n", totalRequests);
        System.out.println("成功: " + totalSuccess);
        System.out.println("失败: " + totalFail);
        System.out.printf("总耗时: %d ms%n", totalTime);
        System.out.printf("QPS: %.2f%n", rps);
        System.out.printf("平均响应时间: %.2f ms%n", avgMs);
        System.out.printf("P50: %d ms | P95: %d ms | P99: %d ms%n", p50, p95, p99);
        System.out.println("==========================================");

        double successRate = totalSuccess * 100.0 / totalRequests;
        assertThat(successRate).as("成功率应 >= 90%%").isGreaterThanOrEqualTo(90.0);
    }
}
