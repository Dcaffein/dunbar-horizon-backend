package com.example.DunbarHorizon;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRetry
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class DunbarHorizonBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DunbarHorizonBackendApplication.class, args);
	}

}
