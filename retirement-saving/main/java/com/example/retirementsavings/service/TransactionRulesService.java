package com.example.retirementsavings.service;

import com.example.retirementsavings.api.dto.ExpenseInput;
import com.example.retirementsavings.api.dto.InvalidTransactionOutput;
import com.example.retirementsavings.api.dto.TransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransactionRulesService {

	private static final Logger LOG = LoggerFactory.getLogger(TransactionRulesService.class);
	private static final double EPSILON = 1e-6;

	public String createFingerprint(ExpenseInput expense) {
		return expense.getDate() + "|" + expense.getAmount();
	}

	public String createFingerprint(TransactionOutput transaction) {
		return transaction.getDate() + "|" + transaction.getAmount() + "|" + transaction.getCeiling() + "|" + transaction.getRemanent();
	}

	public String validateNonNegativeAmount(double amount) {
		if (amount < 0) {
			LOG.debug("Amount validation failed: negative amount={}", amount);
			return "Negative amounts are not allowed";
		}
		return null;
	}

	public String validateNonNegativeRemanent(double remanent) {
		if (remanent < 0) {
			LOG.debug("Remanent validation failed: negative remanent={}", remanent);
			return "Remanent cannot be negative";
		}
		return null;
	}

	public String validateTransactionConsistency(TransactionOutput transaction) {
		String errorMessage = validateNonNegativeAmount(transaction.getAmount());
		if (errorMessage != null) {
			LOG.debug("Transaction consistency failed for {}: {}", transaction.getDate(), errorMessage);
			return errorMessage;
		}
		if (transaction.getCeiling() < transaction.getAmount()) {
			LOG.debug(
					"Transaction consistency failed for {}: ceiling {} < amount {}",
					transaction.getDate(),
					transaction.getCeiling(),
					transaction.getAmount()
			);
			return "Ceiling must be greater than or equal to amount";
		}
		double expectedRemanent = transaction.getCeiling() - transaction.getAmount();
		if (Math.abs(expectedRemanent - transaction.getRemanent()) > EPSILON) {
			LOG.debug(
					"Transaction consistency failed for {}: expected remanent {} but got {}",
					transaction.getDate(),
					expectedRemanent,
					transaction.getRemanent()
			);
			return "Remanent must be equal to ceiling minus amount";
		}
		return validateNonNegativeRemanent(transaction.getRemanent());
	}

	public InvalidTransactionOutput toInvalidTransaction(TransactionOutput transaction, String message) {
		return new InvalidTransactionOutput(
				transaction.getDate(),
				transaction.getAmount(),
				transaction.getCeiling(),
				transaction.getRemanent(),
				message
		);
	}

	public InvalidTransactionOutput toInvalidTransaction(ExpenseInput expense, String message) {
		return new InvalidTransactionOutput(
				expense.getDate(),
				expense.getAmount(),
				message
		);
	}
}
