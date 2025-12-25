package com.swordshield.api.activity.rpc;

import com.swordshield.api.activity.ActivityDto;
import com.swordshield.api.common.PageResult;

import java.util.List;

public interface ActivityAdminRpcService {
    List<ActivityDto> getAll();

    PageResult<ActivityDto> list(int page, int size);

    ActivityDto getById(Long id);

    ActivityDto create(ActivityDto body);

    ActivityDto update(Long id, ActivityDto body);

    void delete(Long id);
}

