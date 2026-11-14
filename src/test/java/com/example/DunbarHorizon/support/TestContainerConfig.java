package com.example.DunbarHorizon.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
@Import(MySqlTestContainerConfig.class)
public class TestContainerConfig {

    private static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withReuse(true);

    static {
        MONGO.start();
    }

    @Bean
    @ServiceConnection
    public MongoDBContainer mongodbContainer() {
        return MONGO;
    }
}
