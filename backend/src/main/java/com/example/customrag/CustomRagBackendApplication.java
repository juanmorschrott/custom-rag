package com.example.customrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CustomRagBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomRagBackendApplication.class, args);
	}

}
