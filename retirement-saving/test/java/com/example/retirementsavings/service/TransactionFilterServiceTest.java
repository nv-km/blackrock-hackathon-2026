package com.example.retirementsavings.service;

import com.example.retirementsavings.api.dto.ExpenseInput;
import com.example.retirementsavings.api.dto.KPeriodInput;
import com.example.retirementsavings.api.dto.PPeriodInput;
import com.example.retirementsavings.api.dto.QPeriodInput;
import com.example.retirementsavings.api.dto.TransactionFilterRequest;
import com.example.retirementsavings.api.dto.TransactionFilterResponse;
import com.example.retirementsavings.api.dto.TransactionOutput;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionFilterServiceTest {

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Test
	void appliesQAndPPeriodsAndExcludesNonInvested() {
		TransactionBuilder builder = new TransactionBuilder();
		TransactionRulesService rules = new TransactionRulesService();
		TransactionFilterService service = new TransactionFilterService(builder, rules);

		List<ExpenseInput> transactions = List.of(
				new ExpenseInput(LocalDateTime.parse("2023-02-28 15:49:30", FORMAT), 375.0),
				new ExpenseInput(LocalDateTime.parse("2023-07-15 10:30:30", FORMAT), 620.0),
				new ExpenseInput(LocalDateTime.parse("2023-10-12 20:15:30", FORMAT), 250.0),
				new ExpenseInput(LocalDateTime.parse("2023-10-12 20:15:30", FORMAT), 250.0),
				new ExpenseInput(LocalDateTime.parse("2023-12-17 08:09:45", FORMAT), -480.0)
		);

		List<QPeriodInput> qPeriods = List.of(
				new QPeriodInput(0.0,
						LocalDateTime.parse("2023-07-01 00:00:00", FORMAT),
						LocalDateTime.parse("2023-07-31 23:59:59", FORMAT))
		);
		List<PPeriodInput> pPeriods = List.of(
				new PPeriodInput(30.0,
						LocalDateTime.parse("2023-10-01 00:00:00", FORMAT),
						LocalDateTime.parse("2023-12-31 23:59:59", FORMAT))
		);
		List<KPeriodInput> kPeriods = List.of(
				new KPeriodInput(
						LocalDateTime.parse("2023-01-01 00:00:00", FORMAT),
						LocalDateTime.parse("2023-12-31 23:59:59", FORMAT))
		);

		TransactionFilterRequest request = new TransactionFilterRequest(qPeriods, pPeriods, kPeriods, 50_000.0, transactions);
		TransactionFilterResponse response = service.filter(request);

		assertEquals(2, response.valid().size());
		assertEquals(2, response.invalid().size());
		assertEquals("Duplicate transaction", response.invalid().get(0).getMessage());
		assertEquals("Negative amounts are not allowed", response.invalid().get(1).getMessage());

		TransactionOutput first = response.valid().get(0);
		TransactionOutput second = response.valid().get(1);
		assertEquals(25.0, first.getRemanent());
		assertEquals(80.0, second.getRemanent());
		assertTrue(first.getInKPeriod());
		assertTrue(second.getInKPeriod());
	}
}
