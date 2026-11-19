package com.example.DunbarHorizon.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlTestContainerConfigTest {

    private static final String DATABASE_IDENTITY = "SELECT CONCAT(@@server_uuid, '/', DATABASE())";

    private final ApplicationContextRunner contexts = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withUserConfiguration(MySqlTestContainerConfig.class)
            .withPropertyValues("spring.datasource.hikari.minimum-idle=0");

    @Test
    @DisplayName("한 컨텍스트를 닫아도 다른 컨텍스트가 공용 MySQL을 조회한다")
    void query_Succeeds_AfterOtherContextCloses() {
        contexts.run(survivingContext -> {
            // given
            DataSource survivingDataSource = survivingContext.getBean(DataSource.class);
            var survivingJdbc = new JdbcTemplate(survivingDataSource);
            String database = survivingJdbc.queryForObject(DATABASE_IDENTITY, String.class);

            // when
            contexts.run(closingContext -> {
                DataSource closingDataSource = closingContext.getBean(DataSource.class);
                assertThat(closingDataSource).isNotSameAs(survivingDataSource);
                assertThat(new JdbcTemplate(closingDataSource).queryForObject(DATABASE_IDENTITY, String.class))
                        .isEqualTo(database);
            });

            // then
            assertThat(survivingJdbc.queryForObject(DATABASE_IDENTITY, String.class)).isEqualTo(database);
        });
    }
}
