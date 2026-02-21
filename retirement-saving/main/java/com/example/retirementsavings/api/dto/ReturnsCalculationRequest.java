package com.example.retirementsavings.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record ReturnsCalculationRequest(
		@NotNull
		@Min(0)
		Integer age,
		@NotNull
		@Positive
		Double wage,
		@NotNull
		@PositiveOrZero
		Double inflation,
		@NotNull
		List<@Valid QPeriodInput> q,
		@NotNull
		List<@Valid PPeriodInput> p,
		@NotNull
		List<@Valid KPeriodInput> k,
		@NotNull
		List<@Valid ExpenseInput> transactions
) {}
