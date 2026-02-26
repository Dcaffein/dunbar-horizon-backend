package com.example.GooRoomBe;

import com.example.GooRoomBe.support.TestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class GooRoomBeApplicationTests {

	@Test
	void contextLoads() {
	}
}