package com.vsk.devtrust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DevtrustApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevtrustApplication.class, args);
	}

}
