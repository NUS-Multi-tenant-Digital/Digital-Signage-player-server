package com.digitalsignage.playerserver.repository;

import com.digitalsignage.playerserver.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findByAssetId(String assetId);
    List<Asset> findByAssetIdIn(List<String> assetIds);
}
