package com.swordshield.file.controller;

import com.swordshield.common.JwtUtil;
import com.swordshield.common.Response;
import com.swordshield.file.domain.AssetFile;
import com.swordshield.file.service.FileService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {
    private final FileService service;
    public FileController(FileService service) { this.service = service; }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<AssetFile> upload(@RequestHeader("Authorization") String authorization,
                                      @RequestPart("file") MultipartFile file) throws Exception {
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        Long userId = Long.valueOf(JwtUtil.parseSubject(token));
        
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
    public Response<String> presigned(@RequestParam String objectName, @RequestParam(defaultValue = "300") int expireSeconds) throws Exception {
        return Response.ok(service.presignedGet(objectName, expireSeconds));
    }
}

