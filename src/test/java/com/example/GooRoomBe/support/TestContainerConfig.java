package com.example.GooRoomBe.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainerConfig {

    private static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.3"))
            .withDatabaseName("gooroom_db")
            .withUsername("root")
            .withPassword("test")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"));

    private static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.12"))
            .withAdminPassword("password")
            .withTmpFs(Map.of("/data", "rw", "/logs", "rw"));

    private static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withTmpFs(Map.of("/data/db", "rw"));

    static {
        MARIADB.start();
        NEO4J.start();
        MONGO.start();
    }

    @Bean
    @ServiceConnection
    public MariaDBContainer<?> mariaDBContainer() {
        return MARIADB;
    }

    @Bean
    @ServiceConnection
    public Neo4jContainer<?> neo4jContainer() {
        return NEO4J;
    }

    @Bean
    @ServiceConnection
    public MongoDBContainer mongodbContainer() {
        return MONGO;
    }
}