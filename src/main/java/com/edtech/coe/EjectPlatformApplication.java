package com.edtech.coe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EjectPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(EjectPlatformApplication.class, args);
	}

}
