package com.swordshield.content.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        contextId = "fileServiceDiscoveryClient",
        name = "${file.service.name:sword-shield-file}",
        configuration = FileFeignConfig.class
)
public interface FileServiceDiscoveryClient extends FileServiceApi {}
