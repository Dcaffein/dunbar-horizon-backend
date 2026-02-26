package com.example.GooRoomBe.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
        basePackages = {"com.example.GooRoomBe.notification.adapter.out.persistence.mongo",
                "com.example.GooRoomBe.cast.adapter.out.persistence.mongo"}
)
@EnableMongoAuditing
public class MongoConfig {
}