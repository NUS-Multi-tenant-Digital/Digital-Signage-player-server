package com.digitalsignage.playerserver.repository;

import com.digitalsignage.playerserver.entity.Manifest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ManifestRepository extends JpaRepository<Manifest, Long> {
    Optional<Manifest> findByManifestId(String manifestId);
    Optional<Manifest> findFirstByDeviceIdOrderByIsActiveDescVersionDesc(String deviceId);
}
