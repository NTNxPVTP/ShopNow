package com.example.shopnow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class ShopNowCartApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopNowCartApplication.class, args);
	}

	@Bean
	@org.springframework.cloud.client.loadbalancer.LoadBalanced
	public RestTemplate restTemplate() {
		return new org.springframework.web.client.RestTemplate();
	}

}
