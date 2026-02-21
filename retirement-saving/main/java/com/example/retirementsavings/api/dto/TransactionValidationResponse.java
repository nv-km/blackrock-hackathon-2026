package com.example.retirementsavings.api.dto;

import java.util.List;

public record TransactionValidationResponse(
		List<TransactionOutput> valid,
		List<InvalidTransactionOutput> invalid
) {}
