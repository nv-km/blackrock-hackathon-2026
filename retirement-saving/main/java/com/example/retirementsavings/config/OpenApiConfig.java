package com.example.retirementsavings.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "Retirement Savings API",
				version = "v1",
				description = "Endpoints for auto-saving investment decisions"
		),
		servers = @Server(url = "/", description = "Default server")
)
public class OpenApiConfig {

}
