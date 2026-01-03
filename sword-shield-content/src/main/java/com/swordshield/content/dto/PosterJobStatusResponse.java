package com.swordshield.content.dto;

import com.swordshield.content.domain.PosterJobStatus;

import java.time.Instant;

/**
 * 海报任务状态响应 DTO。
 *
 * <p>用于查询海报生成任务的当前状态、结果 URL（成功时）以及失败/投递重试信息等。
 */
public class PosterJobStatusResponse {
    /** 海报任务 ID。 */
    private String jobId;

    /** 任务状态。 */
    private PosterJobStatus status;

    /** 结果图片 URL（成功时返回；可能为预签名 URL）。 */
    private String url;

    /** 错误信息（失败时返回）。 */
    private String errorMessage;

    /** 投递 MQ 的尝试次数。 */
    private int dispatchAttempts;

    /** 最近一次投递错误信息。 */
    private String dispatchLastError;

    /** 最近一次投递尝试时间。 */
    private Instant dispatchLastAttemptAt;

    /** 下次允许投递时间（用于重试调度）。 */
    private Instant dispatchNextRetryAt;

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public PosterJobStatus getStatus() { return status; }
    public void setStatus(PosterJobStatus status) { this.status = status; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
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
}
