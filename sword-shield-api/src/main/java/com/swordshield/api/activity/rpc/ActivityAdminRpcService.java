package com.swordshield.api.activity.rpc;

import com.swordshield.api.activity.ActivityDto;
import com.swordshield.api.common.PageResult;

import java.util.List;

/**
 * 活动服务（管理端）RPC 接口。
 *
 * <p>提供管理员侧的活动管理能力（CRUD），供管理后台等服务通过 Dubbo 调用。
 */
public interface ActivityAdminRpcService {
    List<ActivityDto> getAll();

    PageResult<ActivityDto> list(int page, int size);

    ActivityDto getById(Long id);

    ActivityDto create(ActivityDto body);

    ActivityDto update(Long id, ActivityDto body);

    void delete(Long id);
}
