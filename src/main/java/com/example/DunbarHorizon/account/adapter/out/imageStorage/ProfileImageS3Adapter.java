package com.example.DunbarHorizon.account.adapter.out.imageStorage;

import com.example.DunbarHorizon.account.application.port.out.ProfileImageStoragePort;
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

@Component
@RequiredArgsConstructor
public class ProfileImageS3Adapter implements ProfileImageStoragePort {

    private static final Duration PRESIGN_TTL = Duration.ofHours(1);

    private final S3Presigner s3Presigner;
    private final S3ImageUrlResolver s3ImageUrlResolver;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Override
    public PresignedUploadResult presignUpload(String contentType) {
        String key = "profiles/" + UuidUtil.createV7();
        String uploadUrl = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                        .signatureDuration(PRESIGN_TTL)
                        .putObjectRequest(PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(contentType)
                                .build())
                        .build())
                .url()
                .toString();
        return new PresignedUploadResult(uploadUrl, key);
    }

    @Override
    public String resolveUrl(String key) {
        return s3ImageUrlResolver.resolveUrl(key);
    }
}
