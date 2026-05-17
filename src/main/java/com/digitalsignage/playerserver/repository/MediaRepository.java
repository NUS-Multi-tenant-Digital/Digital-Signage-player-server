package com.digitalsignage.playerserver.repository;

import com.digitalsignage.playerserver.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByIdIn(List<Long> ids);
}
