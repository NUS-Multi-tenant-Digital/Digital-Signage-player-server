package com.digitalsignage.playerserver.controller;

import com.digitalsignage.playerserver.dto.request.ReportEventsRequest;
import com.digitalsignage.playerserver.dto.response.ApiResponse;
import com.digitalsignage.playerserver.dto.response.ReportEventsResponse;
import com.digitalsignage.playerserver.service.EventService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/player")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/events")
    public ApiResponse<ReportEventsResponse> reportEvents(@RequestBody ReportEventsRequest request) {
        ReportEventsResponse data = eventService.reportEvents(request);
        return ApiResponse.ok(data);
    }
}
