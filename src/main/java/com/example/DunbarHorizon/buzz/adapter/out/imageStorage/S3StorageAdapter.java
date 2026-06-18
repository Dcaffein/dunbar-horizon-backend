package com.example.DunbarHorizon.buzz.adapter.out.imageStorage;

import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.global.imageStorage.PresignRequest;
import com.example.DunbarHorizon.global.imageStorage.PresignedUploadResult;
import com.example.DunbarHorizon.global.imageStorage.S3ImageUrlResolver;
import com.example.DunbarHorizon.global.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class S3StorageAdapter implements ImageStoragePort {

    private static final Duration PRESIGN_TTL = Duration.ofHours(1);

    private final S3Presigner s3Presigner;
    private final S3ImageUrlResolver s3ImageUrlResolver;

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
        return s3ImageUrlResolver.resolveUrls(keys);
    }
}
