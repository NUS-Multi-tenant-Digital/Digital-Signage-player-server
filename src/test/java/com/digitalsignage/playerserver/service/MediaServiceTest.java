package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.BatchGetAssetUrlRequest;
import com.digitalsignage.playerserver.dto.response.BatchAssetUrlResponse;
import com.digitalsignage.playerserver.entity.Media;
import com.digitalsignage.playerserver.repository.MediaRepository;
import com.digitalsignage.playerserver.repository.ManifestRepository;
import com.digitalsignage.playerserver.repository.ScreenRepository;
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
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private ManifestRepository manifestRepository;
    @Mock
    private ScreenRepository screenRepository;

    private MediaService mediaService;
    private ObjectMapper objectMapper = new ObjectMapper();
    private OssService ossService;

    @BeforeEach
    void setUp() {
        // OssService with empty access-key-id → disabled mode, no OSS signing
        ossService = new OssService(null, "test-bucket", "", "");
        mediaService = new MediaService(mediaRepository, manifestRepository, screenRepository, objectMapper, ossService);
    }

    @Test
    @DisplayName("批量获取资源 URL - 优先返回 file_url")
    void batchGetAssetUrls_prefersFileUrl() {
        Media media = new Media();
        media.setId(1L);
        media.setFileUrl("https://cdn.example.com/video.mp4");
        media.setObjectKey("mvp/video.mp4");
        media.setChecksumSha256("abc123");
        media.setFileSizeBytes(1024000L);

        when(mediaRepository.findByIdIn(List.of(1L))).thenReturn(List.of(media));

        BatchGetAssetUrlRequest req = new BatchGetAssetUrlRequest();
        req.setDeviceId("d1");
        req.setAssetIds(List.of("1"));

        BatchAssetUrlResponse resp = mediaService.batchGetAssetUrls(req);

        assertThat(resp.getAssets()).hasSize(1);
        assertThat(resp.getAssets().get(0).getDownloadUrl()).isEqualTo("https://cdn.example.com/video.mp4");
        assertThat(resp.getAssets().get(0).getSha256()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("file_url 为空时 fallback 到 object_key")
    void batchGetAssetUrls_fallbackToObjectKey() {
        Media media = new Media();
        media.setId(2L);
        media.setFileUrl(null);
        media.setObjectKey("oss://bucket/image.png");
        media.setChecksumSha256("def456");
        media.setFileSizeBytes(2048L);

        when(mediaRepository.findByIdIn(List.of(2L))).thenReturn(List.of(media));

        BatchGetAssetUrlRequest req = new BatchGetAssetUrlRequest();
        req.setDeviceId("d1");
        req.setAssetIds(List.of("2"));

        BatchAssetUrlResponse resp = mediaService.batchGetAssetUrls(req);

        assertThat(resp.getAssets().get(0).getDownloadUrl()).isEqualTo("oss://bucket/image.png");
    }

    @Test
    @DisplayName("空列表返回空结果")
    void batchGetAssetUrls_emptyList() {
        when(mediaRepository.findByIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());

        BatchGetAssetUrlRequest req = new BatchGetAssetUrlRequest();
        req.setDeviceId("d1");
        req.setAssetIds(Collections.emptyList());

        BatchAssetUrlResponse resp = mediaService.batchGetAssetUrls(req);

        assertThat(resp.getAssets()).isEmpty();
    }
}
