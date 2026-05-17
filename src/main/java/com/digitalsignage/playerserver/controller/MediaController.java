package com.digitalsignage.playerserver.controller;

import com.digitalsignage.playerserver.dto.request.BatchGetAssetUrlRequest;
import com.digitalsignage.playerserver.dto.response.ApiResponse;
import com.digitalsignage.playerserver.dto.response.BatchAssetUrlResponse;
import com.digitalsignage.playerserver.service.MediaService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/assets/batch-url")
    public ApiResponse<BatchAssetUrlResponse> batchUrl(@RequestBody BatchGetAssetUrlRequest request) {
        BatchAssetUrlResponse data = mediaService.batchGetAssetUrls(request);
        return ApiResponse.ok(data);
    }
}
