package com.swordshield.content.repository;

import com.swordshield.content.domain.MediaAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    List<MediaAsset> findByArticleId(Long articleId);
    Page<MediaAsset> findByTypeIgnoreCase(String type, Pageable pageable);
}
