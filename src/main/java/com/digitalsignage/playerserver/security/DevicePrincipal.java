package com.digitalsignage.playerserver.security;

public class DevicePrincipal {

    private final Long screenId;
    private final String deviceCode;

    public DevicePrincipal(Long screenId, String deviceCode) {
        this.screenId = screenId;
        this.deviceCode = deviceCode;
    }

    public Long getScreenId() {
        return screenId;
    }

    public String getDeviceCode() {
        return deviceCode;
    }
}
