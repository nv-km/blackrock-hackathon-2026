package com.example.retirementsavings.api.dto;

import java.util.List;

public record ReturnsCalculationResponse(
		double totalTransactionAmount,
		double totalCeiling,
		List<SavingsByDateOutput> savingsByDates
) {}
