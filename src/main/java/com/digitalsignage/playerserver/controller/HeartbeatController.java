package com.digitalsignage.playerserver.controller;

import com.digitalsignage.playerserver.dto.request.HeartbeatRequest;
import com.digitalsignage.playerserver.dto.response.ApiResponse;
import com.digitalsignage.playerserver.dto.response.HeartbeatResponse;
import com.digitalsignage.playerserver.service.HeartbeatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/player")
public class HeartbeatController {

    private final HeartbeatService heartbeatService;

    public HeartbeatController(HeartbeatService heartbeatService) {
        this.heartbeatService = heartbeatService;
    }

    @PostMapping("/heartbeat")
    public ApiResponse<HeartbeatResponse> heartbeat(@RequestBody HeartbeatRequest request) {
        HeartbeatResponse data = heartbeatService.reportHeartbeat(request);
        return ApiResponse.ok(data);
    }
}
