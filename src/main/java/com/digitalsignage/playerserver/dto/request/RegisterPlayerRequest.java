package com.digitalsignage.playerserver.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class RegisterPlayerRequest {

    @JsonProperty("device_sn")
    private String deviceSn;

    @JsonProperty("activation_code")
    private String activationCode;

    @JsonProperty("device_name")
    private String deviceName;

    private String platform;

    @JsonProperty("app_version")
    private String appVersion;

    @JsonProperty("os_version")
    private String osVersion;

    @JsonProperty("firmware_version")
    private String firmwareVersion;

    @JsonProperty("screen_resolution")
    private String screenResolution;

    private String timezone;

    @JsonProperty("mac_address")
    private String macAddress;

    @JsonProperty("ip_address")
    private String ipAddress;

    private Map<String, Object> capabilities;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("location_id")
    private String locationId;

    public String getDeviceSn() {
        return deviceSn;
    }

    public void setDeviceSn(String deviceSn) {
        this.deviceSn = deviceSn;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getScreenResolution() {
        return screenResolution;
    }

    public void setScreenResolution(String screenResolution) {
        this.screenResolution = screenResolution;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Map<String, Object> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Map<String, Object> capabilities) {
        this.capabilities = capabilities;
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
}
