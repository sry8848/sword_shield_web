package com.swordshield.content.service;

import com.swordshield.content.domain.Achievement;
import com.swordshield.content.domain.ContentArticle;
import com.swordshield.content.domain.Exhibitor;
import com.swordshield.content.domain.Material;
import com.swordshield.content.domain.MediaAsset;
import com.swordshield.content.repository.AchievementRepository;
import com.swordshield.content.repository.ContentArticleRepository;
import com.swordshield.content.repository.ExhibitorRepository;
import com.swordshield.content.repository.MaterialRepository;
import com.swordshield.content.repository.MediaAssetRepository;
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

    public ContentService(ContentArticleRepository articleRepo, ExhibitorRepository exhibitorRepo, AchievementRepository achievementRepo, MaterialRepository materialRepo, MediaAssetRepository mediaRepo) {
        this.articleRepo = articleRepo;
        this.exhibitorRepo = exhibitorRepo;
        this.achievementRepo = achievementRepo;
        this.materialRepo = materialRepo;
        this.mediaRepo = mediaRepo;
    }

    public Page<ContentArticle> listArticles(int page, int size, String category, String tags) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size), Sort.by(Sort.Direction.DESC, "publishedAt"));
        if (category != null && !category.isBlank()) {
            return articleRepo.findPublishedArticlesByCategory(category, pageable);
        }
        // Only return published articles (status = 1 and publishedAt is not null)
        return articleRepo.findPublishedArticles(pageable);
    }

    public Page<ContentArticle> listAdminArticles(int page, int size, String category, Integer status, String excludeCategory) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size), Sort.by(Sort.Direction.DESC, "id"));

        boolean hasCategory = category != null && !category.isBlank();
        boolean hasStatus = status != null;
        boolean hasExcludeCategory = excludeCategory != null && !excludeCategory.isBlank();

        if (hasCategory && hasStatus) {
            return articleRepo.findByStatusAndCategory(status, category, pageable);
        }
        if (hasStatus) {
            if (hasExcludeCategory) {
                return articleRepo.findByStatusAndCategoryNot(status, excludeCategory, pageable);
            }
            return articleRepo.findByStatus(status, pageable);
        }
        if (hasCategory) {
            return articleRepo.findByCategory(category, pageable);
        }
        if (hasExcludeCategory) {
            return articleRepo.findByCategoryNot(excludeCategory, pageable);
        }
        return articleRepo.findAll(pageable);
    }

    public ContentArticle createAdminArticle(ContentArticle input, String adminSubject) {
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

        return articleRepo.save(a);
    }

    public ContentArticle updateAdminArticle(Long id, ContentArticle input) {
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

        return articleRepo.save(existing);
    }

    public void deleteAdminArticle(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        if (!articleRepo.existsById(id)) {
            throw new IllegalArgumentException("not found");
        }
        articleRepo.deleteById(id);
    }

    public ContentArticle submitArticle(String title, String summary, String body, String coverUrl, String authorCategory) {
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
        return articleRepo.save(a);
    }

    public ContentArticle articleDetail(Long id) {
        return articleRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
    }

    public Page<Exhibitor> listExhibitors(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size));
        return exhibitorRepo.findAll(pageable);
    }

    public Exhibitor exhibitorDetail(Long id) {
        return exhibitorRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
    }

    public Page<Achievement> listAchievements(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size));
        return achievementRepo.findAll(pageable);
    }

    public Achievement achievementDetail(Long id) {
        return achievementRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
    }

    public Page<Material> listMaterials(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return materialRepo.findAll(pageable);
    }

    public Material createMaterial(String title, String speaker, String objectName, String contentType, long size) {
        if (title == null || title.isBlank()) { throw new IllegalArgumentException("标题不能为空"); }
        if (objectName == null || objectName.isBlank()) { throw new IllegalArgumentException("文件对象名不能为空"); }
        Material m = new Material();
        m.setTitle(title);
        m.setSpeaker(speaker);
        m.setObjectName(objectName);
        m.setContentType(contentType);
        m.setSize(size);
        m.setCreatedAt(Instant.now());
        return materialRepo.save(m);
    }

    public Material materialDetail(Long id) {
        return materialRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
    }

    public void deleteMaterial(Long id) {
        if (!materialRepo.existsById(id)) {
            throw new IllegalArgumentException("not found");
        }
        materialRepo.deleteById(id);
    }

    public Page<MediaAsset> listMedia(int page, int size, String type) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        if (type != null && !type.isBlank()) {
            return mediaRepo.findByTypeIgnoreCase(type.trim(), pageable);
        }
        return mediaRepo.findAll(pageable);
    }

    public MediaAsset createMedia(String title, String summary, String type, String objectName, String coverUrl) {
        if (title == null || title.isBlank()) { throw new IllegalArgumentException("标题不能为空"); }
        if (objectName == null || objectName.isBlank()) { throw new IllegalArgumentException("媒体对象名不能为空"); }
        MediaAsset a = new MediaAsset();
        a.setTitle(title);
        a.setSummary(summary);
        a.setType(type);
        a.setObjectName(objectName);
        a.setCoverUrl(coverUrl);
        a.setCreatedAt(Instant.now());
        return mediaRepo.save(a);
    }

    public MediaAsset createMedia(String title, String summary, String type, String objectName, String coverUrl, Long articleId) {
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
        return mediaRepo.save(a);
    }

    public java.util.List<MediaAsset> getMediaByArticleId(Long articleId) {
        return mediaRepo.findByArticleId(articleId);
    }

    public MediaAsset mediaDetail(Long id) {
        return mediaRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
    }

    public void deleteMedia(Long id) {
        if (!mediaRepo.existsById(id)) {
            throw new IllegalArgumentException("not found");
        }
        mediaRepo.deleteById(id);
    }
}
