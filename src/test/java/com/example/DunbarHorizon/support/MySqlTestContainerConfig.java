package com.example.DunbarHorizon.support;

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestContainerConfig {

    @Bean
    public JdbcConnectionDetails mysqlConnectionDetails() {
        MySQLContainer<?> mysql = SharedMySql.CONTAINER;
        return new JdbcConnectionDetails() {
            @Override
            public String getJdbcUrl() {
                return mysql.getJdbcUrl();
            }

            @Override
            public String getUsername() {
                return mysql.getUsername();
            }

            @Override
            public String getPassword() {
                return mysql.getPassword();
            }
        };
    }

    // MySQL은 첫 JDBC 접속 정보 요청에서 시작되고 테스트 JVM이 끝날 때 정리된다.
    private static class SharedMySql {
        private static final MySQLContainer<?> CONTAINER = start();

        private static MySQLContainer<?> start() {
            MySQLContainer<?> container = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("dunbar_horizon_db")
                    .withUsername("root")
                    .withPassword("test");
            container.start();
            return container;
        }
    }
}
