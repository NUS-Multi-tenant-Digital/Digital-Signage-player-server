package com.digitalsignage.playerserver.repository;

import com.digitalsignage.playerserver.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceId(String deviceId);
    Optional<Device> findByDeviceSn(String deviceSn);
}
