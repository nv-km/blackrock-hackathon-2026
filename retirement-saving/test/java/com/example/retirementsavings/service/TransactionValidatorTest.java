package com.example.retirementsavings.service;

import com.example.retirementsavings.api.dto.TransactionOutput;
import com.example.retirementsavings.api.dto.TransactionValidationRequest;
import com.example.retirementsavings.api.dto.TransactionValidationResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionValidatorTest {

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Test
	void flagsDuplicatesAndInvestmentLimit() {
		TransactionRulesService rules = new TransactionRulesService();
		TransactionValidator validator = new TransactionValidator(rules);

		LocalDateTime date1 = LocalDateTime.parse("2023-10-12 20:15:30", FORMAT);
		LocalDateTime date2 = LocalDateTime.parse("2023-02-28 20:15:30", FORMAT);

		TransactionOutput t1 = new TransactionOutput(date1, 250.0, 300.0, 50.0);
		TransactionOutput duplicate = new TransactionOutput(date1, 250.0, 300.0, 50.0);
		TransactionOutput t2 = new TransactionOutput(date2, 375.0, 400.0, 25.0);

		TransactionValidationRequest request = new TransactionValidationRequest(
				200.0,
				List.of(t1, duplicate, t2)
		);

		TransactionValidationResponse response = validator.validate(request);

		assertEquals(1, response.valid().size());
		assertEquals(2, response.invalid().size());
		assertEquals("Duplicate transaction", response.invalid().get(0).getMessage());
		assertEquals("Transaction exceeds maximum investable amount based on wage", response.invalid().get(1).getMessage());
	}
}
