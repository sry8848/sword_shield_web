package com.swordshield.content.service;

import com.swordshield.common.JwtUtil;
import com.swordshield.content.domain.PosterJob;
import com.swordshield.content.domain.PosterJobStatus;
import com.swordshield.content.repository.PosterJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class PosterJobProcessor {
    private static final Logger log = LoggerFactory.getLogger(PosterJobProcessor.class);

    private final PosterJobRepository posterJobRepository;
    private final PosterRenderService posterRenderService;
    private final FileServiceClient fileServiceClient;
    private final int presignExpireSeconds;

    public PosterJobProcessor(PosterJobRepository posterJobRepository,
                              PosterRenderService posterRenderService,
                              FileServiceClient fileServiceClient,
                              @Value("${app.poster.presign-expire-seconds:600}") int presignExpireSeconds) {
        this.posterJobRepository = posterJobRepository;
        this.posterRenderService = posterRenderService;
        this.fileServiceClient = fileServiceClient;
        this.presignExpireSeconds = presignExpireSeconds;
    }

    public void process(String jobId) {
        if (!StringUtils.hasText(jobId)) {
            return;
        }

        Instant now = Instant.now();
        int claimed = posterJobRepository.transitionStatus(jobId, PosterJobStatus.PENDING, PosterJobStatus.PROCESSING, now);
        if (claimed == 0) {
            return;
        }

        PosterJob job = posterJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        try {
            byte[] bytes = posterRenderService.renderPng(job.getTitle(), job.getSubtitle());
            String subject = job.getSubject();
            String authorization = "Bearer " + JwtUtil.generateToken(subject);
            String filename = "poster-" + jobId + ".png";

            String url = fileServiceClient.uploadAndPresign(bytes, filename, authorization, presignExpireSeconds);
            if (!StringUtils.hasText(url)) {
                throw new IllegalStateException("海报上传失败");
            }

            job.setStatus(PosterJobStatus.SUCCESS);
            job.setResultUrl(url);
            job.setErrorMessage(null);
            job.setUpdatedAt(Instant.now());
            posterJobRepository.save(job);
            log.info("Poster job success: {}", jobId);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (!StringUtils.hasText(msg)) {
                msg = "海报生成失败";
            }
            job.setStatus(PosterJobStatus.FAILED);
            job.setErrorMessage(msg);
            job.setUpdatedAt(Instant.now());
            posterJobRepository.save(job);
            log.warn("Poster job failed: {} - {}", jobId, msg);
        }
    }
}

