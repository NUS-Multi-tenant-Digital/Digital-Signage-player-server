package com.digitalsignage.playerserver.repository;

import com.digitalsignage.playerserver.entity.ManifestAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.digitalsignage.playerserver.entity.Asset;
import java.util.List;

public interface ManifestAssetRepository extends JpaRepository<ManifestAsset, Long> {
    List<ManifestAsset> findByManifestId(String manifestId);

    @Query("SELECT a FROM Asset a JOIN ManifestAsset ma ON a.assetId = ma.assetId WHERE ma.manifestId = :manifestId")
    List<Asset> findAssetsByManifestId(@Param("manifestId") String manifestId);
}
