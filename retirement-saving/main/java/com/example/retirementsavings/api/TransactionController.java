package com.example.retirementsavings.api;

import com.example.retirementsavings.api.dto.ExpenseInput;
import com.example.retirementsavings.api.dto.TransactionFilterRequest;
import com.example.retirementsavings.api.dto.TransactionFilterResponse;
import com.example.retirementsavings.api.dto.TransactionValidationRequest;
import com.example.retirementsavings.api.dto.TransactionValidationResponse;
import com.example.retirementsavings.api.dto.TransactionOutput;
import com.example.retirementsavings.service.TransactionBuilder;
import com.example.retirementsavings.service.TransactionFilterService;
import com.example.retirementsavings.service.TransactionValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Transactions", description = "Automatic saving transaction helper")
public class TransactionController {

	private static final Logger LOG = LoggerFactory.getLogger(TransactionController.class);
	private final TransactionBuilder transactionBuilder;
	private final TransactionValidator transactionValidator;
	private final TransactionFilterService transactionFilterService;

	public TransactionController(
			TransactionBuilder transactionBuilder,
			TransactionValidator transactionValidator,
			TransactionFilterService transactionFilterService
	) {
		this.transactionBuilder = transactionBuilder;
		this.transactionValidator = transactionValidator;
		this.transactionFilterService = transactionFilterService;
	}

	@PostMapping("/blackrock/challenge/v1/transactions:parse")
	@Operation(
			summary = "Build enriched transactions",
			description = "Converts a list of expenses into transactions with ceiling and remanent values"
	)
	public List<TransactionOutput> parseTransactions(@Valid @RequestBody List<@Valid ExpenseInput> expenses) {
		LOG.info("Received parse request with {} expenses", expenses.size());
		List<TransactionOutput> result = transactionBuilder.getCeilingAndRemnantForTranscations(expenses);
		LOG.info("Parse request completed with {} generated transactions", result.size());
		return result;
	}

	@PostMapping("/blackrock/challenge/v1/transactions:validator")
	@Operation(
			summary = "Validate transactions",
			description = "Validates transactions against wage limits and transaction consistency rules"
	)
	public TransactionValidationResponse validateTransactions(@Valid @RequestBody TransactionValidationRequest request) {
		LOG.info("Received validation request with {} transactions", request.transactions().size());
		TransactionValidationResponse response = transactionValidator.validate(request);
		LOG.info(
				"Validation request completed: valid={}, invalid={}",
				response.valid().size(),
				response.invalid().size()
		);
		return response;
	}

	@PostMapping("/blackrock/challenge/v1/transactions:filter")
	@Operation(
			summary = "Filter transactions by temporal constraints",
			description = "Applies q and p period rules, validates duplicates/negatives, and marks k membership"
	)
	public TransactionFilterResponse filterTransactions(@Valid @RequestBody TransactionFilterRequest request) {
		LOG.info(
				"Received temporal filter request: transactions={}, q={}, p={}, k={}",
				request.transactions().size(),
				request.q().size(),
				request.p().size(),
				request.k().size()
		);
		TransactionFilterResponse response = transactionFilterService.filter(request);
		LOG.info(
				"Temporal filter request completed: valid={}, invalid={}",
				response.valid().size(),
				response.invalid().size()
		);
		return response;
	}
}
