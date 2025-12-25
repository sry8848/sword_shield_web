package com.swordshield.admin.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cloud.openfeign.FeignClient;

@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${activity.service.base-url:}')")
@FeignClient(
        contextId = "activityAdminDirectClient",
        name = "activity-admin-direct",
        url = "${activity.service.base-url}",
        configuration = AdminFeignConfig.class
)
public interface ActivityAdminDirectClient extends ActivityAdminApi {}
