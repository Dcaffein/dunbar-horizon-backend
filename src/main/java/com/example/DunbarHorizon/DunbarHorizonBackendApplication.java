package com.example.DunbarHorizon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DunbarHorizonBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DunbarHorizonBackendApplication.class, args);
	}

}
