package com.digitalsignage.playerserver.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchAssetUrlResponse {

    private List<AssetUrlItem> assets;

    public List<AssetUrlItem> getAssets() {
        return assets;
    }

    public void setAssets(List<AssetUrlItem> assets) {
        this.assets = assets;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AssetUrlItem {

        @JsonProperty("asset_id")
        private String assetId;

        @JsonProperty("download_url")
        private String downloadUrl;

        @JsonProperty("expire_at")
        private long expireAt;

        private String sha256;

        @JsonProperty("size_bytes")
        private long sizeBytes;

        public String getAssetId() {
            return assetId;
        }

        public void setAssetId(String assetId) {
            this.assetId = assetId;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }

        public long getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(long expireAt) {
            this.expireAt = expireAt;
        }

        public String getSha256() {
            return sha256;
        }

        public void setSha256(String sha256) {
            this.sha256 = sha256;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public void setSizeBytes(long sizeBytes) {
            this.sizeBytes = sizeBytes;
        }
    }
}
