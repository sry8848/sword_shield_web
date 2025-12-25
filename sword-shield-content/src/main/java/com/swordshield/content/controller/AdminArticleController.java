package com.swordshield.content.controller;

import com.swordshield.common.JwtUtil;
import com.swordshield.common.Response;
import com.swordshield.content.domain.ContentArticle;
import com.swordshield.content.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/content/admin/articles")
@Tag(name = "资讯管理（管理员）", description = "管理员对资讯文章的增删改查")
public class AdminArticleController {
    private final ContentService service;

    public AdminArticleController(ContentService service) {
        this.service = service;
    }

    private String verifyAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new SecurityException("未登录，请先登录");
        }
        String token = authorization.substring(7);
        String subject = JwtUtil.parseSubject(token);
        if (!subject.startsWith("admin_")) {
            throw new SecurityException("无权限访问，需要管理员权限");
        }
        return subject;
    }

    @GetMapping
    @Operation(summary = "资讯列表（管理员）", description = "分页查询全部资讯（包含草稿/已发布），支持按分类与状态过滤")
    public Response<Map<String, Object>> list(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "excludeCategory", required = false) String excludeCategory) {
        verifyAdmin(authorization);
        Page<ContentArticle> p = service.listAdminArticles(page, size, category, status, excludeCategory);
        return Response.ok(Map.of(
                "items", p.getContent(),
                "total", p.getTotalElements(),
                "page", p.getNumber(),
                "size", p.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "资讯详情（管理员）", description = "根据 ID 查询资讯（包含草稿/已发布）")
    public Response<ContentArticle> detail(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") Long id) {
        verifyAdmin(authorization);
        return Response.ok(service.articleDetail(id));
    }

    @PostMapping
    @Operation(summary = "创建资讯", description = "管理员创建资讯，可选择草稿/发布")
    public Response<ContentArticle> create(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @RequestBody ContentArticle body) {
        String subject = verifyAdmin(authorization);
        return Response.ok(service.createAdminArticle(body, subject));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新资讯", description = "管理员更新资讯内容与发布状态")
    public Response<ContentArticle> update(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") Long id,
            @RequestBody ContentArticle body) {
        verifyAdmin(authorization);
        return Response.ok(service.updateAdminArticle(id, body));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除资讯", description = "管理员删除资讯")
    public Response<Object> delete(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") Long id) {
        verifyAdmin(authorization);
        service.deleteAdminArticle(id);
        return Response.ok(null);
    }
}
