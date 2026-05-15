package com.digitalsignage.playerserver.controller;

import com.digitalsignage.playerserver.dto.request.PullManifestRequest;
import com.digitalsignage.playerserver.dto.response.ApiResponse;
import com.digitalsignage.playerserver.dto.response.PullManifestResponse;
import com.digitalsignage.playerserver.service.ManifestService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/player")
public class ManifestController {

    private final ManifestService manifestService;

    public ManifestController(ManifestService manifestService) {
        this.manifestService = manifestService;
    }

    @PostMapping("/manifest/pull")
    public ApiResponse<PullManifestResponse> pullManifest(@RequestBody PullManifestRequest request) {
        PullManifestResponse data = manifestService.pullManifest(request);
        return ApiResponse.ok(data);
    }
}
