package com.example.retirementsavings.api.dto;

public record PerformanceResponse(
		String time,
		String memory,
		int threads
) {}
