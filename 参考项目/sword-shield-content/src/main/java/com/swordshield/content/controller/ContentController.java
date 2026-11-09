package com.swordshield.content.controller;

import com.swordshield.common.Response;
import com.swordshield.content.domain.ContentArticle;
import com.swordshield.content.domain.Exhibitor;
import com.swordshield.content.domain.Achievement;
import com.swordshield.content.domain.Material;
import com.swordshield.content.domain.MediaAsset;
import com.swordshield.content.service.ContentService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/content")
public class ContentController {
    private final ContentService service;

    public ContentController(ContentService service) { this.service = service; }

    @GetMapping("/articles")
    public Response<Map<String, Object>> articles(@RequestParam(name = "page", defaultValue = "0") int page,
                                                  @RequestParam(name = "size", defaultValue = "10") int size,
                                                  @RequestParam(name = "category", required = false) String category,
                                                  @RequestParam(name = "tags", required = false) String tags) {
        Page<ContentArticle> p = service.listArticles(page, size, category, tags);
        return Response.ok(Map.of("items", p.getContent(), "total", p.getTotalElements()));
    }

    @GetMapping("/article")
    public Response<ContentArticle> article(@RequestParam("id") Long id) { return Response.ok(service.articleDetail(id)); }

    @GetMapping("/exhibitors")
    public Response<Map<String, Object>> exhibitors(@RequestParam(name = "page", defaultValue = "0") int page,
                                                    @RequestParam(name = "size", defaultValue = "10") int size) {
        Page<Exhibitor> p = service.listExhibitors(page, size);
        return Response.ok(Map.of("items", p.getContent(), "total", p.getTotalElements()));
    }

    @GetMapping("/exhibitor")
    public Response<Exhibitor> exhibitor(@RequestParam("id") Long id) { return Response.ok(service.exhibitorDetail(id)); }

    @GetMapping("/achievements")
    public Response<Map<String, Object>> achievements(@RequestParam(name = "page", defaultValue = "0") int page,
                                                      @RequestParam(name = "size", defaultValue = "10") int size) {
        Page<Achievement> p = service.listAchievements(page, size);
        return Response.ok(Map.of("items", p.getContent(), "total", p.getTotalElements()));
    }

    @GetMapping("/achievement")
    public Response<Achievement> achievement(@RequestParam("id") Long id) { return Response.ok(service.achievementDetail(id)); }

    @PostMapping("/article/submit")
    public Response<ContentArticle> submit(@RequestBody Map<String, String> body) {
        ContentArticle saved = service.submitArticle(
                body.get("title"),
                body.get("summary"),
                body.get("body"),
                body.get("coverUrl"),
                body.getOrDefault("category", "UGC")
        );
        return Response.ok(saved);
    }

    @GetMapping("/materials")
    public Response<Map<String, Object>> materials(@RequestParam(name = "page", defaultValue = "0") int page,
                                                   @RequestParam(name = "size", defaultValue = "10") int size) {
        org.springframework.data.domain.Page<Material> p = service.listMaterials(page, size);
        return Response.ok(Map.of("items", p.getContent(), "total", p.getTotalElements()));
    }

    @GetMapping("/material")
    public Response<Material> material(@RequestParam("id") Long id) { return Response.ok(service.materialDetail(id)); }

    @PostMapping("/material")
    public Response<Material> createMaterial(@RequestBody Map<String, Object> body) {
        try {
            String title = (String) body.get("title");
            String speaker = (String) body.get("speaker");
            String objectName = (String) body.get("objectName");
            String contentType = (String) body.get("contentType");
            long size = body.get("size") == null ? 0L : ((Number) body.get("size")).longValue();
            Material saved = service.createMaterial(title, speaker, objectName, contentType, size);
            return Response.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(500, "保存失败: " + e.getMessage());
        }
    }

    @GetMapping("/media")
    public Response<Map<String, Object>> mediaList(@RequestParam(name = "page", defaultValue = "0") int page,
                                                   @RequestParam(name = "size", defaultValue = "10") int size,
                                                   @RequestParam(name = "type", required = false) String type) {
        org.springframework.data.domain.Page<MediaAsset> p = service.listMedia(page, size, type);
        return Response.ok(Map.of("items", p.getContent(), "total", p.getTotalElements()));
    }

    @GetMapping("/media/item")
    public Response<MediaAsset> media(@RequestParam("id") Long id) { return Response.ok(service.mediaDetail(id)); }

    @PostMapping("/media")
    public Response<MediaAsset> createMedia(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String summary = (String) body.get("summary");
        String type = (String) body.get("type");
        String objectName = (String) body.get("objectName");
        String coverUrl = (String) body.get("coverUrl");
        Long articleId = body.get("articleId") != null ? ((Number) body.get("articleId")).longValue() : null;
        
        if (articleId != null) {
            return Response.ok(service.createMedia(title, summary, type, objectName, coverUrl, articleId));
        }
        return Response.ok(service.createMedia(title, summary, type, objectName, coverUrl));
    }
    
    @GetMapping("/media/by-article")
    public Response<java.util.List<MediaAsset>> getMediaByArticle(@RequestParam("articleId") Long articleId) {
        return Response.ok(service.getMediaByArticleId(articleId));
    }
}
