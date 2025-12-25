package com.swordshield.file.controller;

import com.swordshield.common.AuthUtils;
import com.swordshield.common.Response;
import com.swordshield.api.file.AssetFileDto;
import com.swordshield.file.service.FileService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/file")
@Tag(name = "文件", description = "文件上传与访问接口")
public class FileController {
    private final FileService service;
    public FileController(FileService service) { this.service = service; }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件", description = "需携带 JWT，支持 multipart 文件上传")
    public Response<AssetFileDto> upload(
                                      @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
                                      @RequestHeader(name = "Authorization", required = false) String authorization,
                                      @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
                                      @RequestPart("file") MultipartFile file) throws Exception {
        Long userId = AuthUtils.requireUserIdAllowAdmin(userIdHeader, authorization);
        
        // Generate unique object name: UUID + original extension
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = UUID.randomUUID().toString() + extension;
        
        AssetFileDto meta = service.putObject(objectName, originalFilename, file.getContentType(), file.getSize(), file.getInputStream(), userId);
        return Response.ok(meta);
    }

    @GetMapping("/presigned")
    @Operation(summary = "获取预签名下载链接", description = "生成短期可访问链接")
    public Response<String> presigned(@RequestParam String objectName, @RequestParam(defaultValue = "300") int expireSeconds) throws Exception {
        return Response.ok(service.presignedGet(objectName, expireSeconds));
    }
}

