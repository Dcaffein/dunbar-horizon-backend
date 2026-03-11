package com.example.DunbarHorizon.global.config.database;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
        basePackages = {"com.example.DunbarHorizon.notification.adapter.out.persistence.mongo",
                "com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo"}
)
@EnableMongoAuditing
public class MongoConfig { }