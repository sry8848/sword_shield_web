package com.swordshield.admin.controller;

import com.swordshield.admin.service.AdminActivityService;
import com.swordshield.api.activity.ActivityDto;
import com.swordshield.common.AuthUtils;
import com.swordshield.common.Response;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/activities")
@Tag(name = "活动管理（管理员）", description = "管理员查看、创建、修改、删除活动")
public class AdminActivityController {
    private final AdminActivityService activityService;

    public AdminActivityController(AdminActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    @Operation(summary = "全部活动列表", description = "管理员查询全部活动")
    public Response<java.util.List<ActivityDto>> getAll(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        return activityService.getAll(authorization, subjectHeader);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询活动", description = "管理员分页查询活动列表")
    public Response<java.util.Map<String, Object>> list(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        return activityService.list(authorization, subjectHeader, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "活动详情（管理员）", description = "根据 ID 查询活动详情")
    public Response<ActivityDto> get(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader,
            @PathVariable("id") Long id) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        return activityService.getById(authorization, subjectHeader, id);
    }

    @PostMapping
    @Operation(summary = "创建活动", description = "管理员创建新活动")
    public Response<ActivityDto> create(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader,
            @RequestBody ActivityDto activity) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        return activityService.create(authorization, subjectHeader, activity);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新活动", description = "管理员修改活动信息")
    public Response<ActivityDto> update(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader,
            @PathVariable("id") Long id,
            @RequestBody ActivityDto activity) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        return activityService.update(authorization, subjectHeader, id, activity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除活动", description = "管理员删除活动")
    public Response<Object> delete(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader,
            @PathVariable("id") Long id) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        return activityService.delete(authorization, subjectHeader, id);
    }
}
