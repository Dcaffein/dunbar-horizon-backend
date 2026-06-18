package com.example.DunbarHorizon.global.imageStorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ImageUrlResolver {

    private static final Duration PRESIGN_TTL = Duration.ofHours(1);

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public String resolveUrl(String key) {
        if (key == null || key.isBlank()) return null;
        if (key.startsWith("https://")) {
            // 이전 코드가 DB에 직접 저장한 만료된 presigned URL — key를 추출해 재서명
            if (key.contains("X-Amz-")) {
                return extractKeyAndPresign(key);
            }
            return key;
        }
        return presign(key);
    }

    public List<String> resolveUrls(List<String> keys) {
        return keys.stream().map(this::resolveUrl).toList();
    }

    private String extractKeyAndPresign(String presignedUrl) {
        try {
            String path = URI.create(presignedUrl).getPath();
            String extractedKey = path.startsWith("/") ? path.substring(1) : path;
            if (extractedKey.isBlank()) return null;
            return presign(extractedKey);
        } catch (Exception e) {
            log.warn("[S3ImageUrlResolver] Failed to extract key from legacy presigned URL");
            return null;
        }
    }

    private String presign(String key) {
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
