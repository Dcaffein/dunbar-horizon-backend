package com.example.DunbarHorizon;

import com.example.DunbarHorizon.support.TestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class DunbarHorizonBackendApplicationTests {

	@Test
	void contextLoads() {
	}
}