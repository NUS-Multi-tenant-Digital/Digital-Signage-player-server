package com.digitalsignage.playerserver.controller;

import com.digitalsignage.playerserver.dto.request.CommandAckRequest;
import com.digitalsignage.playerserver.dto.response.ApiResponse;
import com.digitalsignage.playerserver.dto.response.CommandAckResponse;
import com.digitalsignage.playerserver.service.CommandService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/player")
public class CommandController {

    private final CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/commands/ack")
    public ApiResponse<CommandAckResponse> ackCommand(@RequestBody CommandAckRequest request) {
        CommandAckResponse data = commandService.ackCommand(request);
        return ApiResponse.ok(data);
    }
}
