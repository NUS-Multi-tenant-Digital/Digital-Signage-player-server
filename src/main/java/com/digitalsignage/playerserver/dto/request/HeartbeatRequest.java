package com.digitalsignage.playerserver.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeartbeatRequest {

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("app_version")
    private String appVersion;

    @JsonProperty("manifest_id")
    private String manifestId;

    @JsonProperty("manifest_version")
    private int manifestVersion;

    private long timestamp;

    private PlaybackInfo playback;

    private HealthInfo health;

    private CacheInfo cache;

    private NetworkInfo network;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getManifestId() {
        return manifestId;
    }

    public void setManifestId(String manifestId) {
        this.manifestId = manifestId;
    }

    public int getManifestVersion() {
        return manifestVersion;
    }

    public void setManifestVersion(int manifestVersion) {
        this.manifestVersion = manifestVersion;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public PlaybackInfo getPlayback() {
        return playback;
    }

    public void setPlayback(PlaybackInfo playback) {
        this.playback = playback;
    }

    public HealthInfo getHealth() {
        return health;
    }

    public void setHealth(HealthInfo health) {
        this.health = health;
    }

    public CacheInfo getCache() {
        return cache;
    }

    public void setCache(CacheInfo cache) {
        this.cache = cache;
    }

    public NetworkInfo getNetwork() {
        return network;
    }

    public void setNetwork(NetworkInfo network) {
        this.network = network;
    }

    public static class PlaybackInfo {

        private String state;

        @JsonProperty("current_scene_id")
        private String currentSceneId;

        @JsonProperty("current_slot_id")
        private String currentSlotId;

        @JsonProperty("current_asset_id")
        private String currentAssetId;

        @JsonProperty("position_ms")
        private Long positionMs;

        @JsonProperty("duration_ms")
        private Long durationMs;

        @JsonProperty("last_error_code")
        private String lastErrorCode;

        @JsonProperty("last_error_message")
        private String lastErrorMessage;

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getCurrentSceneId() {
            return currentSceneId;
        }

        public void setCurrentSceneId(String currentSceneId) {
            this.currentSceneId = currentSceneId;
        }

        public String getCurrentSlotId() {
            return currentSlotId;
        }

        public void setCurrentSlotId(String currentSlotId) {
            this.currentSlotId = currentSlotId;
        }

        public String getCurrentAssetId() {
            return currentAssetId;
        }

        public void setCurrentAssetId(String currentAssetId) {
            this.currentAssetId = currentAssetId;
        }

        public Long getPositionMs() {
            return positionMs;
        }

        public void setPositionMs(Long positionMs) {
            this.positionMs = positionMs;
        }

        public Long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(Long durationMs) {
            this.durationMs = durationMs;
        }

        public String getLastErrorCode() {
            return lastErrorCode;
        }

        public void setLastErrorCode(String lastErrorCode) {
            this.lastErrorCode = lastErrorCode;
        }

        public String getLastErrorMessage() {
            return lastErrorMessage;
        }

        public void setLastErrorMessage(String lastErrorMessage) {
            this.lastErrorMessage = lastErrorMessage;
        }
    }

    public static class HealthInfo {

        @JsonProperty("uptime_sec")
        private Long uptimeSec;

        @JsonProperty("memory_usage_mb")
        private Double memoryUsageMb;

        @JsonProperty("storage_free_mb")
        private Double storageFreeMb;

        @JsonProperty("storage_total_mb")
        private Double storageTotalMb;

        @JsonProperty("cpu_temperature_celsius")
        private Double cpuTemperatureCelsius;

        @JsonProperty("app_foreground")
        private Boolean appForeground;

        public Long getUptimeSec() {
            return uptimeSec;
        }

        public void setUptimeSec(Long uptimeSec) {
            this.uptimeSec = uptimeSec;
        }

        public Double getMemoryUsageMb() {
            return memoryUsageMb;
        }

        public void setMemoryUsageMb(Double memoryUsageMb) {
            this.memoryUsageMb = memoryUsageMb;
        }

        public Double getStorageFreeMb() {
            return storageFreeMb;
        }

        public void setStorageFreeMb(Double storageFreeMb) {
            this.storageFreeMb = storageFreeMb;
        }

        public Double getStorageTotalMb() {
            return storageTotalMb;
        }

        public void setStorageTotalMb(Double storageTotalMb) {
            this.storageTotalMb = storageTotalMb;
        }

        public Double getCpuTemperatureCelsius() {
            return cpuTemperatureCelsius;
        }

        public void setCpuTemperatureCelsius(Double cpuTemperatureCelsius) {
            this.cpuTemperatureCelsius = cpuTemperatureCelsius;
        }

        public Boolean getAppForeground() {
            return appForeground;
        }

        public void setAppForeground(Boolean appForeground) {
            this.appForeground = appForeground;
        }
    }

    public static class CacheInfo {

        @JsonProperty("used_cache_mb")
        private Double usedCacheMb;

        @JsonProperty("max_cache_mb")
        private Double maxCacheMb;

        @JsonProperty("cached_asset_count")
        private Integer cachedAssetCount;

        @JsonProperty("missing_required_asset_count")
        private Integer missingRequiredAssetCount;

        @JsonProperty("last_cache_cleanup_at")
        private Long lastCacheCleanupAt;

        public Double getUsedCacheMb() {
            return usedCacheMb;
        }

        public void setUsedCacheMb(Double usedCacheMb) {
            this.usedCacheMb = usedCacheMb;
        }

        public Double getMaxCacheMb() {
            return maxCacheMb;
        }

        public void setMaxCacheMb(Double maxCacheMb) {
            this.maxCacheMb = maxCacheMb;
        }

        public Integer getCachedAssetCount() {
            return cachedAssetCount;
        }

        public void setCachedAssetCount(Integer cachedAssetCount) {
            this.cachedAssetCount = cachedAssetCount;
        }

        public Integer getMissingRequiredAssetCount() {
            return missingRequiredAssetCount;
        }

        public void setMissingRequiredAssetCount(Integer missingRequiredAssetCount) {
            this.missingRequiredAssetCount = missingRequiredAssetCount;
        }

        public Long getLastCacheCleanupAt() {
            return lastCacheCleanupAt;
        }

        public void setLastCacheCleanupAt(Long lastCacheCleanupAt) {
            this.lastCacheCleanupAt = lastCacheCleanupAt;
        }
    }

    public static class NetworkInfo {

        private boolean online;

        @JsonProperty("connection_type")
        private String connectionType;

        @JsonProperty("failed_request_count")
        private Integer failedRequestCount;

        @JsonProperty("last_online_at")
        private Long lastOnlineAt;

        @JsonProperty("last_offline_at")
        private Long lastOfflineAt;

        public boolean isOnline() {
            return online;
        }

        public void setOnline(boolean online) {
            this.online = online;
        }

        public String getConnectionType() {
            return connectionType;
        }

        public void setConnectionType(String connectionType) {
            this.connectionType = connectionType;
        }

        public Integer getFailedRequestCount() {
            return failedRequestCount;
        }

        public void setFailedRequestCount(Integer failedRequestCount) {
            this.failedRequestCount = failedRequestCount;
        }

        public Long getLastOnlineAt() {
            return lastOnlineAt;
        }

        public void setLastOnlineAt(Long lastOnlineAt) {
            this.lastOnlineAt = lastOnlineAt;
        }

        public Long getLastOfflineAt() {
            return lastOfflineAt;
        }

        public void setLastOfflineAt(Long lastOfflineAt) {
            this.lastOfflineAt = lastOfflineAt;
        }
    }
}
