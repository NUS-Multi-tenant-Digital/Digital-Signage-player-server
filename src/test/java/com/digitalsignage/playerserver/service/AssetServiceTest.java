package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.BatchGetAssetUrlRequest;
import com.digitalsignage.playerserver.dto.response.BatchAssetUrlResponse;
import com.digitalsignage.playerserver.entity.Asset;
import com.digitalsignage.playerserver.repository.AssetRepository;
import com.digitalsignage.playerserver.repository.ManifestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private ManifestRepository manifestRepository;

    private AssetService assetService;
    private ObjectMapper objectMapper = new ObjectMapper();
    private OssService ossService;

    @BeforeEach
    void setUp() {
        // OssService with empty access-key-id → disabled mode, no OSS signing
        ossService = new OssService(null, "test-bucket", "", "");
        assetService = new AssetService(assetRepository, manifestRepository, objectMapper, ossService);
    }

    @Test
    @DisplayName("批量获取资源 URL - 优先返回 CDN 路径")
    void batchGetAssetUrls_prefersCdn() {
        Asset asset = new Asset();
        asset.setAssetId("a1");
        asset.setCdnPath("https://cdn.example.com/video.mp4");
        asset.setOssPath("oss://bucket/video.mp4");
        asset.setSha256("abc123");
        asset.setSizeBytes(1024000);

        when(assetRepository.findByAssetIdIn(List.of("a1"))).thenReturn(List.of(asset));

        BatchGetAssetUrlRequest req = new BatchGetAssetUrlRequest();
        req.setDeviceId("d1");
        req.setAssetIds(List.of("a1"));

        BatchAssetUrlResponse resp = assetService.batchGetAssetUrls(req);

        assertThat(resp.getAssets()).hasSize(1);
        assertThat(resp.getAssets().get(0).getDownloadUrl()).isEqualTo("https://cdn.example.com/video.mp4");
        assertThat(resp.getAssets().get(0).getSha256()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("CDN 为空时 fallback 到 OSS 路径")
    void batchGetAssetUrls_fallbackToOss() {
        Asset asset = new Asset();
        asset.setAssetId("a2");
        asset.setCdnPath(null);
        asset.setOssPath("oss://bucket/image.png");
        asset.setSha256("def456");
        asset.setSizeBytes(2048);

        when(assetRepository.findByAssetIdIn(List.of("a2"))).thenReturn(List.of(asset));

        BatchGetAssetUrlRequest req = new BatchGetAssetUrlRequest();
        req.setDeviceId("d1");
        req.setAssetIds(List.of("a2"));

        BatchAssetUrlResponse resp = assetService.batchGetAssetUrls(req);

        assertThat(resp.getAssets().get(0).getDownloadUrl()).isEqualTo("oss://bucket/image.png");
    }

    @Test
    @DisplayName("空列表返回空结果")
    void batchGetAssetUrls_emptyList() {
        when(assetRepository.findByAssetIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());

        BatchGetAssetUrlRequest req = new BatchGetAssetUrlRequest();
        req.setDeviceId("d1");
        req.setAssetIds(Collections.emptyList());

        BatchAssetUrlResponse resp = assetService.batchGetAssetUrls(req);

        assertThat(resp.getAssets()).isEmpty();
    }
}
