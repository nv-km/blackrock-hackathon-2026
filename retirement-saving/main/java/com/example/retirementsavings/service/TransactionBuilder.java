package com.example.retirementsavings.service;

import com.example.retirementsavings.api.dto.ExpenseInput;
import com.example.retirementsavings.api.dto.TransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(TransactionBuilder.class);
	private static final double CEILING_STEP = 100.0;

	public List<TransactionOutput> getCeilingAndRemnantForTranscations(List<ExpenseInput> expenses) {
		LOG.debug("Calculating ceiling/remanent for {} expenses", expenses.size());
		List<TransactionOutput> results = expenses.stream()
				.map(this::getCeilingAndRemnantForATranscation)
				.toList();
		LOG.debug("Ceiling/remanent calculation completed for {} expenses", results.size());
		return results;
	}

	public TransactionOutput calculateCeilingAndRemanent(LocalDateTime date, double amount) {
		double ceiling = Math.ceil(amount / CEILING_STEP) * CEILING_STEP;
		double remanent = ceiling - amount;
		return new TransactionOutput(date, amount, ceiling, remanent);
	}

	private TransactionOutput getCeilingAndRemnantForATranscation(ExpenseInput expense) {
		return calculateCeilingAndRemanent(expense.getDate(), expense.getAmount());
	}
}
