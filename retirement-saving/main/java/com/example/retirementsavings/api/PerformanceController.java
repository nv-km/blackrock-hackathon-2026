package com.example.retirementsavings.api;

import com.example.retirementsavings.api.dto.PerformanceResponse;
import com.example.retirementsavings.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Performance", description = "System execution metrics")
public class PerformanceController {

	private static final Logger LOG = LoggerFactory.getLogger(PerformanceController.class);
	private final PerformanceService performanceService;

	public PerformanceController(PerformanceService performanceService) {
		this.performanceService = performanceService;
	}

	@GetMapping("/blackrock/challenge/v1/performance")
	@Operation(
			summary = "Get system performance metrics",
			description = "Returns current timestamp, memory usage in MB, and active thread count"
	)
	public PerformanceResponse getPerformanceMetrics() {
		LOG.info("Received performance metrics request");
		PerformanceResponse response = performanceService.getCurrentMetrics();
		LOG.info(
				"Performance metrics request completed: memory={}, threads={}",
				response.memory(),
				response.threads()
		);
		return response;
	}
}
