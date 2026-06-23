package com.example.shopnow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ShopNowPaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopNowPaymentApplication.class, args);
	}

	@Bean
	@org.springframework.cloud.client.loadbalancer.LoadBalanced
	public org.springframework.web.client.RestTemplate restTemplate() {
		return new org.springframework.web.client.RestTemplate();
	}
}
