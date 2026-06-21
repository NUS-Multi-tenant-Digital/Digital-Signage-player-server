package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.PullManifestRequest;
import com.digitalsignage.playerserver.dto.response.PullManifestResponse;
import com.digitalsignage.playerserver.entity.Media;
import com.digitalsignage.playerserver.entity.Manifest;
import com.digitalsignage.playerserver.entity.ManifestMedia;
import com.digitalsignage.playerserver.entity.PlayerConfig;
import com.digitalsignage.playerserver.entity.Screen;
import com.digitalsignage.playerserver.repository.ManifestMediaRepository;
import com.digitalsignage.playerserver.repository.ManifestRepository;
import com.digitalsignage.playerserver.repository.PlayerConfigRepository;
import com.digitalsignage.playerserver.repository.ScreenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ManifestService {

    private final ManifestRepository manifestRepository;
    private final ManifestMediaRepository manifestMediaRepository;
    private final PlayerConfigRepository playerConfigRepository;
    private final ScreenRepository screenRepository;
    private final RedisOperations<String, Object> redisOperations;
    private final ObjectMapper objectMapper;

    private static final String MANIFEST_CACHE_PREFIX = "manifest_cache:";

    public ManifestService(ManifestRepository manifestRepository,
                           ManifestMediaRepository manifestMediaRepository,
                           PlayerConfigRepository playerConfigRepository,
                           ScreenRepository screenRepository,
                           RedisOperations<String, Object> redisOperations,
                           ObjectMapper objectMapper) {
        this.manifestRepository = manifestRepository;
        this.manifestMediaRepository = manifestMediaRepository;
        this.playerConfigRepository = playerConfigRepository;
        this.screenRepository = screenRepository;
        this.redisOperations = redisOperations;
        this.objectMapper = objectMapper;
    }

    public PullManifestResponse pullManifest(PullManifestRequest req) {
        long now = System.currentTimeMillis();

        // Resolve deviceCode (external device_id) to screen
        Screen screen = screenRepository.findByDeviceCode(req.getDeviceId()).orElse(null);
        if (screen == null) {
            PullManifestResponse resp = new PullManifestResponse();
            resp.setUpdateType("MANIFEST_NO_UPDATE");
            resp.setManifest(null);
            resp.setNextPollIntervalSec(60);
            resp.setServerTime(now);
            resp.setMessage("Screen not found for device_id: " + req.getDeviceId());
            return resp;
        }

        Long screenId = screen.getId();
        PlayerConfig config = playerConfigRepository.findByScreenId(screenId).orElse(null);
        int nextPollIntervalSec = config != null ? config.getManifestSyncIntervalSec() : 60;

        Optional<Manifest> manifestOpt = manifestRepository
                .findFirstByScreenIdOrderByIsActiveDescVersionDesc(screenId);

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

        // Save sync state to Redis using deviceCode
        String syncKey = "sync_state:" + screen.getDeviceCode();
        Map<String, Object> syncState = new HashMap<>();
        syncState.put("deviceCode", screen.getDeviceCode());
        syncState.put("screenId", screenId);
        syncState.put("manifestVersion", manifest.getVersion());
        syncState.put("lastSyncAt", now);
        syncState.put("lastOnlineAt", now);
        redisOperations.opsForHash().putAll(syncKey, syncState);

        // Try to get manifest data from Redis cache
        String cacheKey = MANIFEST_CACHE_PREFIX + manifest.getManifestId() + ":" + manifest.getVersion();
        Map<String, Object> manifestData = getManifestFromCache(cacheKey);

        if (manifestData == null) {
            // Cache miss: build from MySQL and cache it
            manifestData = buildManifestData(manifest, screen);

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

    private Map<String, Object> buildManifestData(Manifest manifest, Screen screen) {
        Map<String, Object> manifestData = new LinkedHashMap<>();
        manifestData.put("manifest_id", manifest.getManifestId());
        manifestData.put("version", manifest.getVersion());
        manifestData.put("organization_id", manifest.getOrganizationId());
        // 前端契约字段：tenant_id / location_id
        manifestData.put("tenant_id", String.valueOf(manifest.getOrganizationId()));
        manifestData.put("location_id", "");
        manifestData.put("device_id", screen.getDeviceCode());
        manifestData.put("layout_id", manifest.getLayoutId());
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

        // 前端按 asset_id（字符串）查资源，这里把 slot_bindings 里的数字 media_id 投影为 asset_id
        projectSlotBindingAssetIds(manifestData.get("playback_plan"));

        List<Media> mediaList = manifestMediaRepository.findMediaByManifestId(manifest.getManifestId());
        List<ManifestMedia> manifestMediaItems = manifestMediaRepository.findByManifestId(manifest.getManifestId());
        Map<Long, ManifestMedia> mmMap = new HashMap<>();
        for (ManifestMedia mm : manifestMediaItems) {
            mmMap.put(mm.getMediaId(), mm);
        }

        List<Map<String, Object>> assetItems = new ArrayList<>();
        for (Media media : mediaList) {
            Map<String, Object> a = new LinkedHashMap<>();
            // asset_id 与 batch-url 保持一致：media.id 的字符串形式
            a.put("asset_id", String.valueOf(media.getId()));
            a.put("asset_type", media.getMediaType());
            a.put("file_name", media.getName());
            a.put("asset_ref", media.getObjectKey());
            a.put("file_url", media.getFileUrl());
            a.put("size_bytes", media.getFileSizeBytes());
            a.put("sha256", media.getChecksumSha256());
            a.put("duration_ms", media.getDurationSeconds() == null
                    ? 0L : media.getDurationSeconds() * 1000L);
            ManifestMedia mm = mmMap.get(media.getId());
            a.put("required", mm != null && mm.isRequired());
            a.put("priority", mm != null ? mm.getPriority() : 0);
            assetItems.add(a);
        }
        manifestData.put("assets", assetItems);
        manifestData.put("checksum", manifest.getChecksum());
        manifestData.put("generated_at", manifest.getGeneratedAt());

        return manifestData;
    }

    /**
     * 把 playback_plan.scenes[].slot_bindings[] 里的数字 media_id 投影为字符串 asset_id，
     * 以匹配前端按 asset_id 查找资源的契约。
     */
    @SuppressWarnings("unchecked")
    private void projectSlotBindingAssetIds(Object playbackPlan) {
        if (!(playbackPlan instanceof Map)) {
            return;
        }
        Object scenes = ((Map<String, Object>) playbackPlan).get("scenes");
        if (!(scenes instanceof List)) {
            return;
        }
        for (Object sceneObj : (List<Object>) scenes) {
            if (!(sceneObj instanceof Map)) {
                continue;
            }
            Object binds = ((Map<String, Object>) sceneObj).get("slot_bindings");
            if (!(binds instanceof List)) {
                continue;
            }
            for (Object bindObj : (List<Object>) binds) {
                if (bindObj instanceof Map) {
                    Map<String, Object> bind = (Map<String, Object>) bindObj;
                    Object mediaId = bind.get("media_id");
                    if (mediaId != null && bind.get("asset_id") == null) {
                        bind.put("asset_id", String.valueOf(mediaId));
                    }
                }
            }
        }
    }
}
