package com.example.retirementsavings.api.dto;

import java.util.List;

public record TransactionFilterResponse(
		List<TransactionOutput> valid,
		List<InvalidTransactionOutput> invalid
) {}
