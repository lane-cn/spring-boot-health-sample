package com.mydomain.app.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Main {
	
	@Value("${my.health.indicator.command}")
	private String command;

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

	@Bean
	public HealthIndicator myHealthIndicator() {
		return new MyHealthIndicator(command);
	}
}
