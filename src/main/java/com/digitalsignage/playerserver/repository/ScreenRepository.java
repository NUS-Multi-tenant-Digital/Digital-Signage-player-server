package com.digitalsignage.playerserver.repository;

import com.digitalsignage.playerserver.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ScreenRepository extends JpaRepository<Screen, Long> {
    Optional<Screen> findByDeviceCode(String deviceCode);
    Optional<Screen> findByActivationCode(String activationCode);
    Optional<Screen> findByDeviceToken(String deviceToken);
}
