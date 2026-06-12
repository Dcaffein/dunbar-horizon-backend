package com.example.DunbarHorizon.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * @DataNeo4jTest 슬라이스에서 "transactionManager"로 등록되는 Neo4j TM을
 * "neo4jTransactionManager" 별칭으로 추가 노출한다.
 * 같은 인스턴스를 공유하므로 테스트 @Transactional 롤백이 정상 동작한다.
 */
@TestConfiguration
public class TestNeo4jConfig implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        registry.registerAlias("transactionManager", "neo4jTransactionManager");
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {}
}
