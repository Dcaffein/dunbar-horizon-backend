package com.example.DunbarHorizon.support;

import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataNeo4jTest
@ActiveProfiles("test")
@Import({TestContainerConfig.class})
@Transactional
public @interface Neo4jRepositoryTest {
}