package com.digitalsignage.playerserver.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfig {

    @Value("${app.aliyun.oss.endpoint:https://oss-ap-southeast-1.aliyuncs.com}")
    private String endpoint;

    @Value("${app.aliyun.oss.access-key-id:}")
    private String accessKeyId;

    @Value("${app.aliyun.oss.access-key-secret:}")
    private String accessKeySecret;

    @Bean
    public OSS ossClient() {
        if (accessKeyId == null || accessKeyId.isEmpty()) {
            // Return a dummy client that won't be used in tests
            return new OSSClientBuilder().build(endpoint, "dummy", "dummy");
        }
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
