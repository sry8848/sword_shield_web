package com.swordshield.admin.service;

import com.swordshield.admin.dto.ActivityDto;
import com.swordshield.common.Response;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AdminActivityService {
    private final ActivityAdminClient client;

    public AdminActivityService(ActivityAdminClient client) {
        this.client = client;
    }

    public Response<List<ActivityDto>> getAll(String authorization) {
        Response<List<ActivityDto>> res = client.getAll(authorization);
        return res == null ? Response.error(502, "Activity service unavailable") : res;
    }

    public Response<Map<String, Object>> list(String authorization, int page, int size) {
        Response<Map<String, Object>> res = client.list(authorization, page, size);
        return res == null ? Response.error(502, "Activity service unavailable") : res;
    }

    public Response<ActivityDto> getById(String authorization, Long id) {
        Response<ActivityDto> res = client.getById(authorization, id);
        return res == null ? Response.error(502, "Activity service unavailable") : res;
    }

    public Response<ActivityDto> create(String authorization, ActivityDto activity) {
        Response<ActivityDto> res = client.create(authorization, activity);
        return res == null ? Response.error(502, "Activity service unavailable") : res;
    }

    public Response<ActivityDto> update(String authorization, Long id, ActivityDto activity) {
        Response<ActivityDto> res = client.update(authorization, id, activity);
        return res == null ? Response.error(502, "Activity service unavailable") : res;
    }

    public Response<Object> delete(String authorization, Long id) {
        Response<Object> res = client.delete(authorization, id);
        return res == null ? Response.error(502, "Activity service unavailable") : res;
    }
}
