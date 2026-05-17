package com.digitalsignage.playerserver.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "screen")
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_code", nullable = false, unique = true, length = 64)
    private String deviceCode;

    @Column(name = "device_token", length = 512)
    private String deviceToken;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "activation_code", length = 128)
    private String activationCode;

    @Column(name = "activation_status", length = 32)
    private String activationStatus;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "app_version", length = 64)
    private String appVersion;

    @Column(name = "resolution_width")
    private Integer resolutionWidth;

    @Column(name = "resolution_height")
    private Integer resolutionHeight;

    @Column(name = "screen_group_id")
    private Long screenGroupId;

    @Column(name = "ws_status", length = 32)
    private String wsStatus;

    @Column(name = "last_ws_connected_at")
    private LocalDateTime lastWsConnectedAt;

    @Column(name = "last_ws_message_at")
    private LocalDateTime lastWsMessageAt;

    @Column(name = "probe_fail_count")
    private Integer probeFailCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    public String getActivationStatus() {
        return activationStatus;
    }

    public void setActivationStatus(String activationStatus) {
        this.activationStatus = activationStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public Integer getResolutionWidth() {
        return resolutionWidth;
    }

    public void setResolutionWidth(Integer resolutionWidth) {
        this.resolutionWidth = resolutionWidth;
    }

    public Integer getResolutionHeight() {
        return resolutionHeight;
    }

    public void setResolutionHeight(Integer resolutionHeight) {
        this.resolutionHeight = resolutionHeight;
    }

    public Long getScreenGroupId() {
        return screenGroupId;
    }

    public void setScreenGroupId(Long screenGroupId) {
        this.screenGroupId = screenGroupId;
    }

    public String getWsStatus() {
        return wsStatus;
    }

    public void setWsStatus(String wsStatus) {
        this.wsStatus = wsStatus;
    }

    public LocalDateTime getLastWsConnectedAt() {
        return lastWsConnectedAt;
    }

    public void setLastWsConnectedAt(LocalDateTime lastWsConnectedAt) {
        this.lastWsConnectedAt = lastWsConnectedAt;
    }

    public LocalDateTime getLastWsMessageAt() {
        return lastWsMessageAt;
    }

    public void setLastWsMessageAt(LocalDateTime lastWsMessageAt) {
        this.lastWsMessageAt = lastWsMessageAt;
    }

    public Integer getProbeFailCount() {
        return probeFailCount;
    }

    public void setProbeFailCount(Integer probeFailCount) {
        this.probeFailCount = probeFailCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
