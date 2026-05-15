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

        @JsonProperty("current_asset_id")
        private String currentAssetId;

        @JsonProperty("position_ms")
        private Integer positionMs;

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

        public String getCurrentAssetId() {
            return currentAssetId;
        }

        public void setCurrentAssetId(String currentAssetId) {
            this.currentAssetId = currentAssetId;
        }

        public Integer getPositionMs() {
            return positionMs;
        }

        public void setPositionMs(Integer positionMs) {
            this.positionMs = positionMs;
        }
    }

    public static class HealthInfo {

        @JsonProperty("cpu_percent")
        private Double cpuPercent;

        @JsonProperty("memory_percent")
        private Double memoryPercent;

        @JsonProperty("disk_free_mb")
        private Double diskFreeMb;

        private Double temperature;

        public Double getCpuPercent() {
            return cpuPercent;
        }

        public void setCpuPercent(Double cpuPercent) {
            this.cpuPercent = cpuPercent;
        }

        public Double getMemoryPercent() {
            return memoryPercent;
        }

        public void setMemoryPercent(Double memoryPercent) {
            this.memoryPercent = memoryPercent;
        }

        public Double getDiskFreeMb() {
            return diskFreeMb;
        }

        public void setDiskFreeMb(Double diskFreeMb) {
            this.diskFreeMb = diskFreeMb;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }
    }

    public static class CacheInfo {

        @JsonProperty("cached_asset_count")
        private Integer cachedAssetCount;

        @JsonProperty("cache_size_mb")
        private Double cacheSizeMb;

        @JsonProperty("cache_hit")
        private Boolean cacheHit;

        public Integer getCachedAssetCount() {
            return cachedAssetCount;
        }

        public void setCachedAssetCount(Integer cachedAssetCount) {
            this.cachedAssetCount = cachedAssetCount;
        }

        public Double getCacheSizeMb() {
            return cacheSizeMb;
        }

        public void setCacheSizeMb(Double cacheSizeMb) {
            this.cacheSizeMb = cacheSizeMb;
        }

        public Boolean getCacheHit() {
            return cacheHit;
        }

        public void setCacheHit(Boolean cacheHit) {
            this.cacheHit = cacheHit;
        }
    }

    public static class NetworkInfo {

        private boolean online;

        private String type;

        @JsonProperty("signal_quality")
        private Integer signalQuality;

        public boolean isOnline() {
            return online;
        }

        public void setOnline(boolean online) {
            this.online = online;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getSignalQuality() {
            return signalQuality;
        }

        public void setSignalQuality(Integer signalQuality) {
            this.signalQuality = signalQuality;
        }
    }
}
