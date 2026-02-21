package com.example.retirementsavings.service;

import com.example.retirementsavings.api.dto.InvalidTransactionOutput;
import com.example.retirementsavings.api.dto.TransactionOutput;
import com.example.retirementsavings.api.dto.TransactionValidationRequest;
import com.example.retirementsavings.api.dto.TransactionValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TransactionValidator {

	private static final Logger LOG = LoggerFactory.getLogger(TransactionValidator.class);
	private static final double MAX_INVESTABLE_WAGE_RATIO = 0.30;
	private static final double EPSILON = 1e-6;
	private final TransactionRulesService transactionRulesService;

	public TransactionValidator(TransactionRulesService transactionRulesService) {
		this.transactionRulesService = transactionRulesService;
	}

	public TransactionValidationResponse validate(TransactionValidationRequest request) {
		double maxInvestableAmount = request.wage() * MAX_INVESTABLE_WAGE_RATIO;
		double currentInvested = 0.0;
		LOG.debug(
				"Starting transaction validation for {} transactions with maxInvestableAmount={}",
				request.transactions().size(),
				maxInvestableAmount
		);

		List<TransactionOutput> valid = new ArrayList<>();
		List<InvalidTransactionOutput> invalid = new ArrayList<>();
		Set<String> seenTransactions = new HashSet<>();

		for (TransactionOutput transaction : request.transactions()) {
			String fingerprint = transactionRulesService.createFingerprint(transaction);
			if (!seenTransactions.add(fingerprint)) {
				LOG.debug("Duplicate transaction detected at {}", transaction.getDate());
				invalid.add(transactionRulesService.toInvalidTransaction(transaction, "Duplicate transaction"));
				continue;
			}

			String errorMessage = transactionRulesService.validateTransactionConsistency(transaction);
			if (errorMessage != null) {
				invalid.add(transactionRulesService.toInvalidTransaction(transaction, errorMessage));
				continue;
			}

			errorMessage = validateInvestmentLimit(currentInvested, maxInvestableAmount, transaction);
			if (errorMessage != null) {
				LOG.debug(
						"Investment limit exceeded for transaction at {} with remanent={}",
						transaction.getDate(),
						transaction.getRemanent()
				);
				invalid.add(transactionRulesService.toInvalidTransaction(transaction, errorMessage));
				continue;
			}

			valid.add(transaction);
			currentInvested += transaction.getRemanent();
		}

		LOG.debug("Validation completed: valid={}, invalid={}", valid.size(), invalid.size());
		return new TransactionValidationResponse(valid, invalid);
	}

	private String validateInvestmentLimit(double currentInvested, double maxInvestableAmount, TransactionOutput transaction) {
		if (currentInvested + transaction.getRemanent() > maxInvestableAmount + EPSILON) {
			return "Transaction exceeds maximum investable amount based on wage";
		}
		return null;
	}
}
