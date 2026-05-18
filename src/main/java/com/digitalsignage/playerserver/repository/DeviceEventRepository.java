package com.digitalsignage.playerserver.repository;

import com.digitalsignage.playerserver.entity.DeviceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, Long> {
    List<DeviceEvent> findTop100ByScreenIdOrderByCreatedAtDesc(Long screenId);
}
