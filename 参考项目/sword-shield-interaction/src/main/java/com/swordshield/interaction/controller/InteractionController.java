package com.swordshield.interaction.controller;

import com.swordshield.common.JwtUtil;
import com.swordshield.common.Response;
import com.swordshield.interaction.domain.Comment;
import com.swordshield.interaction.service.InteractionService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/interaction")
public class InteractionController {
    private final InteractionService service;
    public InteractionController(InteractionService service) { this.service = service; }

    @GetMapping("/comments")
    public Response<Map<String, Object>> comments(@RequestParam("targetType") String targetType,
                                                  @RequestParam("targetId") Long targetId,
                                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                                  @RequestParam(name = "size", defaultValue = "10") int size) {
        Page<Comment> p = service.listComments(targetType, targetId, page, size);
        return Response.ok(Map.of("items", p.getContent(), "total", p.getTotalElements()));
    }

    @PostMapping("/comment")
    public Response<Object> post(@RequestHeader("Authorization") String authorization,
                                 @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(JwtUtil.parseSubject(authorization.substring(7)));
        String targetType = (String) body.get("targetType");
        Long targetId = ((Number) body.get("targetId")).longValue();
        String content = (String) body.get("content");
        service.postComment(userId, targetType, targetId, content);
        return Response.ok(null);
    }

    @PostMapping("/like")
    public Response<Map<String, Long>> like(@RequestHeader("Authorization") String authorization,
                                            @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(JwtUtil.parseSubject(authorization.substring(7)));
        String targetType = (String) body.get("targetType");
        Long targetId = ((Number) body.get("targetId")).longValue();
        long count = service.like(userId, targetType, targetId);
        return Response.ok(Map.of("count", count));
    }

    @PostMapping("/unlike")
    public Response<Map<String, Long>> unlike(@RequestHeader("Authorization") String authorization,
                                              @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(JwtUtil.parseSubject(authorization.substring(7)));
        String targetType = (String) body.get("targetType");
        Long targetId = ((Number) body.get("targetId")).longValue();
        long count = service.unlike(userId, targetType, targetId);
        return Response.ok(Map.of("count", count));
    }

    @PostMapping("/share")
    public Response<Object> share(@RequestHeader("Authorization") String authorization,
                                  @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(JwtUtil.parseSubject(authorization.substring(7)));
        String targetType = (String) body.get("targetType");
        Long targetId = ((Number) body.get("targetId")).longValue();
        String channel = (String) body.getOrDefault("channel", "link");
        service.share(userId, targetType, targetId, channel);
        return Response.ok(null);
    }

    @GetMapping("/likes/count")
    public Response<Map<String, Long>> likeCount(@RequestParam("targetType") String targetType,
                                                 @RequestParam("targetId") Long targetId) {
        long count = service.countLikes(targetType, targetId);
        return Response.ok(Map.of("count", count));
    }
}

