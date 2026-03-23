package com.example.shopnow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class ShopnowApplication {
	@Autowired
	private Environment env;

	public static void main(String[] args) {
		SpringApplication.run(ShopnowApplication.class, args);

	}

	@Bean
	public CommandLineRunner debugEnv() {
		return args -> {
			System.out.println("========== DEBUG ENVIRONMENT VARIABLES ==========");
			System.out.println(env.toString());
		};
	}

}
