package com.digitalsignage.playerserver.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PullManifestRequest {

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("location_id")
    private String locationId;

    @JsonProperty("current_manifest_id")
    private String currentManifestId;

    @JsonProperty("current_manifest_version")
    private Integer currentManifestVersion;

    @JsonProperty("app_version")
    private String appVersion;

    private String platform;

    @JsonProperty("screen_resolution")
    private String screenResolution;

    @JsonProperty("last_success_sync_at")
    private Long lastSuccessSyncAt;

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

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getCurrentManifestId() {
        return currentManifestId;
    }

    public void setCurrentManifestId(String currentManifestId) {
        this.currentManifestId = currentManifestId;
    }

    public Integer getCurrentManifestVersion() {
        return currentManifestVersion;
    }

    public void setCurrentManifestVersion(Integer currentManifestVersion) {
        this.currentManifestVersion = currentManifestVersion;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getScreenResolution() {
        return screenResolution;
    }

    public void setScreenResolution(String screenResolution) {
        this.screenResolution = screenResolution;
    }

    public Long getLastSuccessSyncAt() {
        return lastSuccessSyncAt;
    }

    public void setLastSuccessSyncAt(Long lastSuccessSyncAt) {
        this.lastSuccessSyncAt = lastSuccessSyncAt;
    }
}
