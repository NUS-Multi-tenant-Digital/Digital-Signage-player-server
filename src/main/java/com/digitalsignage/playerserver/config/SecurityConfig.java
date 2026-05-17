package com.digitalsignage.playerserver.config;

import com.digitalsignage.playerserver.security.DeviceTokenAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<DeviceTokenAuthFilter> deviceTokenFilter(DeviceTokenAuthFilter deviceTokenAuthFilter) {
        FilterRegistrationBean<DeviceTokenAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(deviceTokenAuthFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
