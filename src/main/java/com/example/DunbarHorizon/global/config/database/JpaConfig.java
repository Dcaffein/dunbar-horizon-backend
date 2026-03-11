package com.example.DunbarHorizon.global.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
        basePackages = {
                "com.example.DunbarHorizon.account",
                "com.example.DunbarHorizon.buzz",
                "com.example.DunbarHorizon.trace",
                "com.example.DunbarHorizon.notification.adapter.out.persistence.jpa",
                "com.example.DunbarHorizon.flag"
        }
)
public class JpaConfig {
        @Primary
        @Bean(name = "transactionManager")
        public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
                return new JpaTransactionManager(entityManagerFactory);
        }
}