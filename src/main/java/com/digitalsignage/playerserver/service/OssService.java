package com.digitalsignage.playerserver.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

@Service
public class OssService {

    private final OSS ossClient;
    private final String bucket;
    private final String publicBaseUrl;
    private final boolean enabled;

    public OssService(OSS ossClient,
                      @Value("${app.aliyun.oss.bucket:digital-signage-media}") String bucket,
                      @Value("${app.aliyun.oss.access-key-id:}") String accessKeyId,
                      @Value("${app.storage-public-base-url:}") String publicBaseUrl) {
        this.ossClient = ossClient;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
        this.enabled = accessKeyId != null && !accessKeyId.isEmpty();
    }

    /**
     * 生成带时效的签名下载 URL
     * @param ossPath 对象存储路径，如 "assets/video/demo.mp4"
     * @param expireMs URL 有效时长（毫秒）
     * @return 签名后的完整 URL
     */
    public String generateSignedUrl(String ossPath, long expireMs) {
        if (!enabled || ossPath == null || ossPath.isEmpty()) {
            return null;
        }

        // Strip protocol prefix if present (e.g. "oss://bucket/path" -> "path")
        String objectKey = ossPath;
        if (objectKey.startsWith("oss://")) {
            objectKey = objectKey.substring(objectKey.indexOf("/", 6) + 1);
        }

        try {
            Date expiration = new Date(System.currentTimeMillis() + expireMs);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey);
            request.setExpiration(expiration);
            URL url = ossClient.generatePresignedUrl(request);
            return url.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取公开访问 URL（通过 CDN / 公开 bucket）
     */
    public String getPublicUrl(String path) {
        if (publicBaseUrl == null || publicBaseUrl.isEmpty() || path == null) {
            return null;
        }
        String objectKey = path;
        if (objectKey.startsWith("oss://")) {
            objectKey = objectKey.substring(objectKey.indexOf("/", 6) + 1);
        }
        return publicBaseUrl.endsWith("/")
                ? publicBaseUrl + objectKey
                : publicBaseUrl + "/" + objectKey;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
