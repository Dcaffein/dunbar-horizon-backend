package com.example.DunbarHorizon.global.config;

import com.example.DunbarHorizon.global.config.database.Neo4jConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 캐시 어드바이스가 트랜잭션 어드바이스보다 바깥에 걸리는지 지킨다.
 *
 * 메서드에 어노테이션을 적은 순서는 어드바이스 체인 순서를 정하지 않는다. 순서는 어드바이저의
 * order로 정해지는데, 캐시와 트랜잭션의 기본값이 둘 다 LOWEST_PRECEDENCE라 명시하지 않으면
 * 설정 클래스 등록 순서에 따라 뒤집힌다. 트랜잭션이 바깥으로 잡히면 캐시 히트에도
 * Neo4j 세션 획득과 BEGIN·COMMIT 왕복이 붙는다.
 */
class CacheAdviceOrderTest {

    @Test
    @DisplayName("캐시 어드바이저의 order가 트랜잭션 어드바이저보다 앞선다")
    void 캐시가_트랜잭션보다_바깥에_걸린다() {
        // given
        EnableCaching caching = RedisConfig.class.getAnnotation(EnableCaching.class);
        EnableTransactionManagement transaction = Neo4jConfig.class.getAnnotation(EnableTransactionManagement.class);

        // when
        int cacheOrder = caching.order();
        int transactionOrder = transaction.order();

        // then: order가 작을수록 바깥이다
        assertThat(cacheOrder).isLessThan(transactionOrder);
    }

    @Test
    @DisplayName("캐시 어드바이저를 가장 바깥으로는 두지 않는다")
    void 캐시가_최상위_우선순위는_아니다() {
        // given
        EnableCaching caching = RedisConfig.class.getAnnotation(EnableCaching.class);

        // when
        int cacheOrder = caching.order();

        // then: 메서드 인가처럼 캐시보다 바깥에 있어야 하는 어드바이저의 자리를 남겨둔다
        assertThat(cacheOrder).isGreaterThan(Ordered.HIGHEST_PRECEDENCE);
    }
}
