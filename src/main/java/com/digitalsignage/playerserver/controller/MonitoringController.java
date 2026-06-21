package com.digitalsignage.playerserver.controller;

import com.digitalsignage.playerserver.dto.response.ApiResponse;
import com.digitalsignage.playerserver.service.MonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/player")
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/devices/{deviceId}/monitoring")
    public ApiResponse<Map<String, Object>> getMonitoring(@PathVariable("deviceId") String deviceId) {
        Map<String, Object> data = monitoringService.getDeviceMonitoring(deviceId);
        if (data == null) {
            return ApiResponse.fail("DEVICE_NOT_FOUND", "Device not found: " + deviceId);
        }
        return ApiResponse.ok(data);
    }
}
