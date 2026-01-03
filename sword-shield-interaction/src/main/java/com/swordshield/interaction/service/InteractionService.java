package com.swordshield.interaction.service;

import com.swordshield.common.PageResult;
import com.swordshield.interaction.domain.Comment;
import com.swordshield.interaction.domain.Like;
import com.swordshield.interaction.domain.Share;
import com.swordshield.interaction.dto.CommentDto;
import com.swordshield.interaction.repository.CommentRepository;
import com.swordshield.interaction.repository.LikeRepository;
import com.swordshield.interaction.repository.ShareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

/**
 * 互动服务核心应用服务。
 *
 * <p>提供评论列表/发布、点赞/取消点赞、分享记录等能力，目标对象通过 {@code targetType}/{@code targetId} 进行抽象化关联。
 */
@Service
public class InteractionService {
    private final CommentRepository commentRepo;
    private final LikeRepository likeRepo;
    private final ShareRepository shareRepo;

    public InteractionService(CommentRepository commentRepo, LikeRepository likeRepo, ShareRepository shareRepo) {
        this.commentRepo = commentRepo;
        this.likeRepo = likeRepo;
        this.shareRepo = shareRepo;
    }

    public PageResult<CommentDto> listComments(String targetType, Long targetId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size));
        Page<Comment> p = commentRepo.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId, pageable);
        return PageResult.of(p.getContent().stream().map(this::toDto).toList(), p.getTotalElements(), p.getNumber(), p.getSize());
    }

    public void postComment(Long userId, String targetType, Long targetId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        Comment c = new Comment();
        c.setUserId(userId);
        c.setTargetType(targetType);
        c.setTargetId(targetId);
        c.setContent(content);
        c.setCreatedAt(Instant.now());
        commentRepo.save(c);
    }

    @Transactional
    public synchronized long like(Long userId, String targetType, Long targetId) {
        // Double-check to prevent duplicate likes
        if (likeRepo.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId).isPresent()) {
            throw new IllegalArgumentException("您已点赞，无需重复点赞");
        }
        
        Like l = new Like();
        l.setUserId(userId);
        l.setTargetType(targetType);
        l.setTargetId(targetId);
        l.setCreatedAt(Instant.now());
        
        try {
            likeRepo.save(l);
        } catch (Exception e) {
            // Handle unique constraint violation
            throw new IllegalArgumentException("您已点赞，无需重复点赞");
        }
        
        return likeRepo.countByTargetTypeAndTargetId(targetType, targetId);
    }

    @Transactional
    public long unlike(Long userId, String targetType, Long targetId) {
        Like l = likeRepo.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
            .orElseThrow(() -> new IllegalArgumentException("您还未点赞，无法取消点赞"));
        likeRepo.delete(l);
        return likeRepo.countByTargetTypeAndTargetId(targetType, targetId);
    }

    public void share(Long userId, String targetType, Long targetId, String channel) {
        Share s = new Share();
        s.setUserId(userId);
        s.setTargetType(targetType);
        s.setTargetId(targetId);
        s.setChannel(channel == null ? "link" : channel);
        s.setCreatedAt(Instant.now());
        shareRepo.save(s);
    }

    public long countLikes(String targetType, Long targetId) {
        return likeRepo.countByTargetTypeAndTargetId(targetType, targetId);
    }

    private CommentDto toDto(Comment c) {
        CommentDto dto = new CommentDto();
        dto.setId(c.getId());
        dto.setUserId(c.getUserId());
        dto.setTargetType(c.getTargetType());
        dto.setTargetId(c.getTargetId());
        dto.setContent(c.getContent());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }
}
