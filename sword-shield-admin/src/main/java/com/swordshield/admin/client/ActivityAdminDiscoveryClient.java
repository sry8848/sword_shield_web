package com.swordshield.admin.client;

import com.swordshield.common.Response;
import com.swordshield.api.activity.ActivityDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(
        contextId = "activityAdminDiscoveryClient",
        name = "${activity.service.name:sword-shield-activity}",
        configuration = AdminFeignConfig.class
)
public interface ActivityAdminDiscoveryClient extends ActivityAdminApi {}
