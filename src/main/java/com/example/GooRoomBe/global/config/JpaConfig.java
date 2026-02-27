package com.example.GooRoomBe.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
        basePackages = {
                "com.example.GooRoomBe.account",
                "com.example.GooRoomBe.cast",
                "com.example.GooRoomBe.trace",
                "com.example.GooRoomBe.notification.adapter.out.persistence.jpa"
        }
)
public class JpaConfig {

}