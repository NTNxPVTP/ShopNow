package com.example.shopnow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ShopNowGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopNowGatewayApplication.class, args);
	}
}
