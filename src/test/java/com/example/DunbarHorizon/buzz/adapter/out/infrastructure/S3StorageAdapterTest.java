package com.example.DunbarHorizon.buzz.adapter.out.infrastructure;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

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

    private static S3Client s3Client;
    private S3StorageAdapter adapter;

    @BeforeAll
    static void setUpContainer() {
        s3Client = S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(S3))
                .region(Region.of(REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .build();

        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
    }

    @BeforeEach
    void setUp() {
        adapter = new S3StorageAdapter(s3Client);
        ReflectionTestUtils.setField(adapter, "bucket", BUCKET);
        ReflectionTestUtils.setField(adapter, "region", REGION);
    }

    @Test
    @DisplayName("파일을 업로드하면 S3 URL 형식의 문자열을 반환한다")
    void upload_ReturnsS3Url() {
        // given
        MultipartFile file = new MockMultipartFile(
                "images", "test.jpg", "image/jpeg", "image-content".getBytes());

        // when
        List<String> urls = adapter.upload(List.of(file));

        // then
        assertThat(urls).hasSize(1);
        assertThat(urls.get(0)).contains(BUCKET).contains(REGION).endsWith("test.jpg");
    }

    @Test
    @DisplayName("빈 파일 목록을 전달하면 빈 리스트를 반환한다")
    void upload_EmptyList_ReturnsEmptyList() {
        // when
        List<String> urls = adapter.upload(List.of());

        // then
        assertThat(urls).isEmpty();
    }

    @Test
    @DisplayName("여러 파일을 업로드하면 각 파일에 대한 URL을 반환한다")
    void upload_MultipleFiles_ReturnsMultipleUrls() {
        // given
        MultipartFile file1 = new MockMultipartFile("images", "a.jpg", "image/jpeg", "a".getBytes());
        MultipartFile file2 = new MockMultipartFile("images", "b.jpg", "image/jpeg", "b".getBytes());

        // when
        List<String> urls = adapter.upload(List.of(file1, file2));

        // then
        assertThat(urls).hasSize(2);
        assertThat(urls.get(0)).endsWith("a.jpg");
        assertThat(urls.get(1)).endsWith("b.jpg");
    }
}
