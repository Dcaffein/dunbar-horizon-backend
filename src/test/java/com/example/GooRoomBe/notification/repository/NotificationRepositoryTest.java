package com.example.GooRoomBe.notification.repository;

import com.example.GooRoomBe.notification.domain.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
@Testcontainers
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Container
    @ServiceConnection
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5");

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired private Neo4jClient neo4jClient;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("findByIsSentFalse: 발송 실패한(안 보낸) 알림만 조회한다")
    void findByIsSentFalse_Test() {
        // Given
        Notification sent = Notification.builder().isSent(true).receiverId("A").build();
        Notification failed1 = Notification.builder().isSent(false).receiverId("B").build();
        Notification failed2 = Notification.builder().isSent(false).receiverId("C").build();

        notificationRepository.saveAll(List.of(sent, failed1, failed2));

        // When
        List<Notification> result = notificationRepository.findByIsSentFalse();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("receiverId").containsExactlyInAnyOrder("B", "C");
    }
}