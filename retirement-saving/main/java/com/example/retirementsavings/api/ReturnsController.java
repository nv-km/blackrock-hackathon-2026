package com.example.retirementsavings.api;

import com.example.retirementsavings.api.dto.ReturnsCalculationRequest;
import com.example.retirementsavings.api.dto.ReturnsCalculationResponse;
import com.example.retirementsavings.service.ReturnsCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Returns", description = "Investment returns calculators")
public class ReturnsController {

	private static final Logger LOG = LoggerFactory.getLogger(ReturnsController.class);
	private final ReturnsCalculationService returnsCalculationService;

	public ReturnsController(ReturnsCalculationService returnsCalculationService) {
		this.returnsCalculationService = returnsCalculationService;
	}

	@PostMapping("/blackrock/challenge/v1/returns:nps")
	@Operation(
			summary = "Calculate NPS returns",
			description = "Calculates savings, inflation-adjusted profit, and NPS tax benefit by k periods"
	)
	public ReturnsCalculationResponse calculateNpsReturns(@Valid @RequestBody ReturnsCalculationRequest request) {
		LOG.info(
				"Received NPS returns request: transactions={}, q={}, p={}, k={}",
				request.transactions().size(),
				request.q().size(),
				request.p().size(),
				request.k().size()
		);
		ReturnsCalculationResponse response = returnsCalculationService.calculateNps(request);
		LOG.info("NPS returns request completed with {} k-period entries", response.savingsByDates().size());
		return response;
	}

	@PostMapping("/blackrock/challenge/v1/returns:index")
	@Operation(
			summary = "Calculate index fund returns",
			description = "Calculates savings and inflation-adjusted profit by k periods for index investment"
	)
	public ReturnsCalculationResponse calculateIndexReturns(@Valid @RequestBody ReturnsCalculationRequest request) {
		LOG.info(
				"Received index returns request: transactions={}, q={}, p={}, k={}",
				request.transactions().size(),
				request.q().size(),
				request.p().size(),
				request.k().size()
		);
		ReturnsCalculationResponse response = returnsCalculationService.calculateIndex(request);
		LOG.info("Index returns request completed with {} k-period entries", response.savingsByDates().size());
		return response;
	}
}
