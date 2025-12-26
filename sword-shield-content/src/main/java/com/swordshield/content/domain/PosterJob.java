package com.swordshield.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "poster_job")
public class PosterJob {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "subject", nullable = false, length = 64)
    private String subject;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "subtitle", nullable = false, length = 200)
    private String subtitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PosterJobStatus status;

    @Column(name = "result_url", length = 2048)
    private String resultUrl;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "dispatch_attempts", nullable = false)
    private int dispatchAttempts;

    @Column(name = "dispatch_last_error", length = 1024)
    private String dispatchLastError;

    @Column(name = "dispatch_last_attempt_at")
    private Instant dispatchLastAttemptAt;

    @Column(name = "dispatch_next_retry_at")
    private Instant dispatchNextRetryAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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
