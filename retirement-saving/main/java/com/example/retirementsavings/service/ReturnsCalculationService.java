package com.example.retirementsavings.service;

import com.example.retirementsavings.api.dto.KPeriodInput;
import com.example.retirementsavings.api.dto.ReturnsCalculationRequest;
import com.example.retirementsavings.api.dto.ReturnsCalculationResponse;
import com.example.retirementsavings.api.dto.SavingsByDateOutput;
import com.example.retirementsavings.api.dto.ExpenseInput;
import com.example.retirementsavings.api.dto.TransactionFilterRequest;
import com.example.retirementsavings.api.dto.TransactionFilterResponse;
import com.example.retirementsavings.api.dto.TransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReturnsCalculationService {

	private static final Logger LOG = LoggerFactory.getLogger(ReturnsCalculationService.class);
	private static final double NPS_RATE = 0.0711;
	private static final double INDEX_RATE = 0.1449;
	private static final double NPS_MAX_DEDUCTION = 200_000.0;
	private static final double NPS_INCOME_RATIO_CAP = 0.10;
	private static final int RETIREMENT_AGE = 60;
	private static final int DEFAULT_YEARS_IF_ABOVE_RETIREMENT = 5;

	private final TransactionBuilder transactionBuilder;
	private final TransactionRulesService transactionRulesService;
	private final TransactionFilterService transactionFilterService;

	public ReturnsCalculationService(
			TransactionBuilder transactionBuilder,
			TransactionRulesService transactionRulesService,
			TransactionFilterService transactionFilterService
	) {
		this.transactionBuilder = transactionBuilder;
		this.transactionRulesService = transactionRulesService;
		this.transactionFilterService = transactionFilterService;
	}

	public ReturnsCalculationResponse calculateNps(ReturnsCalculationRequest request) {
		return calculate(request, NPS_RATE, true);
	}

	public ReturnsCalculationResponse calculateIndex(ReturnsCalculationRequest request) {
		return calculate(request, INDEX_RATE, false);
	}

	private ReturnsCalculationResponse calculate(ReturnsCalculationRequest request, double annualRate, boolean includeNpsTaxBenefit) {
		LOG.debug(
				"Starting returns calculation: age={}, wage={}, inflation={}, rate={}, npsMode={}",
				request.age(),
				request.wage(),
				request.inflation(),
				annualRate,
				includeNpsTaxBenefit
		);

		TransactionFilterResponse filtered = transactionFilterService.filter(toFilterRequest(request));
		List<TransactionOutput> validTransactionsForSavings = filtered.valid();
		List<TransactionOutput> validTransactionsForTotals = getValidTransactionsForTotals(request.transactions());

		double totalTransactionAmount = validTransactionsForTotals.stream()
				.mapToDouble(TransactionOutput::getAmount)
				.sum();
		double totalCeiling = validTransactionsForTotals.stream()
				.mapToDouble(TransactionOutput::getCeiling)
				.sum();

		int years = getInvestmentYears(request.age());
		List<SavingsByDateOutput> savingsByDates = request.k().stream()
				.map(period -> buildSavingsByDate(
						period,
						validTransactionsForSavings,
						annualRate,
						request.inflation(),
						years,
						request.wage(),
						includeNpsTaxBenefit
				))
				.toList();

		LOG.debug(
				"Returns calculation completed: validForTotals={}, validForSavings={}, savingsByDates={}",
				validTransactionsForTotals.size(),
				validTransactionsForSavings.size(),
				savingsByDates.size()
		);
		return new ReturnsCalculationResponse(
				round2(totalTransactionAmount),
				round2(totalCeiling),
				savingsByDates
		);
	}

	private SavingsByDateOutput buildSavingsByDate(
			KPeriodInput period,
			List<TransactionOutput> validTransactions,
			double annualRate,
			double inflationPercent,
			int years,
			double wage,
			boolean includeNpsTaxBenefit
	) {
		double amount = calculateAmountForPeriod(validTransactions, period);
		double profit = calculateInflationAdjustedProfit(amount, annualRate, inflationPercent, years);
		double taxBenefit = includeNpsTaxBenefit ? calculateNpsTaxBenefit(amount, wage) : 0.0;

		return new SavingsByDateOutput(
				period.start(),
				period.end(),
				round2(amount),
				round2(profit),
				round2(taxBenefit)
		);
	}

	private double calculateAmountForPeriod(List<TransactionOutput> transactions, KPeriodInput period) {
		if (period.start().isAfter(period.end())) {
			return 0.0;
		}
		return transactions.stream()
				.filter(transaction -> isWithinInclusive(transaction.getDate(), period.start(), period.end()))
				.mapToDouble(TransactionOutput::getRemanent)
				.sum();
	}

	private boolean isWithinInclusive(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
		return !value.isBefore(start) && !value.isAfter(end);
	}

	private double calculateInflationAdjustedProfit(double principal, double annualRate, double inflationPercent, int years) {
		if (principal <= 0) {
			return 0.0;
		}
		double nominalAmount = principal * Math.pow(1 + annualRate, years);
		double inflationAdjustedAmount = nominalAmount / Math.pow(1 + inflationPercent / 100.0, years);
		return inflationAdjustedAmount - principal;
	}

	private double calculateNpsTaxBenefit(double investedAmount, double monthlyWage) {
		if (investedAmount <= 0) {
			return 0.0;
		}

		double annualIncome = monthlyWage * 12.0;
		double deductionLimitByIncome = annualIncome * NPS_INCOME_RATIO_CAP;
		double eligibleDeduction = Math.min(investedAmount, Math.min(deductionLimitByIncome, NPS_MAX_DEDUCTION));
		double taxBefore = calculateTax(annualIncome);
		double taxAfter = calculateTax(Math.max(annualIncome - eligibleDeduction, 0.0));
		return taxBefore - taxAfter;
	}

	private double calculateTax(double income) {
		if (income <= 700_000.0) {
			return 0.0;
		}
		if (income <= 1_000_000.0) {
			return (income - 700_000.0) * 0.10;
		}
		if (income <= 1_200_000.0) {
			return 30_000.0 + (income - 1_000_000.0) * 0.15;
		}
		if (income <= 1_500_000.0) {
			return 60_000.0 + (income - 1_200_000.0) * 0.20;
		}
		return 120_000.0 + (income - 1_500_000.0) * 0.30;
	}

	private int getInvestmentYears(int age) {
		return age < RETIREMENT_AGE ? RETIREMENT_AGE - age : DEFAULT_YEARS_IF_ABOVE_RETIREMENT;
	}

	private double round2(double value) {
		return BigDecimal.valueOf(value)
				.setScale(2, RoundingMode.HALF_UP)
				.doubleValue();
	}

	private TransactionFilterRequest toFilterRequest(ReturnsCalculationRequest request) {
		return new TransactionFilterRequest(
				request.q(),
				request.p(),
				request.k(),
				request.wage(),
				request.transactions()
		);
	}

	private List<TransactionOutput> getValidTransactionsForTotals(List<ExpenseInput> transactions) {
		List<TransactionOutput> validTransactions = new ArrayList<>();
		Set<String> seen = new HashSet<>();

		for (ExpenseInput transaction : transactions) {
			String fingerprint = transactionRulesService.createFingerprint(transaction);
			if (!seen.add(fingerprint)) {
				continue;
			}
			if (transactionRulesService.validateNonNegativeAmount(transaction.getAmount()) != null) {
				continue;
			}
			validTransactions.add(transactionBuilder.calculateCeilingAndRemanent(transaction.getDate(), transaction.getAmount()));
		}

		return validTransactions;
	}
}
