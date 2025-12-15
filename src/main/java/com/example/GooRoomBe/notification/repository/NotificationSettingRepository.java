package com.example.GooRoomBe.notification.repository;

import com.example.GooRoomBe.notification.domain.NotificationSetting;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface NotificationSettingRepository extends Neo4jRepository<NotificationSetting, String> {
}