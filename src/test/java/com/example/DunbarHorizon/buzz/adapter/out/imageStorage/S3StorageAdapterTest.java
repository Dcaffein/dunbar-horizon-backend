package com.example.DunbarHorizon.buzz.adapter.out.imageStorage;

import com.example.DunbarHorizon.global.imageStorage.PresignRequest;
import com.example.DunbarHorizon.global.imageStorage.PresignedUploadResult;
import com.example.DunbarHorizon.global.imageStorage.S3ImageUrlResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Testcontainers
class S3StorageAdapterTest {

    private static final String BUCKET = "test-bucket";
    private static final String REGION = "ap-northeast-2";

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(S3);

    private static S3Presigner s3Presigner;
    private S3StorageAdapter adapter;

    @BeforeAll
    static void setUpContainer() {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey()));

        S3Client s3Client = S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(S3))
                .region(Region.of(REGION))
                .credentialsProvider(credentials)
                .build();
        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());

        s3Presigner = S3Presigner.builder()
                .endpointOverride(localStack.getEndpointOverride(S3))
                .region(Region.of(REGION))
                .credentialsProvider(credentials)
                .build();
    }

    @BeforeEach
    void setUp() {
        S3ImageUrlResolver resolver = new S3ImageUrlResolver(s3Presigner);
        ReflectionTestUtils.setField(resolver, "bucket", BUCKET);
        adapter = new S3StorageAdapter(s3Presigner, resolver);
        ReflectionTestUtils.setField(adapter, "bucket", BUCKET);
    }

    @Test
    @DisplayName("presignUploads 호출 시 buzz/ 접두사를 가진 key와 uploadUrl을 반환한다")
    void presignUploads_ReturnsBuzzKeyAndUploadUrl() {
        List<PresignedUploadResult> results = adapter.presignUploads(
                List.of(new PresignRequest("image/jpeg", 1024)));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).objectKey()).startsWith("buzz/");
        assertThat(results.get(0).uploadUrl()).isNotBlank();
    }

    @Test
    @DisplayName("presignUploads에 여러 요청을 전달하면 각각의 결과를 반환한다")
    void presignUploads_MultipleRequests_ReturnsMultipleResults() {
        List<PresignedUploadResult> results = adapter.presignUploads(List.of(
                new PresignRequest("image/jpeg", 1024),
                new PresignRequest("image/png", 2048)));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).objectKey()).isNotEqualTo(results.get(1).objectKey());
    }

    @Test
    @DisplayName("resolveUrls 호출 시 presigned GET URL을 반환한다")
    void resolveUrls_ReturnsPresignedGetUrl() {
        String key = "buzz/some-uuid";

        List<String> urls = adapter.resolveUrls(List.of(key));

        assertThat(urls).hasSize(1);
        assertThat(urls.get(0)).isNotBlank();
        assertThat(urls.get(0)).contains(key);
    }

    @Test
    @DisplayName("resolveUrls에 https:// 로 시작하는 기존 URL을 전달하면 그대로 반환한다")
    void resolveUrls_LegacyHttpsUrl_ReturnedAsIs() {
        String legacyUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/profiles/old-photo.jpg";

        List<String> urls = adapter.resolveUrls(List.of(legacyUrl));

        assertThat(urls).containsExactly(legacyUrl);
    }
}
