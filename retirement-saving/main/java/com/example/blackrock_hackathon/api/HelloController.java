package com.example.blackrock_hackathon.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	@GetMapping("/api/hello")
	public HelloResponse hello() {
		return new HelloResponse("Hello, Navin!");
	}

	public record HelloResponse(String message) {}
}
