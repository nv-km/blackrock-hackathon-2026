package com.example.retirementsavings.service;

import com.example.retirementsavings.api.dto.ExpenseInput;
import com.example.retirementsavings.api.dto.KPeriodInput;
import com.example.retirementsavings.api.dto.PPeriodInput;
import com.example.retirementsavings.api.dto.QPeriodInput;
import com.example.retirementsavings.api.dto.ReturnsCalculationRequest;
import com.example.retirementsavings.api.dto.ReturnsCalculationResponse;
import com.example.retirementsavings.api.dto.SavingsByDateOutput;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReturnsCalculationServiceTest {

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Test
	void calculatesTotalsAndSavingsByDates() {
		TransactionBuilder builder = new TransactionBuilder();
		TransactionRulesService rules = new TransactionRulesService();
		TransactionFilterService filterService = new TransactionFilterService(builder, rules);
		ReturnsCalculationService service = new ReturnsCalculationService(builder, rules, filterService);

		ReturnsCalculationRequest request = new ReturnsCalculationRequest(
				29,
				50_000.0,
				5.5,
				List.of(new QPeriodInput(
						0.0,
						LocalDateTime.parse("2023-07-01 00:00:00", FORMAT),
						LocalDateTime.parse("2023-07-31 23:59:59", FORMAT)
				)),
				List.of(new PPeriodInput(
						25.0,
						LocalDateTime.parse("2023-10-01 08:00:00", FORMAT),
						LocalDateTime.parse("2023-12-31 23:59:59", FORMAT)
				)),
				List.of(
						new KPeriodInput(
								LocalDateTime.parse("2023-01-01 00:00:00", FORMAT),
								LocalDateTime.parse("2023-12-31 23:59:59", FORMAT)
						),
						new KPeriodInput(
								LocalDateTime.parse("2023-03-01 00:00:00", FORMAT),
								LocalDateTime.parse("2023-11-30 23:59:59", FORMAT)
						)
				),
				List.of(
						new ExpenseInput(LocalDateTime.parse("2023-02-28 15:49:30", FORMAT), 375.0),
						new ExpenseInput(LocalDateTime.parse("2023-07-01 21:59:30", FORMAT), 620.0),
						new ExpenseInput(LocalDateTime.parse("2023-10-12 20:15:30", FORMAT), 250.0),
						new ExpenseInput(LocalDateTime.parse("2023-12-17 08:09:45", FORMAT), 480.0),
						new ExpenseInput(LocalDateTime.parse("2023-12-17 08:09:45", FORMAT), -10.0)
				)
		);

		ReturnsCalculationResponse response = service.calculateNps(request);

		assertEquals(1725.0, response.totalTransactionAmount(), 0.01);
		assertEquals(1900.0, response.totalCeiling(), 0.01);
		assertEquals(2, response.savingsByDates().size());

		SavingsByDateOutput fullYear = response.savingsByDates().get(0);
		SavingsByDateOutput marchToNov = response.savingsByDates().get(1);

		assertEquals(145.0, fullYear.amount(), 0.01);
		assertEquals(86.88, fullYear.profit(), 0.01);
		assertEquals(0.0, fullYear.taxBenefit(), 0.01);

		assertEquals(75.0, marchToNov.amount(), 0.01);
		assertEquals(44.94, marchToNov.profit(), 0.01);
		assertEquals(0.0, marchToNov.taxBenefit(), 0.01);
	}
}
