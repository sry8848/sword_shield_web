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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/content")
@Tag(name = "内容", description = "文章、展商、成就、素材与媒体接口")
public class ContentController {
    private final ContentService service;

    public ContentController(ContentService service) { this.service = service; }

    @GetMapping("/articles")
    @Operation(summary = "文章列表", description = "按分页、分类、标签筛选文章")
    public Response<Map<String, Object>> articles(@RequestParam(name = "page", defaultValue = "0") int page,
                                                  @RequestParam(name = "size", defaultValue = "10") int size,
                                                  @RequestParam(name = "category", required = false) String category,
                                                  @RequestParam(name = "tags", required = false) String tags) {
        Page<ContentArticle> p = service.listArticles(page, size, category, tags);
        return Response.ok(Map.of("items", p.getContent(), "total", p.getTotalElements()));
    }

    @GetMapping("/article")
    @Operation(summary = "文章详情", description = "根据 ID 获取文章内容")
    public Response<ContentArticle> article(@RequestParam("id") Long id) { return Response.ok(service.articleDetail(id)); }

    @GetMapping("/exhibitors")
    @Operation(summary = "展商列表", description = "分页查询参展企业/机构")
    public Response<Map<String, Object>> exhibitors(@RequestParam(name = "page", defaultValue = "0") int page,
                                                    @RequestParam(name = "size", defaultValue = "10") int size) {
        Page<Exhibitor> p = service.listExhibitors(page, size);
        return Response.ok(Map.of("items", p.getContent(), "total", p.getTotalElements()));
    }

    @GetMapping("/exhibitor")
    @Operation(summary = "展商详情", description = "根据 ID 获取展商信息")
    public Response<Exhibitor> exhibitor(@RequestParam("id") Long id) { return Response.ok(service.exhibitorDetail(id)); }

    @GetMapping("/achievements")
    @Operation(summary = "成就列表", description = "分页查询成果/案例")
    public Response<Map<String, Object>> achievements(@RequestParam(name = "page", defaultValue = "0") int page,
                                                      @RequestParam(name = "size", defaultValue = "10") int size) {
        Page<Achievement> p = service.listAchievements(page, size);
        return Response.ok(Map.of("items", p.getContent(), "total", p.getTotalElements()));
    }

    @GetMapping("/achievement")
    @Operation(summary = "成就详情", description = "根据 ID 获取成就内容")
    public Response<Achievement> achievement(@RequestParam("id") Long id) { return Response.ok(service.achievementDetail(id)); }

    @PostMapping("/article/submit")
    @Operation(summary = "提交文章", description = "UGC 投稿入口，提交标题、摘要、正文等信息")
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
    @Operation(summary = "资料列表", description = "分页查询会议资料、幻灯、白皮书等")
    public Response<Map<String, Object>> materials(@RequestParam(name = "page", defaultValue = "0") int page,
                                                   @RequestParam(name = "size", defaultValue = "10") int size) {
        org.springframework.data.domain.Page<Material> p = service.listMaterials(page, size);
        return Response.ok(Map.of("items", p.getContent(), "total", p.getTotalElements()));
    }

    @GetMapping("/material")
    @Operation(summary = "资料详情", description = "根据 ID 获取资料元信息")
    public Response<Material> material(@RequestParam("id") Long id) { return Response.ok(service.materialDetail(id)); }

    @PostMapping("/material")
    @Operation(summary = "新建资料", description = "创建资料元数据，支持上传后的对象信息对接")
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
    @Operation(summary = "媒体素材列表", description = "分页查询视频/音频/图片等媒体资产，可按类型过滤")
    public Response<Map<String, Object>> mediaList(@RequestParam(name = "page", defaultValue = "0") int page,
                                                   @RequestParam(name = "size", defaultValue = "10") int size,
                                                   @RequestParam(name = "type", required = false) String type) {
        org.springframework.data.domain.Page<MediaAsset> p = service.listMedia(page, size, type);
        return Response.ok(Map.of("items", p.getContent(), "total", p.getTotalElements()));
    }

    @GetMapping("/media/item")
    @Operation(summary = "媒体详情", description = "根据 ID 获取单个媒体资产信息")
    public Response<MediaAsset> media(@RequestParam("id") Long id) { return Response.ok(service.mediaDetail(id)); }

    @PostMapping("/media")
    @Operation(summary = "创建媒体素材", description = "上传后的媒体元数据登记，可关联文章 ID")
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
    @Operation(summary = "按文章查询媒体", description = "通过文章 ID 获取关联媒体列表")
    public Response<java.util.List<MediaAsset>> getMediaByArticle(@RequestParam("articleId") Long articleId) {
        return Response.ok(service.getMediaByArticleId(articleId));
    }
}
