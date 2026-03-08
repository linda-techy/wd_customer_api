package com.wd.custapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CustApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustApiApplication.class, args);
	}
}
