package com.example.DunbarHorizon.buzz.adapter.out.imageStorage;

import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.global.model.PresignRequest;
import com.example.DunbarHorizon.global.model.PresignedUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import com.example.DunbarHorizon.global.util.UuidUtil;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class S3StorageAdapter implements ImageStoragePort {

    private static final Duration PRESIGN_TTL = Duration.ofHours(1);

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Override
    public List<PresignedUploadResult> presignUploads(List<PresignRequest> requests) {
        return requests.stream()
                .map(request -> {
                    String key = "buzz/" + UuidUtil.createV7();
                    String uploadUrl = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                                    .signatureDuration(PRESIGN_TTL)
                                    .putObjectRequest(PutObjectRequest.builder()
                                            .bucket(bucket)
                                            .key(key)
                                            .contentType(request.contentType())
                                            .build())
                                    .build())
                            .url()
                            .toString();
                    return new PresignedUploadResult(uploadUrl, key);
                })
                .toList();
    }

    @Override
    public List<String> resolveUrls(List<String> keys) {
        return keys.stream()
                .map(this::resolveUrl)
                .toList();
    }

    private String resolveUrl(String key) {
        if (key.startsWith("https://")) {
            return key;
        }
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(PRESIGN_TTL)
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build())
                        .build())
                .url()
                .toString();
    }
}
