package com.example.DunbarHorizon.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainerConfig {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("dunbar_horizon_db")
            .withUsername("root")
            .withPassword("test")
            .withReuse(true);

    private static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.12"))
            .withAdminPassword("password")
            .withEnv("NEO4J_PLUGINS", "[\"apoc\"]")
            .withNeo4jConfig("dbms.security.procedures.unrestricted", "apoc.*")
            .withNeo4jConfig("dbms.security.procedures.allowlist", "apoc.*")
            .withReuse(true);

    private static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withReuse(true);

    static {
        MYSQL.start();
        NEO4J.start();
        MONGO.start();
    }

    @Bean
    @ServiceConnection
    public MySQLContainer<?> mariaDBContainer() {
        return MYSQL;
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