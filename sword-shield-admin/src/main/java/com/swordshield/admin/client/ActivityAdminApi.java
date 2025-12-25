package com.swordshield.admin.client;

import com.swordshield.common.Response;
import com.swordshield.api.activity.ActivityDto;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

public interface ActivityAdminApi {
    @GetMapping("/api/activity/admin/activities")
    Response<List<ActivityDto>> getAll(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Auth-Subject", required = false) String subject
    );

    @GetMapping("/api/activity/admin/activities/list")
    Response<Map<String, Object>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Auth-Subject", required = false) String subject,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );

    @GetMapping("/api/activity/admin/activities/{id}")
    Response<ActivityDto> getById(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Auth-Subject", required = false) String subject,
            @PathVariable("id") Long id
    );

    @PostMapping("/api/activity/admin/activities")
    Response<ActivityDto> create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Auth-Subject", required = false) String subject,
            @RequestBody ActivityDto body
    );

    @PutMapping("/api/activity/admin/activities/{id}")
    Response<ActivityDto> update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Auth-Subject", required = false) String subject,
            @PathVariable("id") Long id,
            @RequestBody ActivityDto body
    );

    @DeleteMapping("/api/activity/admin/activities/{id}")
    Response<Object> delete(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Auth-Subject", required = false) String subject,
            @PathVariable("id") Long id
    );
}
