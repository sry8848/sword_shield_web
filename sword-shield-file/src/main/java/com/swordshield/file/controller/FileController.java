package com.swordshield.file.controller;

import com.swordshield.common.JwtUtil;
import com.swordshield.common.Response;
import com.swordshield.file.domain.AssetFile;
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
    public Response<AssetFile> upload(
                                      @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
                                      @RequestHeader("Authorization") String authorization,
                                      @RequestPart("file") MultipartFile file) throws Exception {
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        String subject = JwtUtil.parseSubject(token);
        if (subject != null && subject.startsWith("admin_")) {
            subject = subject.substring("admin_".length());
        }
        Long userId = Long.valueOf(subject);
        
        // Generate unique object name: UUID + original extension
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = UUID.randomUUID().toString() + extension;
        
        AssetFile meta = service.putObject(objectName, originalFilename, file.getContentType(), file.getSize(), file.getInputStream(), userId);
        return Response.ok(meta);
    }

    @GetMapping("/presigned")
    @Operation(summary = "获取预签名下载链接", description = "生成短期可访问链接")
    public Response<String> presigned(@RequestParam String objectName, @RequestParam(defaultValue = "300") int expireSeconds) throws Exception {
        return Response.ok(service.presignedGet(objectName, expireSeconds));
    }
}

