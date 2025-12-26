package com.swordshield.content.controller;

import com.swordshield.common.AuthUtils;
import com.swordshield.common.Response;
import com.swordshield.content.dto.PosterJobCreateResponse;
import com.swordshield.content.dto.PosterJobStatusResponse;
import com.swordshield.content.service.PosterJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content")
@Validated
@Tag(name = "海报", description = "异步生成海报并上传到文件服务（RocketMQ 任务队列）")
public class PosterController {
    private static final Logger log = LoggerFactory.getLogger(PosterController.class);
    private final PosterJobService posterJobService;

    public PosterController(PosterJobService posterJobService) {
        this.posterJobService = posterJobService;
    }

    @PostMapping(value = "/poster", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "提交海报生成任务", description = "立即返回 jobId，前端轮询查询任务状态后获取可访问链接")
    public Response<PosterJobCreateResponse> createPosterJob(
            @RequestParam(name = "title") @NotBlank(message = "title 不能为空") String title,
            @RequestParam(name = "subtitle") @NotBlank(message = "subtitle 不能为空") String subtitle,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader) {
        String subject = AuthUtils.requireSubject(subjectHeader, authorization);
        PosterJobCreateResponse res = posterJobService.createJob(subject, title, subtitle);
        log.info("Poster job created: {}", res.getJobId());
        return Response.ok(res);
    }

    @GetMapping("/poster/jobs/{jobId}")
    @Operation(summary = "查询海报任务状态", description = "根据 jobId 返回任务状态与最终链接（成功时）")
    public Response<PosterJobStatusResponse> getPosterJob(
            @PathVariable("jobId") String jobId,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader) {
        String subject = AuthUtils.requireSubject(subjectHeader, authorization);
        return Response.ok(posterJobService.getJobStatus(subject, jobId));
    }
}

