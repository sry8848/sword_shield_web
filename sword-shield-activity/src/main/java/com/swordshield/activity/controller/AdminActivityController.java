package com.swordshield.activity.controller;

import com.swordshield.activity.service.ActivityAdminService;
import com.swordshield.common.AuthUtils;
import com.swordshield.common.Response;
import com.swordshield.api.activity.ActivityDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/activity/admin/activities")
@Tag(name = "活动管理（管理员/活动服务）", description = "活动服务内部的管理员活动 CRUD 接口")
public class AdminActivityController {
    private final ActivityAdminService activityService;

    public AdminActivityController(ActivityAdminService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    @Operation(summary = "全部活动列表", description = "管理员查询全部活动")
    public Response<java.util.List<ActivityDto>> getAll(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        return Response.ok(activityService.getAll());
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询活动", description = "管理员分页查询活动列表")
    public Response<Map<String, Object>> list(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        Page<ActivityDto> p = activityService.list(page, size);
        return Response.ok(Map.of(
                "items", p.getContent(),
                "total", p.getTotalElements(),
                "page", p.getNumber(),
                "size", p.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "活动详情（管理员）", description = "根据 ID 查询活动详情")
    public Response<ActivityDto> get(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader,
            @PathVariable("id") Long id) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        return Response.ok(activityService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建活动", description = "管理员创建新活动")
    public Response<ActivityDto> create(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader,
            @RequestBody ActivityDto activity) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        return Response.ok(activityService.create(activity));
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
        return Response.ok(activityService.update(id, activity));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除活动", description = "管理员删除活动")
    public Response<Object> delete(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader,
            @PathVariable("id") Long id) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        activityService.delete(id);
        return Response.ok(null);
    }
}
