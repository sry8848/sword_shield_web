package com.swordshield.content.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cloud.openfeign.FeignClient;

@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${file.service.base-url:}')")
@FeignClient(
        contextId = "fileServiceDirectClient",
        name = "file-service-direct",
        url = "${file.service.base-url}",
        configuration = FileFeignConfig.class
)
public interface FileServiceDirectClient extends FileServiceApi {}
