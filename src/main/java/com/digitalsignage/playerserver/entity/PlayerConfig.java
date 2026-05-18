package com.digitalsignage.playerserver.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_configs")
public class PlayerConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "screen_id", nullable = false, unique = true)
    private Long screenId;

    @Column(name = "heartbeat_interval_sec", nullable = false)
    private int heartbeatIntervalSec;

    @Column(name = "manifest_sync_interval_sec", nullable = false)
    private int manifestSyncIntervalSec;

    @Column(name = "event_flush_interval_sec", nullable = false)
    private int eventFlushIntervalSec;

    @Column(name = "max_cache_size_mb", nullable = false)
    private long maxCacheSizeMb;

    @Column(name = "asset_download_concurrency", nullable = false)
    private int assetDownloadConcurrency;

    @Column(name = "enable_offline_mode", nullable = false)
    private boolean enableOfflineMode;

    @Column(name = "enable_watchdog", nullable = false)
    private boolean enableWatchdog;

    @Column(name = "enable_screenshot", nullable = false)
    private boolean enableScreenshot;

    @Column(name = "log_level", nullable = false, length = 16)
    private String logLevel;

    @Column(name = "supported_asset_types_json", nullable = false, columnDefinition = "JSON")
    private String supportedAssetTypesJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScreenId() {
        return screenId;
    }

    public void setScreenId(Long screenId) {
        this.screenId = screenId;
    }

    public int getHeartbeatIntervalSec() {
        return heartbeatIntervalSec;
    }

    public void setHeartbeatIntervalSec(int heartbeatIntervalSec) {
        this.heartbeatIntervalSec = heartbeatIntervalSec;
    }

    public int getManifestSyncIntervalSec() {
        return manifestSyncIntervalSec;
    }

    public void setManifestSyncIntervalSec(int manifestSyncIntervalSec) {
        this.manifestSyncIntervalSec = manifestSyncIntervalSec;
    }

    public int getEventFlushIntervalSec() {
        return eventFlushIntervalSec;
    }

    public void setEventFlushIntervalSec(int eventFlushIntervalSec) {
        this.eventFlushIntervalSec = eventFlushIntervalSec;
    }

    public long getMaxCacheSizeMb() {
        return maxCacheSizeMb;
    }

    public void setMaxCacheSizeMb(long maxCacheSizeMb) {
        this.maxCacheSizeMb = maxCacheSizeMb;
    }

    public int getAssetDownloadConcurrency() {
        return assetDownloadConcurrency;
    }

    public void setAssetDownloadConcurrency(int assetDownloadConcurrency) {
        this.assetDownloadConcurrency = assetDownloadConcurrency;
    }

    public boolean isEnableOfflineMode() {
        return enableOfflineMode;
    }

    public void setEnableOfflineMode(boolean enableOfflineMode) {
        this.enableOfflineMode = enableOfflineMode;
    }

    public boolean isEnableWatchdog() {
        return enableWatchdog;
    }

    public void setEnableWatchdog(boolean enableWatchdog) {
        this.enableWatchdog = enableWatchdog;
    }

    public boolean isEnableScreenshot() {
        return enableScreenshot;
    }

    public void setEnableScreenshot(boolean enableScreenshot) {
        this.enableScreenshot = enableScreenshot;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getSupportedAssetTypesJson() {
        return supportedAssetTypesJson;
    }

    public void setSupportedAssetTypesJson(String supportedAssetTypesJson) {
        this.supportedAssetTypesJson = supportedAssetTypesJson;
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
