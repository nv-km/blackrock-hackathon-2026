package com.example.retirementsavings.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record TransactionValidationRequest(
		@NotNull
		@Positive
		Double wage,
		@NotNull
		List<@Valid TransactionOutput> transactions
) {}
