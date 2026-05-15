package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.PullManifestRequest;
import com.digitalsignage.playerserver.dto.response.PullManifestResponse;
import com.digitalsignage.playerserver.entity.Asset;
import com.digitalsignage.playerserver.entity.Manifest;
import com.digitalsignage.playerserver.entity.ManifestAsset;
import com.digitalsignage.playerserver.entity.PlayerConfig;
import com.digitalsignage.playerserver.repository.ManifestAssetRepository;
import com.digitalsignage.playerserver.repository.ManifestRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ManifestServiceTest {

    @Mock private ManifestRepository manifestRepository;
    @Mock private ManifestAssetRepository manifestAssetRepository;
    @Mock private PlayerConfigRepository playerConfigRepository;
    @Mock private RedisOperations<String, Object> redisOperations;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    private ManifestService manifestService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(redisOperations.opsForHash()).thenReturn(hashOperations);
        manifestService = new ManifestService(manifestRepository, manifestAssetRepository,
                playerConfigRepository, redisOperations, objectMapper);
    }

    @Test
    @DisplayName("设备无 manifest - 返回 NO_UPDATE")
    void pullManifest_noManifest() {
        when(manifestRepository.findFirstByDeviceIdOrderByIsActiveDescVersionDesc("device_001"))
                .thenReturn(Optional.empty());

        PullManifestRequest req = new PullManifestRequest();
        req.setDeviceId("device_001");

        PullManifestResponse resp = manifestService.pullManifest(req);

        assertThat(resp.getUpdateType()).isEqualTo("MANIFEST_NO_UPDATE");
        assertThat(resp.getManifest()).isNull();
        assertThat(resp.getNextPollIntervalSec()).isEqualTo(60);
    }

    @Test
    @DisplayName("客户端版本已最新 - 返回 NO_UPDATE")
    void pullManifest_alreadyUpToDate() {
        Manifest manifest = new Manifest();
        manifest.setManifestId("m1");
        manifest.setVersion(5);
        when(manifestRepository.findFirstByDeviceIdOrderByIsActiveDescVersionDesc("device_001"))
                .thenReturn(Optional.of(manifest));

        PullManifestRequest req = new PullManifestRequest();
        req.setDeviceId("device_001");
        req.setCurrentManifestVersion(5);

        PullManifestResponse resp = manifestService.pullManifest(req);

        assertThat(resp.getUpdateType()).isEqualTo("MANIFEST_NO_UPDATE");
    }

    @Test
    @DisplayName("有新版本 - 返回 FULL_UPDATE 含完整 manifest")
    void pullManifest_fullUpdate() {
        Manifest manifest = new Manifest();
        manifest.setManifestId("m1");
        manifest.setVersion(2);
        manifest.setTenantId("t1");
        manifest.setDeviceId("device_001");
        manifest.setLocationId("loc1");
        manifest.setGroupId("g1");
        manifest.setValidFrom(1000L);
        manifest.setValidTo(0L);
        manifest.setTtlSec(3600);
        manifest.setTemplateConfigJson("{\"template_id\":\"T1\",\"slots\":[]}");
        manifest.setPlaybackPlanJson("{\"scenes\":[]}");
        manifest.setCachePolicyJson("{\"max_cache_size_mb\":2048}");
        manifest.setFallbackPolicyJson("{\"fallback_asset_id\":\"a1\"}");
        manifest.setChecksum("abc123");
        manifest.setGeneratedAt(1000L);

        when(manifestRepository.findFirstByDeviceIdOrderByIsActiveDescVersionDesc("device_001"))
                .thenReturn(Optional.of(manifest));
        when(manifestAssetRepository.findAssetsByManifestId("m1")).thenReturn(Collections.emptyList());
        when(manifestAssetRepository.findByManifestId("m1")).thenReturn(Collections.emptyList());

        PullManifestRequest req = new PullManifestRequest();
        req.setDeviceId("device_001");
        req.setCurrentManifestVersion(1);

        PullManifestResponse resp = manifestService.pullManifest(req);

        assertThat(resp.getUpdateType()).isEqualTo("MANIFEST_FULL_UPDATE");
        assertThat(resp.getManifest()).isNotNull();
        assertThat(resp.getServerTime()).isGreaterThan(0);
    }

    @Test
    @DisplayName("使用设备配置中的 poll interval")
    void pullManifest_usesConfigInterval() {
        PlayerConfig config = new PlayerConfig();
        config.setManifestSyncIntervalSec(120);
        when(playerConfigRepository.findByDeviceId("device_001")).thenReturn(Optional.of(config));
        when(manifestRepository.findFirstByDeviceIdOrderByIsActiveDescVersionDesc("device_001"))
                .thenReturn(Optional.empty());

        PullManifestRequest req = new PullManifestRequest();
        req.setDeviceId("device_001");

        PullManifestResponse resp = manifestService.pullManifest(req);

        assertThat(resp.getNextPollIntervalSec()).isEqualTo(120);
    }
}
