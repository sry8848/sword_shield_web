package com.swordshield.activity.rpc;

import com.swordshield.activity.service.ActivityAdminService;
import com.swordshield.api.activity.ActivityDto;
import com.swordshield.api.activity.rpc.ActivityAdminRpcService;
import com.swordshield.api.common.PageResult;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.domain.Page;

import java.util.List;

@DubboService
public class ActivityAdminRpcServiceImpl implements ActivityAdminRpcService {
    private final ActivityAdminService activityAdminService;

    public ActivityAdminRpcServiceImpl(ActivityAdminService activityAdminService) {
        this.activityAdminService = activityAdminService;
    }

    @Override
    public List<ActivityDto> getAll() {
        return activityAdminService.getAll();
    }

    @Override
    public PageResult<ActivityDto> list(int page, int size) {
        Page<ActivityDto> p = activityAdminService.list(page, size);
        PageResult<ActivityDto> result = new PageResult<>();
        result.setItems(p.getContent());
        result.setTotal(p.getTotalElements());
        result.setPage(p.getNumber());
        result.setSize(p.getSize());
        return result;
    }

    @Override
    public ActivityDto getById(Long id) {
        return activityAdminService.getById(id);
    }

    @Override
    public ActivityDto create(ActivityDto body) {
        return activityAdminService.create(body);
    }

    @Override
    public ActivityDto update(Long id, ActivityDto body) {
        return activityAdminService.update(id, body);
    }

    @Override
    public void delete(Long id) {
        activityAdminService.delete(id);
    }
}

