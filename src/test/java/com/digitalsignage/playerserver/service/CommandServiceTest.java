package com.digitalsignage.playerserver.service;

import com.digitalsignage.playerserver.dto.request.CommandAckRequest;
import com.digitalsignage.playerserver.dto.response.CommandAckResponse;
import com.digitalsignage.playerserver.entity.Command;
import com.digitalsignage.playerserver.entity.CommandAck;
import com.digitalsignage.playerserver.entity.Screen;
import com.digitalsignage.playerserver.repository.CommandAckRepository;
import com.digitalsignage.playerserver.repository.CommandRepository;
import com.digitalsignage.playerserver.repository.ScreenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommandServiceTest {

    @Mock private CommandRepository commandRepository;
    @Mock private CommandAckRepository commandAckRepository;
    @Mock private ScreenRepository screenRepository;

    private CommandService commandService;

    private Screen testScreen;

    @BeforeEach
    void setUp() {
        testScreen = new Screen();
        testScreen.setId(1L);
        testScreen.setDeviceCode("d1");

        when(screenRepository.findByDeviceCode("d1")).thenReturn(Optional.of(testScreen));

        commandService = new CommandService(commandRepository, commandAckRepository, screenRepository);
    }

    @Test
    @DisplayName("ACK 成功 - 命令状态更新为 completed")
    void ackCommand_success() {
        Command cmd = new Command();
        cmd.setCommandId("cmd_001");
        cmd.setStatus("pending");
        when(commandRepository.findByCommandId("cmd_001")).thenReturn(Optional.of(cmd));
        when(commandAckRepository.save(any(CommandAck.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandAckRequest req = new CommandAckRequest();
        req.setCommandId("cmd_001");
        req.setDeviceId("d1");
        req.setType("REFRESH_MANIFEST");
        req.setSuccess(true);
        req.setExecutedAt(System.currentTimeMillis());

        CommandAckResponse resp = commandService.ackCommand(req);

        assertThat(resp.isSuccess()).isTrue();
        verify(commandAckRepository).save(any(CommandAck.class));
        assertThat(cmd.getStatus()).isEqualTo("completed");
    }

    @Test
    @DisplayName("ACK 失败 - 命令状态更新为 failed")
    void ackCommand_failure() {
        Command cmd = new Command();
        cmd.setCommandId("cmd_002");
        cmd.setStatus("pending");
        when(commandRepository.findByCommandId("cmd_002")).thenReturn(Optional.of(cmd));
        when(commandAckRepository.save(any(CommandAck.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandAckRequest req = new CommandAckRequest();
        req.setCommandId("cmd_002");
        req.setDeviceId("d1");
        req.setType("CLEAR_CACHE");
        req.setSuccess(false);
        req.setErrorCode("EXEC_FAILED");
        req.setErrorMessage("Cache locked");
        req.setExecutedAt(System.currentTimeMillis());

        CommandAckResponse resp = commandService.ackCommand(req);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(cmd.getStatus()).isEqualTo("failed");
    }

    @Test
    @DisplayName("ACK 不存在的命令 - 仍然保存 ack 记录")
    void ackCommand_commandNotFound() {
        when(commandRepository.findByCommandId("cmd_999")).thenReturn(Optional.empty());
        when(commandAckRepository.save(any(CommandAck.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandAckRequest req = new CommandAckRequest();
        req.setCommandId("cmd_999");
        req.setDeviceId("d1");
        req.setType("RESTART");
        req.setSuccess(true);
        req.setExecutedAt(System.currentTimeMillis());

        CommandAckResponse resp = commandService.ackCommand(req);

        assertThat(resp.isSuccess()).isTrue();
        verify(commandAckRepository).save(any(CommandAck.class));
        verify(commandRepository, never()).save(any());
    }
}
