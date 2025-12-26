package com.swordshield.content.dto;

import com.swordshield.content.domain.PosterJobStatus;

import java.time.Instant;

public class PosterJobStatusResponse {
    private String jobId;
    private PosterJobStatus status;
    private String url;
    private String errorMessage;
    private int dispatchAttempts;
    private String dispatchLastError;
    private Instant dispatchLastAttemptAt;
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
