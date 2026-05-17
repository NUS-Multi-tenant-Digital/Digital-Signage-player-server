package com.digitalsignage.playerserver.repository;

import com.digitalsignage.playerserver.entity.ManifestMedia;
import com.digitalsignage.playerserver.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ManifestMediaRepository extends JpaRepository<ManifestMedia, Long> {
    List<ManifestMedia> findByManifestId(String manifestId);

    @Query("SELECT m FROM Media m JOIN ManifestMedia mm ON m.id = mm.mediaId WHERE mm.manifestId = :manifestId")
    List<Media> findMediaByManifestId(@Param("manifestId") String manifestId);
}
