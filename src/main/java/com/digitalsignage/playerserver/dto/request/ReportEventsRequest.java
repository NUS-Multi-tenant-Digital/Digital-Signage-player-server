package com.digitalsignage.playerserver.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ReportEventsRequest {

    @JsonProperty("device_id")
    private String deviceId;

    private List<EventItem> events;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public List<EventItem> getEvents() {
        return events;
    }

    public void setEvents(List<EventItem> events) {
        this.events = events;
    }

    public static class EventItem {

        @JsonProperty("event_id")
        private String eventId;

        @JsonProperty("event_type")
        private String eventType;

        private long timestamp;

        @JsonProperty("manifest_id")
        private String manifestId;

        @JsonProperty("manifest_version")
        private Long manifestVersion;

        @JsonProperty("asset_id")
        private String assetId;

        @JsonProperty("playlist_item_id")
        private String playlistItemId;

        @JsonProperty("error_code")
        private String errorCode;

        @JsonProperty("error_message")
        private String errorMessage;

        @JsonProperty("extra_json")
        private String extraJson;

        public String getEventId() {
            return eventId;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getManifestId() {
            return manifestId;
        }

        public void setManifestId(String manifestId) {
            this.manifestId = manifestId;
        }

        public Long getManifestVersion() {
            return manifestVersion;
        }

        public void setManifestVersion(Long manifestVersion) {
            this.manifestVersion = manifestVersion;
        }

        public String getAssetId() {
            return assetId;
        }

        public void setAssetId(String assetId) {
            this.assetId = assetId;
        }

        public String getPlaylistItemId() {
            return playlistItemId;
        }

        public void setPlaylistItemId(String playlistItemId) {
            this.playlistItemId = playlistItemId;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getExtraJson() {
            return extraJson;
        }

        public void setExtraJson(String extraJson) {
            this.extraJson = extraJson;
        }
    }
}
