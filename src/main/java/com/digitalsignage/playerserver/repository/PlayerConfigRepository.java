package com.digitalsignage.playerserver.repository;

import com.digitalsignage.playerserver.entity.PlayerConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlayerConfigRepository extends JpaRepository<PlayerConfig, Long> {
    Optional<PlayerConfig> findByScreenId(Long screenId);
}
