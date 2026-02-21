package com.example.retirementsavings.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record TransactionFilterRequest(
		@NotNull
		List<@Valid QPeriodInput> q,
		@NotNull
		List<@Valid PPeriodInput> p,
		@NotNull
		List<@Valid KPeriodInput> k,
		@NotNull
		@Positive
		Double wage,
		@NotNull
		List<@Valid ExpenseInput> transactions
) {}
