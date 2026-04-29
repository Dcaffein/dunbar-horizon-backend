package com.example.DunbarHorizon.buzz.adapter.out.infrastructure;

import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3StorageAdapter implements ImageStoragePort {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    @Override
    public List<String> upload(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            String key = UUID.randomUUID() + "_" + file.getOriginalFilename();
            try {
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(file.getContentType())
                                .contentLength(file.getSize())
                                .build(),
                        RequestBody.fromBytes(file.getBytes())
                );
            } catch (IOException e) {
                throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
            }
            urls.add("https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key));
        }
        return urls;
    }
}
