package com.swordshield.file;

import com.swordshield.file.repository.AssetFileRepository;
import com.swordshield.file.service.FileService;
import io.minio.MinioClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class FileServiceTest {
    @Test
    void constructOk() {
        MinioClient client = Mockito.mock(MinioClient.class);
        AssetFileRepository repo = Mockito.mock(AssetFileRepository.class);
        FileService service = new FileService(client, repo, "bucket");
        Assertions.assertNotNull(service);
    }
}

