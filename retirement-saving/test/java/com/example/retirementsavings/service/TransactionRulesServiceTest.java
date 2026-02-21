package com.example.retirementsavings.service;

import com.example.retirementsavings.api.dto.InvalidTransactionOutput;
import com.example.retirementsavings.api.dto.TransactionOutput;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionRulesServiceTest {

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Test
	void validatesNegativeAmount() {
		TransactionRulesService rules = new TransactionRulesService();
		String message = rules.validateNonNegativeAmount(-10.0);

		assertEquals("Negative amounts are not allowed", message);
	}

	@Test
	void validatesTransactionConsistency() {
		TransactionRulesService rules = new TransactionRulesService();
		LocalDateTime date = LocalDateTime.parse("2023-10-12 20:15:30", FORMAT);

		TransactionOutput ceilingTooLow = new TransactionOutput(date, 200.0, 150.0, -50.0);
		assertEquals("Ceiling must be greater than or equal to amount", rules.validateTransactionConsistency(ceilingTooLow));

		TransactionOutput wrongRemanent = new TransactionOutput(date, 200.0, 300.0, 50.0);
		assertEquals("Remanent must be equal to ceiling minus amount", rules.validateTransactionConsistency(wrongRemanent));

		TransactionOutput valid = new TransactionOutput(date, 200.0, 300.0, 100.0);
		assertNull(rules.validateTransactionConsistency(valid));
	}

	@Test
	void preservesTransactionFieldsInInvalidOutput() {
		TransactionRulesService rules = new TransactionRulesService();
		LocalDateTime date = LocalDateTime.parse("2023-02-28 20:15:30", FORMAT);
		TransactionOutput transaction = new TransactionOutput(date, 250.0, 300.0, 50.0);

		InvalidTransactionOutput invalid = rules.toInvalidTransaction(transaction, "Duplicate transaction");

		assertEquals(250.0, invalid.getAmount());
		assertEquals(300.0, invalid.getCeiling());
		assertEquals(50.0, invalid.getRemanent());
		assertEquals("Duplicate transaction", invalid.getMessage());
	}
}
