package com.swordshield.content.service;

import com.swordshield.common.PageResult;
import com.swordshield.common.cache.CacheSupport;
import com.swordshield.content.domain.Achievement;
import com.swordshield.content.domain.ContentArticle;
import com.swordshield.content.domain.Exhibitor;
import com.swordshield.content.domain.Material;
import com.swordshield.content.domain.MediaAsset;
import com.swordshield.content.dto.AchievementDto;
import com.swordshield.content.dto.AdminArticleUpsertRequest;
import com.swordshield.content.dto.ContentArticleDto;
import com.swordshield.content.dto.ExhibitorDto;
import com.swordshield.content.dto.MaterialDto;
import com.swordshield.content.dto.MediaAssetDto;
import com.swordshield.content.repository.AchievementRepository;
import com.swordshield.content.repository.ContentArticleRepository;
import com.swordshield.content.repository.ExhibitorRepository;
import com.swordshield.content.repository.MaterialRepository;
import com.swordshield.content.repository.MediaAssetRepository;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Optional;

@Service
public class ContentService {
    private final ContentArticleRepository articleRepo;
    private final ExhibitorRepository exhibitorRepo;
    private final AchievementRepository achievementRepo;
    private final MaterialRepository materialRepo;
    private final MediaAssetRepository mediaRepo;
    private final CacheManager cacheManager;

    public ContentService(ContentArticleRepository articleRepo,
                          ExhibitorRepository exhibitorRepo,
                          AchievementRepository achievementRepo,
                          MaterialRepository materialRepo,
                          MediaAssetRepository mediaRepo,
                          CacheManager cacheManager) {
        this.articleRepo = articleRepo;
        this.exhibitorRepo = exhibitorRepo;
        this.achievementRepo = achievementRepo;
        this.materialRepo = materialRepo;
        this.mediaRepo = mediaRepo;
        this.cacheManager = cacheManager;
    }

    @Cacheable(cacheNames = "content:articles",
            key = "'p=' + #page + ':s=' + #size + ':c=' + (#category == null ? '' : #category.trim()) + ':x=' + (#excludeCategory == null ? '' : #excludeCategory.trim())",
            sync = true)
    public PageResult<ContentArticleDto> listArticles(int page, int size, String category, String excludeCategory) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size), Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<ContentArticle> p;
        if (category != null && !category.isBlank()) {
            p = articleRepo.findPublishedArticlesByCategory(category.trim(), pageable);
        } else if (excludeCategory != null && !excludeCategory.isBlank()) {
            p = articleRepo.findPublishedArticlesExcludeCategory(excludeCategory.trim(), pageable);
        } else {
            // Only return published articles (status = 1 and publishedAt is not null)
            p = articleRepo.findPublishedArticles(pageable);
        }
        return PageResult.of(p.getContent().stream().map(this::toDto).toList(), p.getTotalElements(), p.getNumber(), p.getSize());
    }

    public PageResult<ContentArticleDto> listAdminArticles(int page, int size, String category, Integer status, String excludeCategory) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size), Sort.by(Sort.Direction.DESC, "id"));

        boolean hasCategory = category != null && !category.isBlank();
        boolean hasStatus = status != null;
        boolean hasExcludeCategory = excludeCategory != null && !excludeCategory.isBlank();

        Page<ContentArticle> p;
        if (hasCategory && hasStatus) {
            p = articleRepo.findByStatusAndCategory(status, category, pageable);
        } else if (hasStatus) {
            if (hasExcludeCategory) {
                p = articleRepo.findByStatusAndCategoryNot(status, excludeCategory, pageable);
            } else {
                p = articleRepo.findByStatus(status, pageable);
            }
        } else if (hasCategory) {
            p = articleRepo.findByCategory(category, pageable);
        } else if (hasExcludeCategory) {
            p = articleRepo.findByCategoryNot(excludeCategory, pageable);
        } else {
            p = articleRepo.findAll(pageable);
        }
        return PageResult.of(p.getContent().stream().map(this::toDto).toList(), p.getTotalElements(), p.getNumber(), p.getSize());
    }

    @CacheEvict(cacheNames = "content:articles", allEntries = true)
    public ContentArticleDto createAdminArticle(AdminArticleUpsertRequest input, String adminSubject) {
        if (input == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (input.getTitle() == null || input.getTitle().isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (input.getSummary() == null || input.getSummary().isBlank()) {
            throw new IllegalArgumentException("摘要不能为空");
        }
        if (input.getBody() == null || input.getBody().isBlank()) {
            throw new IllegalArgumentException("正文内容不能为空");
        }

        int status = input.getStatus();
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("状态非法，仅支持 0(草稿)/1(发布)");
        }

        ContentArticle a = new ContentArticle();
        a.setTitle(input.getTitle());
        a.setSummary(input.getSummary());
        a.setBody(input.getBody());
        a.setCoverUrl(input.getCoverUrl());
        a.setCategory(Optional.ofNullable(input.getCategory()).filter(s -> !s.isBlank()).orElse("通用"));
        a.setLiveUrl(input.getLiveUrl());
        a.setReplayUrl(input.getReplayUrl());
        a.setStatus(status);

        String author = input.getAuthor();
        if (author == null || author.isBlank()) {
            author = adminSubject != null && adminSubject.startsWith("admin_") ? adminSubject.substring("admin_".length()) : adminSubject;
        }
        a.setAuthor(author);

        if (status == 1) {
            a.setPublishedAt(input.getPublishedAt() != null ? input.getPublishedAt() : Instant.now());
        } else {
            a.setPublishedAt(null);
        }

        return toDto(articleRepo.save(a));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "content:articles", allEntries = true),
            @CacheEvict(cacheNames = "content:article", key = "'id=' + #id")
    })
    public ContentArticleDto updateAdminArticle(Long id, AdminArticleUpsertRequest input) {
        if (id == null) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        ContentArticle existing = articleRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        if (input == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (input.getTitle() == null || input.getTitle().isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (input.getSummary() == null || input.getSummary().isBlank()) {
            throw new IllegalArgumentException("摘要不能为空");
        }
        if (input.getBody() == null || input.getBody().isBlank()) {
            throw new IllegalArgumentException("正文内容不能为空");
        }

        int status = input.getStatus();
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("状态非法，仅支持 0(草稿)/1(发布)");
        }

        existing.setTitle(input.getTitle());
        existing.setSummary(input.getSummary());
        existing.setBody(input.getBody());
        existing.setCoverUrl(input.getCoverUrl());
        existing.setCategory(Optional.ofNullable(input.getCategory()).filter(s -> !s.isBlank()).orElse("通用"));
        existing.setLiveUrl(input.getLiveUrl());
        existing.setReplayUrl(input.getReplayUrl());
        existing.setAuthor(input.getAuthor());
        existing.setStatus(status);

        if (status == 1) {
            if (input.getPublishedAt() != null) {
                existing.setPublishedAt(input.getPublishedAt());
            } else if (existing.getPublishedAt() == null) {
                existing.setPublishedAt(Instant.now());
            }
        } else {
            existing.setPublishedAt(null);
        }

        return toDto(articleRepo.save(existing));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "content:articles", allEntries = true),
            @CacheEvict(cacheNames = "content:article", key = "'id=' + #id")
    })
    public void deleteAdminArticle(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        if (!articleRepo.existsById(id)) {
            throw new IllegalArgumentException("not found");
        }
        articleRepo.deleteById(id);
    }

    public ContentArticleDto submitArticle(String title, String summary, String body, String coverUrl, String authorCategory) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("摘要不能为空");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("正文内容不能为空");
        }
        ContentArticle a = new ContentArticle();
        a.setTitle(title);
        a.setSummary(summary);
        a.setBody(body);
        a.setCoverUrl(coverUrl);
        a.setCategory(authorCategory == null ? "UGC" : authorCategory);
        // 未发布状态，管理员审核后可设置 publishedAt
        a.setPublishedAt(null);
        return toDto(articleRepo.save(a));
    }

    public ContentArticleDto articleDetail(Long id) {
        return CacheSupport.getOptional(cacheManager, "content:article", "id=" + id,
                () -> articleRepo.findById(id).map(this::toDto))
                .orElseThrow(() -> new IllegalArgumentException("not found"));
    }

    @Cacheable(cacheNames = "content:exhibitors", key = "'p=' + #page + ':s=' + #size", sync = true)
    public PageResult<ExhibitorDto> listExhibitors(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size));
        Page<Exhibitor> p = exhibitorRepo.findAll(pageable);
        return PageResult.of(p.getContent().stream().map(this::toDto).toList(), p.getTotalElements(), p.getNumber(), p.getSize());
    }

    public ExhibitorDto exhibitorDetail(Long id) {
        return CacheSupport.getOptional(cacheManager, "content:exhibitor", "id=" + id,
                () -> exhibitorRepo.findById(id).map(this::toDto))
                .orElseThrow(() -> new IllegalArgumentException("not found"));
    }

    @Cacheable(cacheNames = "content:achievements", key = "'p=' + #page + ':s=' + #size", sync = true)
    public PageResult<AchievementDto> listAchievements(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size));
        Page<Achievement> p = achievementRepo.findAll(pageable);
        return PageResult.of(p.getContent().stream().map(this::toDto).toList(), p.getTotalElements(), p.getNumber(), p.getSize());
    }

    public AchievementDto achievementDetail(Long id) {
        return CacheSupport.getOptional(cacheManager, "content:achievement", "id=" + id,
                () -> achievementRepo.findById(id).map(this::toDto))
                .orElseThrow(() -> new IllegalArgumentException("not found"));
    }

    @Cacheable(cacheNames = "content:materials", key = "'p=' + #page + ':s=' + #size", sync = true)
    public PageResult<MaterialDto> listMaterials(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Material> p = materialRepo.findAll(pageable);
        return PageResult.of(p.getContent().stream().map(this::toDto).toList(), p.getTotalElements(), p.getNumber(), p.getSize());
    }

    @CacheEvict(cacheNames = "content:materials", allEntries = true)
    public MaterialDto createMaterial(String title, String speaker, String objectName, String contentType, long size) {
        if (title == null || title.isBlank()) { throw new IllegalArgumentException("标题不能为空"); }
        if (objectName == null || objectName.isBlank()) { throw new IllegalArgumentException("文件对象名不能为空"); }
        Material m = new Material();
        m.setTitle(title);
        m.setSpeaker(speaker);
        m.setObjectName(objectName);
        m.setContentType(contentType);
        m.setSize(size);
        m.setCreatedAt(Instant.now());
        return toDto(materialRepo.save(m));
    }

    public MaterialDto materialDetail(Long id) {
        return CacheSupport.getOptional(cacheManager, "content:material", "id=" + id,
                () -> materialRepo.findById(id).map(this::toDto))
                .orElseThrow(() -> new IllegalArgumentException("not found"));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "content:materials", allEntries = true),
            @CacheEvict(cacheNames = "content:material", key = "'id=' + #id")
    })
    public void deleteMaterial(Long id) {
        if (!materialRepo.existsById(id)) {
            throw new IllegalArgumentException("not found");
        }
        materialRepo.deleteById(id);
    }

    @Cacheable(cacheNames = "content:media",
            key = "'p=' + #page + ':s=' + #size + ':t=' + (#type == null ? '' : #type.trim())",
            sync = true)
    public PageResult<MediaAssetDto> listMedia(int page, int size, String type) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MediaAsset> p;
        if (type != null && !type.isBlank()) {
            p = mediaRepo.findByTypeIgnoreCase(type.trim(), pageable);
        } else {
            p = mediaRepo.findAll(pageable);
        }
        return PageResult.of(p.getContent().stream().map(this::toDto).toList(), p.getTotalElements(), p.getNumber(), p.getSize());
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "content:media", allEntries = true),
            @CacheEvict(cacheNames = "content:mediaByArticle", allEntries = true)
    })
    public MediaAssetDto createMedia(String title, String summary, String type, String objectName, String coverUrl) {
        if (title == null || title.isBlank()) { throw new IllegalArgumentException("标题不能为空"); }
        if (objectName == null || objectName.isBlank()) { throw new IllegalArgumentException("媒体对象名不能为空"); }
        MediaAsset a = new MediaAsset();
        a.setTitle(title);
        a.setSummary(summary);
        a.setType(type);
        a.setObjectName(objectName);
        a.setCoverUrl(coverUrl);
        a.setCreatedAt(Instant.now());
        return toDto(mediaRepo.save(a));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "content:media", allEntries = true),
            @CacheEvict(cacheNames = "content:mediaByArticle", key = "'articleId=' + #articleId")
    })
    public MediaAssetDto createMedia(String title, String summary, String type, String objectName, String coverUrl, Long articleId) {
        if (title == null || title.isBlank()) { throw new IllegalArgumentException("标题不能为空"); }
        if (objectName == null || objectName.isBlank()) { throw new IllegalArgumentException("媒体对象名不能为空"); }
        MediaAsset a = new MediaAsset();
        a.setTitle(title);
        a.setSummary(summary);
        a.setType(type);
        a.setObjectName(objectName);
        a.setCoverUrl(coverUrl);
        a.setArticleId(articleId);
        a.setCreatedAt(Instant.now());
        return toDto(mediaRepo.save(a));
    }

    @Cacheable(cacheNames = "content:mediaByArticle", key = "'articleId=' + #articleId", sync = true)
    public java.util.List<MediaAssetDto> getMediaByArticleId(Long articleId) {
        return mediaRepo.findByArticleId(articleId).stream().map(this::toDto).toList();
    }

    public MediaAssetDto mediaDetail(Long id) {
        return CacheSupport.getOptional(cacheManager, "content:mediaItem", "id=" + id,
                () -> mediaRepo.findById(id).map(this::toDto))
                .orElseThrow(() -> new IllegalArgumentException("not found"));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "content:media", allEntries = true),
            @CacheEvict(cacheNames = "content:mediaByArticle", allEntries = true),
            @CacheEvict(cacheNames = "content:mediaItem", key = "'id=' + #id")
    })
    public void deleteMedia(Long id) {
        if (!mediaRepo.existsById(id)) {
            throw new IllegalArgumentException("not found");
        }
        mediaRepo.deleteById(id);
    }

    private ContentArticleDto toDto(ContentArticle a) {
        ContentArticleDto dto = new ContentArticleDto();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setSummary(a.getSummary());
        dto.setBody(a.getBody());
        dto.setCoverUrl(a.getCoverUrl());
        dto.setCategory(a.getCategory());
        dto.setAuthor(a.getAuthor());
        dto.setViews(a.getViews());
        dto.setLiveUrl(a.getLiveUrl());
        dto.setReplayUrl(a.getReplayUrl());
        dto.setStatus(a.getStatus());
        dto.setPublishedAt(a.getPublishedAt());
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }

    private ExhibitorDto toDto(Exhibitor e) {
        ExhibitorDto dto = new ExhibitorDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setLogoUrl(e.getLogoUrl());
        dto.setIntroduction(e.getIntroduction());
        dto.setWebsite(e.getWebsite());
        dto.setBrand(e.getBrand());
        dto.setVideoUrl(e.getVideoUrl());
        return dto;
    }

    private AchievementDto toDto(Achievement a) {
        AchievementDto dto = new AchievementDto();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setDescription(a.getDescription());
        dto.setFileUrl(a.getFileUrl());
        return dto;
    }

    private MaterialDto toDto(Material m) {
        MaterialDto dto = new MaterialDto();
        dto.setId(m.getId());
        dto.setTitle(m.getTitle());
        dto.setSpeaker(m.getSpeaker());
        dto.setObjectName(m.getObjectName());
        dto.setContentType(m.getContentType());
        dto.setSize(m.getSize());
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }

    private MediaAssetDto toDto(MediaAsset a) {
        MediaAssetDto dto = new MediaAssetDto();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setSummary(a.getSummary());
        dto.setType(a.getType());
        dto.setObjectName(a.getObjectName());
        dto.setCoverUrl(a.getCoverUrl());
        dto.setArticleId(a.getArticleId());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }
}
