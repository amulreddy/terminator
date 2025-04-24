package com.autowares.mongoose;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.autowares.ServiceDiscoveryConfig;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
		servers = {
			@Server(url = "/", description = "Default Server URL")
		}
)
@SpringBootApplication
@EnableConfigurationProperties(ServiceDiscoveryConfig.class)
public class MongooseSystemCoordinatorApplication {
	public static void main(String args[]) {
		SpringApplication.run(MongooseSystemCoordinatorApplication.class, args);
	}
}
