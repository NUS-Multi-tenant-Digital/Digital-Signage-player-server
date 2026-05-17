package com.digitalsignage.playerserver.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "manifests")
public class Manifest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manifest_id", nullable = false, unique = true, length = 64)
    private String manifestId;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "screen_id", nullable = false)
    private Long screenId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "layout_id")
    private Long layoutId;

    @Column(name = "valid_from", nullable = false)
    private long validFrom;

    @Column(name = "valid_to", nullable = false)
    private long validTo;

    @Column(name = "ttl_sec", nullable = false)
    private int ttlSec;

    @Column(name = "template_config_json", nullable = false, columnDefinition = "JSON")
    private String templateConfigJson;

    @Column(name = "playback_plan_json", nullable = false, columnDefinition = "JSON")
    private String playbackPlanJson;

    @Column(name = "cache_policy_json", nullable = false, columnDefinition = "JSON")
    private String cachePolicyJson;

    @Column(name = "fallback_policy_json", nullable = false, columnDefinition = "JSON")
    private String fallbackPolicyJson;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @Column(name = "generated_at", nullable = false)
    private long generatedAt;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT")
    private boolean isActive;

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

    public String getManifestId() {
        return manifestId;
    }

    public void setManifestId(String manifestId) {
        this.manifestId = manifestId;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Long getScreenId() {
        return screenId;
    }

    public void setScreenId(Long screenId) {
        this.screenId = screenId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Long getLayoutId() {
        return layoutId;
    }

    public void setLayoutId(Long layoutId) {
        this.layoutId = layoutId;
    }

    public long getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(long validFrom) {
        this.validFrom = validFrom;
    }

    public long getValidTo() {
        return validTo;
    }

    public void setValidTo(long validTo) {
        this.validTo = validTo;
    }

    public int getTtlSec() {
        return ttlSec;
    }

    public void setTtlSec(int ttlSec) {
        this.ttlSec = ttlSec;
    }

    public String getTemplateConfigJson() {
        return templateConfigJson;
    }

    public void setTemplateConfigJson(String templateConfigJson) {
        this.templateConfigJson = templateConfigJson;
    }

    public String getPlaybackPlanJson() {
        return playbackPlanJson;
    }

    public void setPlaybackPlanJson(String playbackPlanJson) {
        this.playbackPlanJson = playbackPlanJson;
    }

    public String getCachePolicyJson() {
        return cachePolicyJson;
    }

    public void setCachePolicyJson(String cachePolicyJson) {
        this.cachePolicyJson = cachePolicyJson;
    }

    public String getFallbackPolicyJson() {
        return fallbackPolicyJson;
    }

    public void setFallbackPolicyJson(String fallbackPolicyJson) {
        this.fallbackPolicyJson = fallbackPolicyJson;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
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
