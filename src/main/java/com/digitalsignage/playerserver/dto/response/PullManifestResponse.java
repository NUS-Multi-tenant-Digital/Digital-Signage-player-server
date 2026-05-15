package com.digitalsignage.playerserver.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PullManifestResponse {

    @JsonProperty("update_type")
    private String updateType;

    private Object manifest;

    @JsonProperty("next_poll_interval_sec")
    private int nextPollIntervalSec;

    @JsonProperty("server_time")
    private long serverTime;

    private String message;

    public String getUpdateType() {
        return updateType;
    }

    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }

    public Object getManifest() {
        return manifest;
    }

    public void setManifest(Object manifest) {
        this.manifest = manifest;
    }

    public int getNextPollIntervalSec() {
        return nextPollIntervalSec;
    }

    public void setNextPollIntervalSec(int nextPollIntervalSec) {
        this.nextPollIntervalSec = nextPollIntervalSec;
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
