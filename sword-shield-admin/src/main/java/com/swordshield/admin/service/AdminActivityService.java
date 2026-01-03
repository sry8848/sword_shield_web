package com.swordshield.admin.service;

import com.swordshield.common.ErrorCode;
import com.swordshield.common.Response;
import com.swordshield.api.activity.ActivityDto;
import com.swordshield.api.activity.rpc.ActivityAdminRpcService;
import com.swordshield.api.common.PageResult;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 后台活动管理聚合服务。
 *
 * <p>管理端通过 Dubbo/RPC 调用活动服务的管理接口（{@link com.swordshield.api.activity.rpc.ActivityAdminRpcService}），
 * 并将结果包装为统一的 {@link com.swordshield.common.Response} 返回给管理端 Controller。
 */
@Service
public class AdminActivityService {
    @DubboReference(check = false)
    private ActivityAdminRpcService activityAdminRpcService;

    public Response<List<ActivityDto>> getAll(String authorization, String subjectHeader) {
        try {
            return Response.ok(activityAdminRpcService.getAll());
        } catch (Exception e) {
            return Response.error(ErrorCode.SERVICE_UNAVAILABLE, "Activity service unavailable");
        }
    }

    public Response<Map<String, Object>> list(String authorization, String subjectHeader, int page, int size) {
        try {
            PageResult<ActivityDto> p = activityAdminRpcService.list(page, size);
            return Response.ok(Map.of(
                    "items", p.getItems(),
                    "total", p.getTotal(),
                    "page", p.getPage(),
                    "size", p.getSize()
            ));
        } catch (Exception e) {
            return Response.error(ErrorCode.SERVICE_UNAVAILABLE, "Activity service unavailable");
        }
    }

    public Response<ActivityDto> getById(String authorization, String subjectHeader, Long id) {
        try {
            return Response.ok(activityAdminRpcService.getById(id));
        } catch (Exception e) {
            return Response.error(ErrorCode.SERVICE_UNAVAILABLE, "Activity service unavailable");
        }
    }

    public Response<ActivityDto> create(String authorization, String subjectHeader, ActivityDto activity) {
        try {
            return Response.ok(activityAdminRpcService.create(activity));
        } catch (Exception e) {
            return Response.error(ErrorCode.SERVICE_UNAVAILABLE, "Activity service unavailable");
        }
    }

    public Response<ActivityDto> update(String authorization, String subjectHeader, Long id, ActivityDto activity) {
        try {
            return Response.ok(activityAdminRpcService.update(id, activity));
        } catch (Exception e) {
            return Response.error(ErrorCode.SERVICE_UNAVAILABLE, "Activity service unavailable");
        }
    }

    public Response<Object> delete(String authorization, String subjectHeader, Long id) {
        try {
            activityAdminRpcService.delete(id);
            return Response.ok(null);
        } catch (Exception e) {
            return Response.error(ErrorCode.SERVICE_UNAVAILABLE, "Activity service unavailable");
        }
    }
}
