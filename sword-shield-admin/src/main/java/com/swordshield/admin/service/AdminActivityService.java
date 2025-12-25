package com.swordshield.admin.service;

import com.swordshield.admin.client.ActivityAdminDirectClient;
import com.swordshield.admin.client.ActivityAdminDiscoveryClient;
import com.swordshield.admin.client.ActivityAdminApi;
import com.swordshield.admin.dto.ActivityDto;
import com.swordshield.common.Response;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AdminActivityService {
    private final ActivityAdminDiscoveryClient discoveryClient;
    private final ObjectProvider<ActivityAdminDirectClient> directClientProvider;

    public AdminActivityService(ActivityAdminDiscoveryClient discoveryClient,
                                ObjectProvider<ActivityAdminDirectClient> directClientProvider) {
        this.discoveryClient = discoveryClient;
        this.directClientProvider = directClientProvider;
    }

    public Response<List<ActivityDto>> getAll(String authorization, String subjectHeader) {
        Response<List<ActivityDto>> res;
        try {
            res = api().getAll(authorization, subjectHeader);
        } catch (Exception e) {
            return Response.error(502, "Activity service unavailable");
        }
        return res == null ? Response.error(502, "Activity service unavailable") : res;
    }

    public Response<Map<String, Object>> list(String authorization, String subjectHeader, int page, int size) {
        Response<Map<String, Object>> res;
        try {
            res = api().list(authorization, subjectHeader, page, size);
        } catch (Exception e) {
            return Response.error(502, "Activity service unavailable");
        }
        return res == null ? Response.error(502, "Activity service unavailable") : res;
    }

    public Response<ActivityDto> getById(String authorization, String subjectHeader, Long id) {
        Response<ActivityDto> res;
        try {
            res = api().getById(authorization, subjectHeader, id);
        } catch (Exception e) {
            return Response.error(502, "Activity service unavailable");
        }
        return res == null ? Response.error(502, "Activity service unavailable") : res;
    }

    public Response<ActivityDto> create(String authorization, String subjectHeader, ActivityDto activity) {
        Response<ActivityDto> res;
        try {
            res = api().create(authorization, subjectHeader, activity);
        } catch (Exception e) {
            return Response.error(502, "Activity service unavailable");
        }
        return res == null ? Response.error(502, "Activity service unavailable") : res;
    }

    public Response<ActivityDto> update(String authorization, String subjectHeader, Long id, ActivityDto activity) {
        Response<ActivityDto> res;
        try {
            res = api().update(authorization, subjectHeader, id, activity);
        } catch (Exception e) {
            return Response.error(502, "Activity service unavailable");
        }
        return res == null ? Response.error(502, "Activity service unavailable") : res;
    }

    public Response<Object> delete(String authorization, String subjectHeader, Long id) {
        Response<Object> res;
        try {
            res = api().delete(authorization, subjectHeader, id);
        } catch (Exception e) {
            return Response.error(502, "Activity service unavailable");
        }
        return res == null ? Response.error(502, "Activity service unavailable") : res;
    }

    private ActivityAdminApi api() {
        ActivityAdminDirectClient direct = directClientProvider.getIfAvailable();
        return direct != null ? direct : discoveryClient;
    }
}
