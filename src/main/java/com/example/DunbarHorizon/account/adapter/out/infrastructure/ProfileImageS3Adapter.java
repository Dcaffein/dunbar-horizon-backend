package com.example.DunbarHorizon.account.adapter.out.infrastructure;

import com.example.DunbarHorizon.account.application.model.UploadFile;
import com.example.DunbarHorizon.account.application.port.out.ProfileImageStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfileImageS3Adapter implements ProfileImageStoragePort {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    @Override
    public String upload(UploadFile file) {
        String key = "profiles/" + UUID.randomUUID() + "_" + file.originalFilename();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.contentType())
                        .contentLength((long) file.content().length)
                        .build(),
                RequestBody.fromBytes(file.content())
        );

        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
    }
}
