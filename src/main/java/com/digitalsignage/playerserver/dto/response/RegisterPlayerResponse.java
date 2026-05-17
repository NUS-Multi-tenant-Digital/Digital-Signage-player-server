package com.digitalsignage.playerserver.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterPlayerResponse {

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_expire_at")
    private long tokenExpireAt;

    private PlayerConfigDto config;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getTokenExpireAt() {
        return tokenExpireAt;
    }

    public void setTokenExpireAt(long tokenExpireAt) {
        this.tokenExpireAt = tokenExpireAt;
    }

    public PlayerConfigDto getConfig() {
        return config;
    }

    public void setConfig(PlayerConfigDto config) {
        this.config = config;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PlayerConfigDto {

        @JsonProperty("device_id")
        private String deviceId;

        @JsonProperty("heartbeat_interval_sec")
        private Integer heartbeatIntervalSec;

        @JsonProperty("manifest_sync_interval_sec")
        private Integer manifestSyncIntervalSec;

        @JsonProperty("event_flush_interval_sec")
        private Integer eventFlushIntervalSec;

        @JsonProperty("max_cache_size_mb")
        private Integer maxCacheSizeMb;

        @JsonProperty("asset_download_concurrency")
        private Integer assetDownloadConcurrency;

        @JsonProperty("enable_offline_mode")
        private Boolean enableOfflineMode;

        @JsonProperty("enable_watchdog")
        private Boolean enableWatchdog;

        @JsonProperty("enable_screenshot")
        private Boolean enableScreenshot;

        @JsonProperty("log_level")
        private String logLevel;

        @JsonProperty("supported_asset_types")
        private List<String> supportedAssetTypes;

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public Integer getHeartbeatIntervalSec() {
            return heartbeatIntervalSec;
        }

        public void setHeartbeatIntervalSec(Integer heartbeatIntervalSec) {
            this.heartbeatIntervalSec = heartbeatIntervalSec;
        }

        public Integer getManifestSyncIntervalSec() {
            return manifestSyncIntervalSec;
        }

        public void setManifestSyncIntervalSec(Integer manifestSyncIntervalSec) {
            this.manifestSyncIntervalSec = manifestSyncIntervalSec;
        }

        public Integer getEventFlushIntervalSec() {
            return eventFlushIntervalSec;
        }

        public void setEventFlushIntervalSec(Integer eventFlushIntervalSec) {
            this.eventFlushIntervalSec = eventFlushIntervalSec;
        }

        public Integer getMaxCacheSizeMb() {
            return maxCacheSizeMb;
        }

        public void setMaxCacheSizeMb(Integer maxCacheSizeMb) {
            this.maxCacheSizeMb = maxCacheSizeMb;
        }

        public Integer getAssetDownloadConcurrency() {
            return assetDownloadConcurrency;
        }

        public void setAssetDownloadConcurrency(Integer assetDownloadConcurrency) {
            this.assetDownloadConcurrency = assetDownloadConcurrency;
        }

        public Boolean getEnableOfflineMode() {
            return enableOfflineMode;
        }

        public void setEnableOfflineMode(Boolean enableOfflineMode) {
            this.enableOfflineMode = enableOfflineMode;
        }

        public Boolean getEnableWatchdog() {
            return enableWatchdog;
        }

        public void setEnableWatchdog(Boolean enableWatchdog) {
            this.enableWatchdog = enableWatchdog;
        }

        public Boolean getEnableScreenshot() {
            return enableScreenshot;
        }

        public void setEnableScreenshot(Boolean enableScreenshot) {
            this.enableScreenshot = enableScreenshot;
        }

        public String getLogLevel() {
            return logLevel;
        }

        public void setLogLevel(String logLevel) {
            this.logLevel = logLevel;
        }

        public List<String> getSupportedAssetTypes() {
            return supportedAssetTypes;
        }

        public void setSupportedAssetTypes(List<String> supportedAssetTypes) {
            this.supportedAssetTypes = supportedAssetTypes;
        }
    }
}
