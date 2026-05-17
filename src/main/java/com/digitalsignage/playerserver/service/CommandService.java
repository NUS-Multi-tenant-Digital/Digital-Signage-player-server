package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.CommandAckRequest;
import com.digitalsignage.playerserver.dto.response.CommandAckResponse;
import com.digitalsignage.playerserver.entity.CommandAck;
import com.digitalsignage.playerserver.entity.Screen;
import com.digitalsignage.playerserver.repository.CommandAckRepository;
import com.digitalsignage.playerserver.repository.CommandRepository;
import com.digitalsignage.playerserver.repository.ScreenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CommandService {

    private final CommandRepository commandRepository;
    private final CommandAckRepository commandAckRepository;
    private final ScreenRepository screenRepository;

    public CommandService(CommandRepository commandRepository,
                          CommandAckRepository commandAckRepository,
                          ScreenRepository screenRepository) {
        this.commandRepository = commandRepository;
        this.commandAckRepository = commandAckRepository;
        this.screenRepository = screenRepository;
    }

    @Transactional
    public CommandAckResponse ackCommand(CommandAckRequest req) {
        long now = System.currentTimeMillis();

        // Resolve deviceCode (external device_id) to screen
        Screen screen = screenRepository.findByDeviceCode(req.getDeviceId()).orElse(null);
        if (screen == null) {
            CommandAckResponse resp = new CommandAckResponse();
            resp.setSuccess(false);
            return resp;
        }

        Long screenId = screen.getId();

        CommandAck ack = new CommandAck();
        ack.setCommandId(req.getCommandId());
        ack.setScreenId(screenId);
        ack.setType(req.getType());
        ack.setSuccess(req.isSuccess());
        ack.setErrorCode(req.getErrorCode());
        ack.setErrorMessage(req.getErrorMessage());
        ack.setExecutedAt(req.getExecutedAt());
        ack.setReceivedAt(now);
        ack.setCreatedAt(LocalDateTime.now());
        commandAckRepository.save(ack);

        // Update command status
        commandRepository.findByCommandId(req.getCommandId()).ifPresent(cmd -> {
            cmd.setStatus(req.isSuccess() ? "completed" : "failed");
            cmd.setUpdatedAt(LocalDateTime.now());
            commandRepository.save(cmd);
        });

        CommandAckResponse resp = new CommandAckResponse();
        resp.setSuccess(true);
        return resp;
    }
}
