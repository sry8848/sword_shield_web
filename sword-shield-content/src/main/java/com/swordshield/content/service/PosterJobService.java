package com.swordshield.content.service;

import com.swordshield.common.ForbiddenException;
import com.swordshield.content.domain.PosterJob;
import com.swordshield.content.domain.PosterJobStatus;
import com.swordshield.content.dto.PosterJobCreateResponse;
import com.swordshield.content.dto.PosterJobStatusResponse;
import com.swordshield.content.repository.PosterJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
public class PosterJobService {
    private static final Logger log = LoggerFactory.getLogger(PosterJobService.class);

    private final PosterJobRepository posterJobRepository;
    private final PosterJobDispatchRetryService dispatchRetryService;

    public PosterJobService(PosterJobRepository posterJobRepository,
                            PosterJobDispatchRetryService dispatchRetryService) {
        this.posterJobRepository = posterJobRepository;
        this.dispatchRetryService = dispatchRetryService;
    }

    public PosterJobCreateResponse createJob(String subject, String title, String subtitle) {
        if (!StringUtils.hasText(subject)) {
            throw new SecurityException("未登录，请先登录");
        }
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("title 不能为空");
        }
        if (!StringUtils.hasText(subtitle)) {
            throw new IllegalArgumentException("subtitle 不能为空");
        }

        Instant now = Instant.now();
        String jobId = UUID.randomUUID().toString().replace("-", "");

        PosterJob job = new PosterJob();
        job.setId(jobId);
        job.setSubject(subject.trim());
        job.setTitle(title.trim());
        job.setSubtitle(subtitle.trim());
        job.setStatus(PosterJobStatus.PENDING);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        posterJobRepository.save(job);
        try {
            dispatchRetryService.dispatchNow(jobId);
        } catch (Exception e) {
            log.warn("Poster job dispatch service failed: {}", jobId, e);
        }
        return new PosterJobCreateResponse(jobId);
    }

    public PosterJobStatusResponse getJobStatus(String subject, String jobId) {
        if (!StringUtils.hasText(subject)) {
            throw new SecurityException("未登录，请先登录");
        }
        if (!StringUtils.hasText(jobId)) {
            throw new IllegalArgumentException("jobId 不能为空");
        }

        PosterJob job = posterJobRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("not found"));
        if (!subject.trim().equals(job.getSubject())) {
            throw new ForbiddenException("无权限访问该任务");
        }

        PosterJobStatusResponse res = new PosterJobStatusResponse();
        res.setJobId(job.getId());
        res.setStatus(job.getStatus());
        res.setUrl(job.getResultUrl());
        res.setErrorMessage(job.getErrorMessage());
        res.setDispatchAttempts(job.getDispatchAttempts());
        res.setDispatchLastError(job.getDispatchLastError());
        res.setDispatchLastAttemptAt(job.getDispatchLastAttemptAt());
        res.setDispatchNextRetryAt(job.getDispatchNextRetryAt());
        return res;
    }
}
