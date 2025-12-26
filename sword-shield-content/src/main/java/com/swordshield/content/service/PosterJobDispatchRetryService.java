package com.swordshield.content.service;

import com.swordshield.content.domain.PosterJob;
import com.swordshield.content.domain.PosterJobStatus;
import com.swordshield.content.mq.PosterJobDispatcher;
import com.swordshield.content.repository.PosterJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PosterJobDispatchRetryService {
    private static final Logger log = LoggerFactory.getLogger(PosterJobDispatchRetryService.class);

    private final PosterJobRepository posterJobRepository;
    private final PosterJobDispatcher dispatcher;

    private final int maxAttempts;
    private final Duration lockDuration;
    private final Duration baseDelay;
    private final Duration maxDelay;
    private final int batchSize;

    public PosterJobDispatchRetryService(PosterJobRepository posterJobRepository,
                                        PosterJobDispatcher dispatcher,
                                        @Value("${app.poster.mq.dispatch-retry.max-attempts:10}") int maxAttempts,
                                        @Value("${app.poster.mq.dispatch-retry.lock-ms:15000}") long lockMs,
                                        @Value("${app.poster.mq.dispatch-retry.base-delay-ms:1000}") long baseDelayMs,
                                        @Value("${app.poster.mq.dispatch-retry.max-delay-ms:30000}") long maxDelayMs,
                                        @Value("${app.poster.mq.dispatch-retry.batch-size:50}") int batchSize) {
        this.posterJobRepository = posterJobRepository;
        this.dispatcher = dispatcher;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.lockDuration = Duration.ofMillis(Math.max(1000, lockMs));
        this.baseDelay = Duration.ofMillis(Math.max(100, baseDelayMs));
        this.maxDelay = Duration.ofMillis(Math.max(this.baseDelay.toMillis(), maxDelayMs));
        this.batchSize = Math.max(1, batchSize);
    }

    public void dispatchNow(String jobId) {
        attemptDispatch(jobId);
    }

    @Scheduled(
            fixedDelayString = "${app.poster.mq.dispatch-retry.scheduler-delay-ms:2000}",
            initialDelayString = "${app.poster.mq.dispatch-retry.scheduler-initial-delay-ms:2000}"
    )
    public void dispatchDueJobs() {
        Instant now = Instant.now();
        List<PosterJob> jobs = posterJobRepository.findDueDispatchJobs(
                PosterJobStatus.PENDING,
                now,
                PageRequest.of(0, batchSize)
        );
        if (jobs.isEmpty()) {
            return;
        }
        for (PosterJob job : jobs) {
            attemptDispatch(job.getId());
        }
    }

    private void attemptDispatch(String jobId) {
        if (!StringUtils.hasText(jobId)) {
            return;
        }

        Instant now = Instant.now();
        int claimed = posterJobRepository.claimDispatchAttempt(
                jobId,
                PosterJobStatus.PENDING,
                now,
                now.plus(lockDuration),
                maxAttempts
        );
        if (claimed == 0) {
            return;
        }

        PosterJob job = posterJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        try {
            dispatcher.dispatch(jobId);
            job.setDispatchLastError(null);
            job.setDispatchNextRetryAt(null);
            job.setUpdatedAt(Instant.now());
            posterJobRepository.save(job);
            log.info("Poster job dispatched: {} attempt={}", jobId, job.getDispatchAttempts());
        } catch (Exception e) {
            String err = summarize(e);
            Instant failAt = Instant.now();
            int attempts = job.getDispatchAttempts();
            if (attempts >= maxAttempts) {
                job.setStatus(PosterJobStatus.FAILED);
                job.setErrorMessage("任务投递失败（已重试 " + attempts + " 次）: " + err);
                job.setDispatchLastError(err);
                job.setDispatchNextRetryAt(null);
                job.setUpdatedAt(failAt);
                posterJobRepository.save(job);
                log.warn("Poster job dispatch exhausted: {} attempts={}, err={}", jobId, attempts, err);
                return;
            }

            Instant nextRetryAt = failAt.plus(computeBackoff(attempts));
            job.setDispatchLastError(err);
            job.setDispatchNextRetryAt(nextRetryAt);
            job.setUpdatedAt(failAt);
            posterJobRepository.save(job);
            log.warn("Poster job dispatch failed: {} attempt={}, nextRetryAt={}, err={}", jobId, attempts, nextRetryAt, err);
        }
    }

    private Duration computeBackoff(int attempts) {
        // attempts starts from 1
        int exp = Math.max(0, Math.min(attempts - 1, 20));
        long delay = safeMultiply(baseDelay.toMillis(), 1L << exp);
        delay = Math.min(delay, maxDelay.toMillis());
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.min(500, delay + 1));
        return Duration.ofMillis(delay + jitter);
    }

    private static long safeMultiply(long a, long b) {
        if (a <= 0 || b <= 0) return 0;
        if (a > Long.MAX_VALUE / b) return Long.MAX_VALUE;
        return a * b;
    }

    private static String summarize(Throwable t) {
        if (t == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendThrowable(sb, t);
        Throwable cause = t.getCause();
        int depth = 0;
        while (cause != null && cause != t && depth++ < 3) {
            sb.append("; cause=");
            appendThrowable(sb, cause);
            cause = cause.getCause();
        }
        String s = sb.toString();
        return s.length() > 512 ? s.substring(0, 512) + "..." : s;
    }

    private static void appendThrowable(StringBuilder sb, Throwable t) {
        sb.append(t.getClass().getSimpleName());
        String msg = t.getMessage();
        if (StringUtils.hasText(msg)) {
            sb.append(": ").append(msg.replace('\n', ' ').replace('\r', ' '));
        }
    }
}

