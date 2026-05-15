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
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ManifestService {

    private final ManifestRepository manifestRepository;
    private final ManifestAssetRepository manifestAssetRepository;
    private final PlayerConfigRepository playerConfigRepository;
    private final RedisOperations<String, Object> redisOperations;
    private final ObjectMapper objectMapper;

    private static final String MANIFEST_CACHE_PREFIX = "manifest_cache:";

    public ManifestService(ManifestRepository manifestRepository,
                           ManifestAssetRepository manifestAssetRepository,
                           PlayerConfigRepository playerConfigRepository,
                           RedisOperations<String, Object> redisOperations,
                           ObjectMapper objectMapper) {
        this.manifestRepository = manifestRepository;
        this.manifestAssetRepository = manifestAssetRepository;
        this.playerConfigRepository = playerConfigRepository;
        this.redisOperations = redisOperations;
        this.objectMapper = objectMapper;
    }

    public PullManifestResponse pullManifest(PullManifestRequest req) {
        long now = System.currentTimeMillis();

        PlayerConfig config = playerConfigRepository.findByDeviceId(req.getDeviceId()).orElse(null);
        int nextPollIntervalSec = config != null ? config.getManifestSyncIntervalSec() : 60;

        Optional<Manifest> manifestOpt = manifestRepository
                .findFirstByDeviceIdOrderByIsActiveDescVersionDesc(req.getDeviceId());

        if (manifestOpt.isEmpty()) {
            PullManifestResponse resp = new PullManifestResponse();
            resp.setUpdateType("MANIFEST_NO_UPDATE");
            resp.setManifest(null);
            resp.setNextPollIntervalSec(nextPollIntervalSec);
            resp.setServerTime(now);
            resp.setMessage("No manifest assigned to this device yet");
            return resp;
        }

        Manifest manifest = manifestOpt.get();
        int clientVersion = req.getCurrentManifestVersion() != null ? req.getCurrentManifestVersion() : 0;

        if (clientVersion >= manifest.getVersion()) {
            PullManifestResponse resp = new PullManifestResponse();
            resp.setUpdateType("MANIFEST_NO_UPDATE");
            resp.setManifest(null);
            resp.setNextPollIntervalSec(nextPollIntervalSec);
            resp.setServerTime(now);
            resp.setMessage("Manifest is up to date");
            return resp;
        }

        // Save sync state to Redis
        String syncKey = "sync_state:" + req.getDeviceId();
        Map<String, Object> syncState = new HashMap<>();
        syncState.put("deviceId", req.getDeviceId());
        syncState.put("manifestVersion", manifest.getVersion());
        syncState.put("lastSyncAt", now);
        syncState.put("lastOnlineAt", now);
        redisOperations.opsForHash().putAll(syncKey, syncState);

        // Try to get manifest data from Redis cache
        String cacheKey = MANIFEST_CACHE_PREFIX + manifest.getManifestId() + ":" + manifest.getVersion();
        Map<String, Object> manifestData = getManifestFromCache(cacheKey);

        if (manifestData == null) {
            // Cache miss: build from MySQL and cache it
            manifestData = buildManifestData(manifest);

            // Cache with TTL from manifest's ttl_sec (default 1 hour)
            int ttlSec = manifest.getTtlSec() > 0 ? manifest.getTtlSec() : 3600;
            try {
                String json = objectMapper.writeValueAsString(manifestData);
                redisOperations.opsForValue().set(cacheKey, json);
                redisOperations.expire(cacheKey, ttlSec, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }

        PullManifestResponse resp = new PullManifestResponse();
        resp.setUpdateType("MANIFEST_FULL_UPDATE");
        resp.setManifest(manifestData);
        resp.setNextPollIntervalSec(nextPollIntervalSec);
        resp.setServerTime(now);
        resp.setMessage("New manifest available");
        return resp;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getManifestFromCache(String cacheKey) {
        try {
            Object cached = redisOperations.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached.toString(), Map.class);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Map<String, Object> buildManifestData(Manifest manifest) {
        Map<String, Object> manifestData = new LinkedHashMap<>();
        manifestData.put("manifest_id", manifest.getManifestId());
        manifestData.put("version", manifest.getVersion());
        manifestData.put("tenant_id", manifest.getTenantId());
        manifestData.put("device_id", manifest.getDeviceId());
        manifestData.put("location_id", manifest.getLocationId());
        manifestData.put("group_id", manifest.getGroupId());
        manifestData.put("valid_from", manifest.getValidFrom());
        manifestData.put("valid_to", manifest.getValidTo());
        manifestData.put("ttl_sec", manifest.getTtlSec());

        try {
            manifestData.put("template_config", objectMapper.readValue(manifest.getTemplateConfigJson(), Object.class));
            manifestData.put("playback_plan", objectMapper.readValue(manifest.getPlaybackPlanJson(), Object.class));
            manifestData.put("cache_policy", objectMapper.readValue(manifest.getCachePolicyJson(), Object.class));
            manifestData.put("fallback_policy", objectMapper.readValue(manifest.getFallbackPolicyJson(), Object.class));
        } catch (Exception e) {
            manifestData.put("template_config", Map.of());
            manifestData.put("playback_plan", Map.of());
            manifestData.put("cache_policy", Map.of());
            manifestData.put("fallback_policy", Map.of());
        }

        List<Asset> assets = manifestAssetRepository.findAssetsByManifestId(manifest.getManifestId());
        List<ManifestAsset> manifestAssets = manifestAssetRepository.findByManifestId(manifest.getManifestId());
        Map<String, ManifestAsset> maMap = new HashMap<>();
        for (ManifestAsset ma : manifestAssets) {
            maMap.put(ma.getAssetId(), ma);
        }

        List<Map<String, Object>> assetList = new ArrayList<>();
        for (Asset asset : assets) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("asset_id", asset.getAssetId());
            a.put("asset_type", asset.getAssetType());
            a.put("file_name", asset.getFileName());
            a.put("asset_ref", asset.getAssetRef());
            a.put("oss_path", asset.getOssPath());
            a.put("cdn_path", asset.getCdnPath());
            a.put("mime_type", asset.getMimeType());
            a.put("size_bytes", asset.getSizeBytes());
            a.put("sha256", asset.getSha256());
            a.put("duration_ms", asset.getDurationMs());
            ManifestAsset ma = maMap.get(asset.getAssetId());
            a.put("required", ma != null && ma.isRequired());
            a.put("priority", ma != null ? ma.getPriority() : 0);
            assetList.add(a);
        }
        manifestData.put("assets", assetList);
        manifestData.put("checksum", manifest.getChecksum());
        manifestData.put("generated_at", manifest.getGeneratedAt());

        return manifestData;
    }
}
