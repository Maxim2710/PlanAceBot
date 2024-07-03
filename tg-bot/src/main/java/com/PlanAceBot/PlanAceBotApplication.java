package com.PlanAceBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlanAceBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlanAceBotApplication.class, args);
	}

}
