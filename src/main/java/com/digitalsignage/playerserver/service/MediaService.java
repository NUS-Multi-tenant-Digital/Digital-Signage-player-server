package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.BatchGetAssetUrlRequest;
import com.digitalsignage.playerserver.dto.response.BatchAssetUrlResponse;
import com.digitalsignage.playerserver.entity.Media;
import com.digitalsignage.playerserver.entity.Manifest;
import com.digitalsignage.playerserver.entity.Screen;
import com.digitalsignage.playerserver.repository.MediaRepository;
import com.digitalsignage.playerserver.repository.ManifestRepository;
import com.digitalsignage.playerserver.repository.ScreenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MediaService {

    private final MediaRepository mediaRepository;
    private final ManifestRepository manifestRepository;
    private final ScreenRepository screenRepository;
    private final ObjectMapper objectMapper;
    private final OssService ossService;

    private static final long DEFAULT_URL_EXPIRE_MS = 10 * 60 * 1000L;

    public MediaService(MediaRepository mediaRepository,
                        ManifestRepository manifestRepository,
                        ScreenRepository screenRepository,
                        ObjectMapper objectMapper,
                        OssService ossService) {
        this.mediaRepository = mediaRepository;
        this.manifestRepository = manifestRepository;
        this.screenRepository = screenRepository;
        this.objectMapper = objectMapper;
        this.ossService = ossService;
    }

    public BatchAssetUrlResponse batchGetAssetUrls(BatchGetAssetUrlRequest req) {
        // Parse asset_ids as media IDs (Long)
        List<Long> mediaIds = req.getAssetIds().stream()
                .map(id -> {
                    try { return Long.parseLong(id); } catch (NumberFormatException e) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Media> mediaList = mediaRepository.findByIdIn(mediaIds);
        long now = System.currentTimeMillis();

        long urlExpireMs = resolveUrlExpireMs(req.getDeviceId(), req.getManifestId());

        List<BatchAssetUrlResponse.AssetUrlItem> items = new ArrayList<>();
        for (Media media : mediaList) {
            BatchAssetUrlResponse.AssetUrlItem item = new BatchAssetUrlResponse.AssetUrlItem();
            item.setAssetId(String.valueOf(media.getId()));

            String url = resolveDownloadUrl(media, urlExpireMs);
            item.setDownloadUrl(url);
            item.setExpireAt(now + urlExpireMs);
            item.setSha256(media.getChecksumSha256());
            item.setSizeBytes(media.getFileSizeBytes() != null ? media.getFileSizeBytes() : 0);
            items.add(item);
        }

        BatchAssetUrlResponse resp = new BatchAssetUrlResponse();
        resp.setAssets(items);
        return resp;
    }

    private String resolveDownloadUrl(Media media, long urlExpireMs) {
        // 1. file_url takes priority (already a public URL / CDN URL)
        if (media.getFileUrl() != null && !media.getFileUrl().isEmpty()) {
            return media.getFileUrl();
        }

        String objectKey = media.getObjectKey();
        if (objectKey == null || objectKey.isEmpty()) {
            return "https://cdn.example.com/media/" + media.getId();
        }

        // 2. Generate signed URL via Aliyun OSS SDK
        if (ossService.isEnabled()) {
            String signedUrl = ossService.generateSignedUrl(objectKey, urlExpireMs);
            if (signedUrl != null) {
                return signedUrl;
            }
        }

        // 3. Use public base URL if configured
        String publicUrl = ossService.getPublicUrl(objectKey);
        if (publicUrl != null) {
            return publicUrl;
        }

        // 4. Fallback to raw path
        return objectKey;
    }

    @SuppressWarnings("unchecked")
    private long resolveUrlExpireMs(String deviceId, String manifestId) {
        try {
            Optional<Manifest> manifestOpt;
            if (manifestId != null && !manifestId.isEmpty()) {
                manifestOpt = manifestRepository.findByManifestId(manifestId);
            } else if (deviceId != null) {
                // Resolve deviceCode to screenId
                Screen screen = screenRepository.findByDeviceCode(deviceId).orElse(null);
                if (screen != null) {
                    manifestOpt = manifestRepository.findFirstByScreenIdOrderByIsActiveDescVersionDesc(screen.getId());
                } else {
                    return DEFAULT_URL_EXPIRE_MS;
                }
            } else {
                return DEFAULT_URL_EXPIRE_MS;
            }

            if (manifestOpt.isPresent()) {
                String cachePolicyJson = manifestOpt.get().getCachePolicyJson();
                if (cachePolicyJson != null && !cachePolicyJson.isEmpty()) {
                    Map<String, Object> cachePolicy = objectMapper.readValue(cachePolicyJson, Map.class);
                    Object expireSec = cachePolicy.get("url_expire_sec");
                    if (expireSec != null) {
                        return ((Number) expireSec).longValue() * 1000L;
                    }
                }
            }
        } catch (Exception ignored) {}
        return DEFAULT_URL_EXPIRE_MS;
    }
}
