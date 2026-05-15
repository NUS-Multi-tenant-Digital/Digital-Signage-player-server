package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.BatchGetAssetUrlRequest;
import com.digitalsignage.playerserver.dto.response.BatchAssetUrlResponse;
import com.digitalsignage.playerserver.entity.Asset;
import com.digitalsignage.playerserver.entity.Manifest;
import com.digitalsignage.playerserver.repository.AssetRepository;
import com.digitalsignage.playerserver.repository.ManifestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final ManifestRepository manifestRepository;
    private final ObjectMapper objectMapper;
    private final OssService ossService;

    private static final long DEFAULT_URL_EXPIRE_MS = 10 * 60 * 1000L;

    public AssetService(AssetRepository assetRepository,
                        ManifestRepository manifestRepository,
                        ObjectMapper objectMapper,
                        OssService ossService) {
        this.assetRepository = assetRepository;
        this.manifestRepository = manifestRepository;
        this.objectMapper = objectMapper;
        this.ossService = ossService;
    }

    public BatchAssetUrlResponse batchGetAssetUrls(BatchGetAssetUrlRequest req) {
        List<Asset> assets = assetRepository.findByAssetIdIn(req.getAssetIds());
        long now = System.currentTimeMillis();

        long urlExpireMs = resolveUrlExpireMs(req.getDeviceId(), req.getManifestId());

        List<BatchAssetUrlResponse.AssetUrlItem> items = new ArrayList<>();
        for (Asset asset : assets) {
            BatchAssetUrlResponse.AssetUrlItem item = new BatchAssetUrlResponse.AssetUrlItem();
            item.setAssetId(asset.getAssetId());

            // URL resolution priority:
            // 1. CDN path (direct, no signing needed)
            // 2. OSS signed URL (if OSS configured)
            // 3. Public base URL + oss path
            // 4. Raw oss_path as fallback
            String url = resolveDownloadUrl(asset, urlExpireMs);
            item.setDownloadUrl(url);
            item.setExpireAt(asset.getExpireAt() != null ? asset.getExpireAt() : now + urlExpireMs);
            item.setSha256(asset.getSha256());
            item.setSizeBytes(asset.getSizeBytes());
            items.add(item);
        }

        BatchAssetUrlResponse resp = new BatchAssetUrlResponse();
        resp.setAssets(items);
        return resp;
    }

    private String resolveDownloadUrl(Asset asset, long urlExpireMs) {
        // 1. CDN path takes priority (already a public URL)
        if (asset.getCdnPath() != null && !asset.getCdnPath().isEmpty()) {
            return asset.getCdnPath();
        }

        String ossPath = asset.getOssPath();
        if (ossPath == null || ossPath.isEmpty()) {
            return "https://cdn.example.com/assets/" + asset.getAssetId();
        }

        // 2. Generate signed URL via Aliyun OSS SDK
        if (ossService.isEnabled()) {
            String signedUrl = ossService.generateSignedUrl(ossPath, urlExpireMs);
            if (signedUrl != null) {
                return signedUrl;
            }
        }

        // 3. Use public base URL if configured
        String publicUrl = ossService.getPublicUrl(ossPath);
        if (publicUrl != null) {
            return publicUrl;
        }

        // 4. Fallback to raw path
        return ossPath;
    }

    @SuppressWarnings("unchecked")
    private long resolveUrlExpireMs(String deviceId, String manifestId) {
        try {
            Optional<Manifest> manifestOpt;
            if (manifestId != null && !manifestId.isEmpty()) {
                manifestOpt = manifestRepository.findByManifestId(manifestId);
            } else if (deviceId != null) {
                manifestOpt = manifestRepository.findFirstByDeviceIdOrderByIsActiveDescVersionDesc(deviceId);
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
