package com.yerin.jobq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobqApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobqApplication.class, args);
	}

}
