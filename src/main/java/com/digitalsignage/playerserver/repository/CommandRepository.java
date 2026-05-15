package com.digitalsignage.playerserver.repository;

import com.digitalsignage.playerserver.entity.Command;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommandRepository extends JpaRepository<Command, Long> {
    Optional<Command> findByCommandId(String commandId);
    List<Command> findByDeviceIdAndStatusAndExpireAtGreaterThan(String deviceId, String status, long now);
}
