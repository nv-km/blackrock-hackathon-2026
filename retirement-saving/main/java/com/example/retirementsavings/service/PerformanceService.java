package com.example.retirementsavings.service;

import com.example.retirementsavings.api.dto.PerformanceResponse;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PerformanceService {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	private static final double BYTES_IN_MB = 1024.0 * 1024.0;

	public PerformanceResponse getCurrentMetrics() {
		Runtime runtime = Runtime.getRuntime();
		double usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_IN_MB;
		String memory = round2(usedMemoryMb) + " MB";

		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		int threads = threadMXBean.getThreadCount();

		String time = LocalDateTime.now().format(DATE_TIME_FORMATTER);
		return new PerformanceResponse(time, memory, threads);
	}

	private String round2(double value) {
		return BigDecimal.valueOf(value)
				.setScale(2, RoundingMode.HALF_UP)
				.toPlainString();
	}
}
