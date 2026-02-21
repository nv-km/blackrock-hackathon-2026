package com.example.retirementsavings.service;

import com.example.retirementsavings.api.dto.PerformanceResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceServiceTest {

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	@Test
	void reportsCurrentMetrics() {
		PerformanceService service = new PerformanceService();

		PerformanceResponse response = service.getCurrentMetrics();

		LocalDateTime.parse(response.time(), FORMAT);
		assertTrue(response.memory().endsWith(" MB"));
		assertTrue(response.threads() > 0);
	}
}
