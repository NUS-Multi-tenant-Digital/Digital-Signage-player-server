package com.digitalsignage.playerserver.repository;

import com.digitalsignage.playerserver.entity.Command;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommandRepository extends JpaRepository<Command, Long> {
    Optional<Command> findByCommandId(String commandId);
    List<Command> findByScreenIdAndStatusAndExpireAtGreaterThan(Long screenId, String status, long now);
}
