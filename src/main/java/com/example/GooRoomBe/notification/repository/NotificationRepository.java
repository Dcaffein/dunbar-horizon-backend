package com.example.GooRoomBe.notification.repository;

import com.example.GooRoomBe.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends Neo4jRepository<Notification, String> {

    // 안 읽은 것만 보기
    Slice<Notification> findAllByReceiverIdOrderByCreatedAtDesc(String receiverId, Pageable pageable);

    // 안 읽은 알림 개수
    long countByReceiverIdAndIsReadFalse(String receiverId);

    // 발송 실패한 알림 조회
    List<Notification> findByIsSentFalse();

    //오래된 알림 삭제
    void deleteByCreatedAtBefore(LocalDateTime threshold);
}