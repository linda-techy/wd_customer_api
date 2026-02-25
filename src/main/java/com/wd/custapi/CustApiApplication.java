package com.wd.custapi;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CustApiApplication {

	public static void main(String[] args) {
		loadDotenv();
		SpringApplication.run(CustApiApplication.class, args);
	}

	private static void loadDotenv() {
		String appEnv = System.getenv("APP_ENV");
		String filename = switch (appEnv != null ? appEnv : "") {
			case "staging" -> ".env.staging";
			case "production" -> ".env.production";
			default -> ".env";
		};
		Dotenv.configure()
				.filename(filename)
				.systemProperties()
				.ignoreIfMissing()
				.load();
	}
}
