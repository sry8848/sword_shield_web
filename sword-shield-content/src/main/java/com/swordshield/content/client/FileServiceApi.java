package com.swordshield.content.client;

import com.swordshield.common.Response;
import com.swordshield.api.file.AssetFileDto;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

public interface FileServiceApi {
    @PostMapping(value = "/api/file/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Response<AssetFileDto> upload(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestPart("file") Resource file
    );

    @GetMapping("/api/file/presigned")
    Response<String> presigned(
            @RequestParam("objectName") String objectName,
            @RequestParam("expireSeconds") int expireSeconds
    );
}
