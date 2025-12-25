package com.swordshield.interaction.controller;

import com.swordshield.common.AuthUtils;
import com.swordshield.common.PageResult;
import com.swordshield.common.Response;
import com.swordshield.interaction.dto.CommentDto;
import com.swordshield.interaction.service.InteractionService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/interaction")
@Tag(name = "互动", description = "评论、点赞、分享等互动行为")
public class InteractionController {
    private final InteractionService service;
    public InteractionController(InteractionService service) { this.service = service; }

    @GetMapping("/comments")
    @Operation(summary = "评论列表", description = "按目标类型和 ID 分页返回评论")
    public Response<Map<String, Object>> comments(@RequestParam("targetType") String targetType,
                                                  @RequestParam("targetId") Long targetId,
                                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                                  @RequestParam(name = "size", defaultValue = "10") int size) {
        PageResult<CommentDto> p = service.listComments(targetType, targetId, page, size);
        return Response.ok(Map.of("items", p.getItems(), "total", p.getTotal()));
    }

    @PostMapping("/comment")
    @Operation(summary = "发表评论", description = "需携带用户 JWT")
    public Response<Object> post(
                                 @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
                                 @RequestHeader(name = "Authorization", required = false) String authorization,
                                 @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
                                 @RequestBody Map<String, Object> body) {
        Long userId = AuthUtils.requireUserId(userIdHeader, authorization);
        String targetType = (String) body.get("targetType");
        Long targetId = ((Number) body.get("targetId")).longValue();
        String content = (String) body.get("content");
        service.postComment(userId, targetType, targetId, content);
        return Response.ok(null);
    }

    @PostMapping("/like")
    @Operation(summary = "点赞", description = "需携带用户 JWT，返回最新点赞数")
    public Response<Map<String, Long>> like(
                                            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
                                            @RequestHeader(name = "Authorization", required = false) String authorization,
                                            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
                                            @RequestBody Map<String, Object> body) {
        Long userId = AuthUtils.requireUserId(userIdHeader, authorization);
        String targetType = (String) body.get("targetType");
        Long targetId = ((Number) body.get("targetId")).longValue();
        long count = service.like(userId, targetType, targetId);
        return Response.ok(Map.of("count", count));
    }

    @PostMapping("/unlike")
    @Operation(summary = "取消点赞", description = "需携带用户 JWT，返回最新点赞数")
    public Response<Map<String, Long>> unlike(
                                               @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
                                               @RequestHeader(name = "Authorization", required = false) String authorization,
                                               @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
                                              @RequestBody Map<String, Object> body) {
        Long userId = AuthUtils.requireUserId(userIdHeader, authorization);
        String targetType = (String) body.get("targetType");
        Long targetId = ((Number) body.get("targetId")).longValue();
        long count = service.unlike(userId, targetType, targetId);
        return Response.ok(Map.of("count", count));
    }

    @PostMapping("/share")
    @Operation(summary = "分享", description = "记录用户分享行为，需携带 JWT")
    public Response<Object> share(
                                  @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
                                  @RequestHeader(name = "Authorization", required = false) String authorization,
                                  @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
                                  @RequestBody Map<String, Object> body) {
        Long userId = AuthUtils.requireUserId(userIdHeader, authorization);
        String targetType = (String) body.get("targetType");
        Long targetId = ((Number) body.get("targetId")).longValue();
        String channel = (String) body.getOrDefault("channel", "link");
        service.share(userId, targetType, targetId, channel);
        return Response.ok(null);
    }

    @GetMapping("/likes/count")
    @Operation(summary = "点赞数查询", description = "根据目标类型和 ID 查询点赞总数")
    public Response<Map<String, Long>> likeCount(@RequestParam("targetType") String targetType,
                                                 @RequestParam("targetId") Long targetId) {
        long count = service.countLikes(targetType, targetId);
        return Response.ok(Map.of("count", count));
    }
}

