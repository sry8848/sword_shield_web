package com.swordshield.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 海报生成任务（Domain Entity）。
 *
 * <p>用于异步生成海报图片的任务记录：
 * <ul>
 *   <li>{@code subject}：任务归属主体（通常为用户 subject，用于鉴权/隔离）。</li>
 *   <li>{@code status}/{@code resultUrl}/{@code errorMessage}：任务执行结果。</li>
 *   <li>投递重试相关字段用于记录 MQ 投递次数与下次重试时间。</li>
 * </ul>
 */
@Entity
@Table(name = "poster_job")
public class PosterJob {
    /** 海报任务 ID（业务生成的字符串主键）。 */
    @Id
    @Column(length = 64)
    private String id;

    /** 任务归属主体（用于鉴权隔离，通常是用户 subject）。 */
    @Column(name = "subject", nullable = false, length = 64)
    private String subject;

    /** 海报标题。 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 海报副标题。 */
    @Column(name = "subtitle", nullable = false, length = 200)
    private String subtitle;

    /** 任务状态。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PosterJobStatus status;

    /** 结果图片 URL（成功时写入，通常为预签名 URL）。 */
    @Column(name = "result_url", length = 2048)
    private String resultUrl;

    /** 错误信息（失败时写入）。 */
    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    /** 投递 MQ 的尝试次数（用于重试控制）。 */
    @Column(name = "dispatch_attempts", nullable = false)
    private int dispatchAttempts;

    /** 最近一次投递错误信息（投递中可能为 {@code DISPATCHING}）。 */
    @Column(name = "dispatch_last_error", length = 1024)
    private String dispatchLastError;

    /** 最近一次投递尝试时间。 */
    @Column(name = "dispatch_last_attempt_at")
    private Instant dispatchLastAttemptAt;

    /** 下次允许投递的时间（用于锁/重试调度）。 */
    @Column(name = "dispatch_next_retry_at")
    private Instant dispatchNextRetryAt;

    /** 创建时间。 */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** 更新时间。 */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public PosterJobStatus getStatus() { return status; }
    public void setStatus(PosterJobStatus status) { this.status = status; }
    public String getResultUrl() { return resultUrl; }
    public void setResultUrl(String resultUrl) { this.resultUrl = resultUrl; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getDispatchAttempts() { return dispatchAttempts; }
    public void setDispatchAttempts(int dispatchAttempts) { this.dispatchAttempts = dispatchAttempts; }
    public String getDispatchLastError() { return dispatchLastError; }
    public void setDispatchLastError(String dispatchLastError) { this.dispatchLastError = dispatchLastError; }
    public Instant getDispatchLastAttemptAt() { return dispatchLastAttemptAt; }
    public void setDispatchLastAttemptAt(Instant dispatchLastAttemptAt) { this.dispatchLastAttemptAt = dispatchLastAttemptAt; }
    public Instant getDispatchNextRetryAt() { return dispatchNextRetryAt; }
    public void setDispatchNextRetryAt(Instant dispatchNextRetryAt) { this.dispatchNextRetryAt = dispatchNextRetryAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
