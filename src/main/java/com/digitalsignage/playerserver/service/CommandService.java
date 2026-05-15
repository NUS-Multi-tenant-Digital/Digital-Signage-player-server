package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.CommandAckRequest;
import com.digitalsignage.playerserver.dto.response.CommandAckResponse;
import com.digitalsignage.playerserver.entity.Command;
import com.digitalsignage.playerserver.entity.CommandAck;
import com.digitalsignage.playerserver.repository.CommandAckRepository;
import com.digitalsignage.playerserver.repository.CommandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CommandService {

    private final CommandRepository commandRepository;
    private final CommandAckRepository commandAckRepository;

    public CommandService(CommandRepository commandRepository,
                          CommandAckRepository commandAckRepository) {
        this.commandRepository = commandRepository;
        this.commandAckRepository = commandAckRepository;
    }

    @Transactional
    public CommandAckResponse ackCommand(CommandAckRequest req) {
        long now = System.currentTimeMillis();

        CommandAck ack = new CommandAck();
        ack.setCommandId(req.getCommandId());
        ack.setDeviceId(req.getDeviceId());
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
