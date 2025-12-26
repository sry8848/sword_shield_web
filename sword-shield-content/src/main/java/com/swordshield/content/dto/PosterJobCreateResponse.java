package com.swordshield.content.dto;

public class PosterJobCreateResponse {
    private String jobId;

    public PosterJobCreateResponse() {}

    public PosterJobCreateResponse(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
}

