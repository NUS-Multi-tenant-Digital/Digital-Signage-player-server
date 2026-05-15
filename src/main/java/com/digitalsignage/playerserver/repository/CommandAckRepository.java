package com.digitalsignage.playerserver.repository;

import com.digitalsignage.playerserver.entity.CommandAck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommandAckRepository extends JpaRepository<CommandAck, Long> {
    Optional<CommandAck> findByCommandId(String commandId);
}
