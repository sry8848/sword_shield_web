package com.swordshield.content.dto;

/**
 * 海报任务创建响应 DTO。
 *
 * <p>创建海报生成任务后返回的任务 ID，客户端可用该 ID 轮询任务状态。
 */
public class PosterJobCreateResponse {
    /** 海报任务 ID，用于后续查询任务状态。 */
    private String jobId;

    public PosterJobCreateResponse() {}

    public PosterJobCreateResponse(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
}
