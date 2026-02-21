package com.example.retirementsavings.service;

import com.example.retirementsavings.api.dto.TransactionOutput;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionBuilderTest {

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Test
	void calculatesCeilingAndRemanent() {
		TransactionBuilder builder = new TransactionBuilder();
		LocalDateTime date = LocalDateTime.parse("2023-10-12 20:15:30", FORMAT);

		TransactionOutput result = builder.calculateCeilingAndRemanent(date, 250.0);

		assertEquals(300.0, result.getCeiling());
		assertEquals(50.0, result.getRemanent());
	}

	@Test
	void handlesExactHundreds() {
		TransactionBuilder builder = new TransactionBuilder();
		LocalDateTime date = LocalDateTime.parse("2023-10-12 20:15:30", FORMAT);

		TransactionOutput result = builder.calculateCeilingAndRemanent(date, 400.0);

		assertEquals(400.0, result.getCeiling());
		assertEquals(0.0, result.getRemanent());
	}
}
