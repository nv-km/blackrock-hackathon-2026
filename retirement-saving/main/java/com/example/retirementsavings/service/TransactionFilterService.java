package com.example.retirementsavings.service;

import com.example.retirementsavings.api.dto.ExpenseInput;
import com.example.retirementsavings.api.dto.InvalidTransactionOutput;
import com.example.retirementsavings.api.dto.KPeriodInput;
import com.example.retirementsavings.api.dto.PPeriodInput;
import com.example.retirementsavings.api.dto.QPeriodInput;
import com.example.retirementsavings.api.dto.TransactionFilterRequest;
import com.example.retirementsavings.api.dto.TransactionFilterResponse;
import com.example.retirementsavings.api.dto.TransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
public class TransactionFilterService {

	private static final Logger LOG = LoggerFactory.getLogger(TransactionFilterService.class);
	private final TransactionBuilder transactionBuilder;
	private final TransactionRulesService transactionRulesService;

	public TransactionFilterService(TransactionBuilder transactionBuilder, TransactionRulesService transactionRulesService) {
		this.transactionBuilder = transactionBuilder;
		this.transactionRulesService = transactionRulesService;
	}

	public TransactionFilterResponse filter(TransactionFilterRequest request) {
		List<ExpenseInput> transactions = request.transactions();
		LOG.debug(
				"Starting temporal filtering: transactions={}, qPeriods={}, pPeriods={}, kPeriods={}",
				transactions.size(),
				request.q().size(),
				request.p().size(),
				request.k().size()
		);
		// Processing order is fixed: base rounding -> q override -> p addition -> k evaluation.
		List<TransactionOutput> baseTransactions = transactionBuilder.getCeilingAndRemnantForTranscations(transactions);
		double[] qFixedByIndex = computeApplicableQFixed(transactions, request.q());
		double[] pExtraByIndex = computeApplicablePExtra(transactions, request.p());

		List<TransactionOutput> processedValid = new ArrayList<>();
		List<InvalidTransactionOutput> invalid = new ArrayList<>();
		Set<String> seen = new HashSet<>();

		for (int i = 0; i < transactions.size(); i++) {
			ExpenseInput transaction = transactions.get(i);
			String fingerprint = transactionRulesService.createFingerprint(transaction);
			// First occurrence wins; later occurrences are treated as invalid duplicates.
			if (!seen.add(fingerprint)) {
				LOG.debug("Duplicate transaction rejected at {}", transaction.getDate());
				invalid.add(transactionRulesService.toInvalidTransaction(transaction, "Duplicate transaction"));
				continue;
			}
			String errorMessage = transactionRulesService.validateNonNegativeAmount(transaction.getAmount());
			if (errorMessage != null) {
				LOG.debug("Transaction rejected at {} due to amount validation", transaction.getDate());
				invalid.add(transactionRulesService.toInvalidTransaction(transaction, errorMessage));
				continue;
			}
			TransactionOutput base = baseTransactions.get(i);
			double remanent = base.getRemanent();
			// q replaces remanent, while p always adds on top of the current remanent.
			if (!Double.isNaN(qFixedByIndex[i])) {
				remanent = qFixedByIndex[i];
			}
			remanent += pExtraByIndex[i];

			TransactionOutput adjusted = new TransactionOutput(
					transaction.getDate(),
					transaction.getAmount(),
					base.getCeiling(),
					remanent,
					null
			);
			if (adjusted.getRemanent() <= 0) {
				LOG.debug("Transaction ignored at {} because remanent is not added: {}", transaction.getDate(), adjusted.getRemanent());
				continue;
			}
			processedValid.add(adjusted);
		}

		boolean[] inKPeriodByIndex = computeKMembership(processedValid, request.k());
		List<TransactionOutput> valid = new ArrayList<>(processedValid.size());
		for (int i = 0; i < processedValid.size(); i++) {
			TransactionOutput transaction = processedValid.get(i);
			// Reuse TransactionOutput for all APIs and fill inKPeriod only for this endpoint.
			valid.add(new TransactionOutput(
					transaction.getDate(),
					transaction.getAmount(),
					transaction.getCeiling(),
					transaction.getRemanent(),
					inKPeriodByIndex[i]
			));
		}

		LOG.debug(
				"Temporal filtering completed: valid={}, invalid={}",
				valid.size(),
				invalid.size()
		);
		return new TransactionFilterResponse(valid, invalid);
	}

	private double[] computeApplicableQFixed(List<ExpenseInput> transactions, List<QPeriodInput> periods) {
		double[] qFixedByIndex = new double[transactions.size()];
		// NaN means "no q-period match" for that transaction index.
		for (int i = 0; i < qFixedByIndex.length; i++) {
			qFixedByIndex[i] = Double.NaN;
		}
		if (transactions.isEmpty() || periods.isEmpty()) {
			return qFixedByIndex;
		}

		List<IndexedQPeriod> qPeriods = new ArrayList<>(periods.size());
		for (int i = 0; i < periods.size(); i++) {
			QPeriodInput period = periods.get(i);
			if (period.start().isAfter(period.end())) {
				continue;
			}
			qPeriods.add(new IndexedQPeriod(i, period.fixed(), period.start(), period.end()));
		}
		if (qPeriods.isEmpty()) {
			return qFixedByIndex;
		}

		// Sweep-line preparation: process period starts/ends in chronological order.
		List<IndexedQPeriod> starts = new ArrayList<>(qPeriods);
		starts.sort(Comparator
				.comparing(IndexedQPeriod::start)
				.thenComparing(IndexedQPeriod::order));

		List<IndexedQPeriod> ends = new ArrayList<>(qPeriods);
		ends.sort(Comparator
				.comparing(IndexedQPeriod::end)
				.thenComparing(IndexedQPeriod::order));

		TreeSet<IndexedQPeriod> activeQPeriods = new TreeSet<>(Comparator
				.comparing(IndexedQPeriod::start, Comparator.reverseOrder())
				.thenComparing(IndexedQPeriod::order));

		List<IndexedTransaction> indexedTransactions = toIndexedTransactions(transactions);
		int startPointer = 0;
		int endPointer = 0;

		for (IndexedTransaction indexedTransaction : indexedTransactions) {
			LocalDateTime date = indexedTransaction.date();
			// start <= date means the range starts inclusively at start.
			while (startPointer < starts.size() && !starts.get(startPointer).start().isAfter(date)) {
				activeQPeriods.add(starts.get(startPointer));
				startPointer++;
			}
			// end < date removes only already-expired ranges, so end is inclusive.
			while (endPointer < ends.size() && ends.get(endPointer).end().isBefore(date)) {
				activeQPeriods.remove(ends.get(endPointer));
				endPointer++;
			}
			// Highest-priority q rule is latest start; tie breaks by input order.
			if (!activeQPeriods.isEmpty()) {
				qFixedByIndex[indexedTransaction.index()] = activeQPeriods.first().fixed();
			}
		}

		return qFixedByIndex;
	}

	private double[] computeApplicablePExtra(List<ExpenseInput> transactions, List<PPeriodInput> periods) {
		double[] pExtraByIndex = new double[transactions.size()];
		if (transactions.isEmpty() || periods.isEmpty()) {
			return pExtraByIndex;
		}

		List<IndexedPPeriod> pPeriods = new ArrayList<>(periods.size());
		for (int i = 0; i < periods.size(); i++) {
			PPeriodInput period = periods.get(i);
			if (period.start().isAfter(period.end())) {
				continue;
			}
			pPeriods.add(new IndexedPPeriod(i, period.extra(), period.start(), period.end()));
		}
		if (pPeriods.isEmpty()) {
			return pExtraByIndex;
		}

		// Sweep-line over p periods while maintaining running extra amount.
		List<IndexedPPeriod> starts = new ArrayList<>(pPeriods);
		starts.sort(Comparator
				.comparing(IndexedPPeriod::start)
				.thenComparing(IndexedPPeriod::order));

		List<IndexedPPeriod> ends = new ArrayList<>(pPeriods);
		ends.sort(Comparator
				.comparing(IndexedPPeriod::end)
				.thenComparing(IndexedPPeriod::order));

		List<IndexedTransaction> indexedTransactions = toIndexedTransactions(transactions);
		int startPointer = 0;
		int endPointer = 0;
		double activeExtra = 0.0;

		for (IndexedTransaction indexedTransaction : indexedTransactions) {
			LocalDateTime date = indexedTransaction.date();
			// Add all p-period extras that are active at this timestamp.
			while (startPointer < starts.size() && !starts.get(startPointer).start().isAfter(date)) {
				activeExtra += starts.get(startPointer).extra();
				startPointer++;
			}
			// Remove extras for periods that ended before this timestamp.
			while (endPointer < ends.size() && ends.get(endPointer).end().isBefore(date)) {
				activeExtra -= ends.get(endPointer).extra();
				endPointer++;
			}
			// All active p periods contribute cumulatively.
			pExtraByIndex[indexedTransaction.index()] = activeExtra;
		}

		return pExtraByIndex;
	}

	private boolean[] computeKMembership(List<TransactionOutput> transactions, List<KPeriodInput> kPeriods) {
		boolean[] inKPeriodByIndex = new boolean[transactions.size()];
		if (transactions.isEmpty()) {
			return inKPeriodByIndex;
		}

		// Mark whether each transaction falls in at least one k period.
		List<IndexedProcessedTransaction> sortedTransactions = toIndexedProcessedTransactions(transactions);
		markKMembership(inKPeriodByIndex, sortedTransactions, kPeriods);
		return inKPeriodByIndex;
	}

	private void markKMembership(
			boolean[] inKPeriodByIndex,
			List<IndexedProcessedTransaction> sortedTransactions,
			List<KPeriodInput> kPeriods
	) {
		if (kPeriods.isEmpty()) {
			return;
		}

		List<IndexedKPeriod> starts = new ArrayList<>(kPeriods.size());
		List<IndexedKPeriod> ends = new ArrayList<>(kPeriods.size());
		for (int i = 0; i < kPeriods.size(); i++) {
			KPeriodInput period = kPeriods.get(i);
			if (period.start().isAfter(period.end())) {
				continue;
			}
			IndexedKPeriod indexedPeriod = new IndexedKPeriod(i, period.start(), period.end());
			starts.add(indexedPeriod);
			ends.add(indexedPeriod);
		}
		if (starts.isEmpty()) {
			return;
		}

		starts.sort(Comparator
				.comparing(IndexedKPeriod::start)
				.thenComparing(IndexedKPeriod::order));
		ends.sort(Comparator
				.comparing(IndexedKPeriod::end)
				.thenComparing(IndexedKPeriod::order));

		int activeCount = 0;
		int startPointer = 0;
		int endPointer = 0;

		for (IndexedProcessedTransaction transaction : sortedTransactions) {
			LocalDateTime date = transaction.date();
			while (startPointer < starts.size() && !starts.get(startPointer).start().isAfter(date)) {
				activeCount++;
				startPointer++;
			}
			while (endPointer < ends.size() && ends.get(endPointer).end().isBefore(date)) {
				activeCount--;
				endPointer++;
			}
			// Any active k range marks this transaction as inKPeriod.
			inKPeriodByIndex[transaction.index()] = activeCount > 0;
		}
	}

	private List<IndexedTransaction> toIndexedTransactions(List<ExpenseInput> transactions) {
		List<IndexedTransaction> indexedTransactions = new ArrayList<>(transactions.size());
		for (int i = 0; i < transactions.size(); i++) {
			indexedTransactions.add(new IndexedTransaction(i, transactions.get(i).getDate()));
		}
		indexedTransactions.sort(Comparator
				.comparing(IndexedTransaction::date)
				.thenComparing(IndexedTransaction::index));
		return indexedTransactions;
	}

	private List<IndexedProcessedTransaction> toIndexedProcessedTransactions(List<TransactionOutput> transactions) {
		List<IndexedProcessedTransaction> indexedTransactions = new ArrayList<>(transactions.size());
		for (int i = 0; i < transactions.size(); i++) {
			TransactionOutput transaction = transactions.get(i);
			indexedTransactions.add(new IndexedProcessedTransaction(i, transaction.getDate(), transaction.getRemanent()));
		}
		indexedTransactions.sort(Comparator
				.comparing(IndexedProcessedTransaction::date)
				.thenComparing(IndexedProcessedTransaction::index));
		return indexedTransactions;
	}

	private record IndexedTransaction(int index, LocalDateTime date) {}

	private record IndexedProcessedTransaction(int index, LocalDateTime date, double remanent) {}

	private record IndexedQPeriod(int order, double fixed, LocalDateTime start, LocalDateTime end) {}

	private record IndexedPPeriod(int order, double extra, LocalDateTime start, LocalDateTime end) {}

	private record IndexedKPeriod(int order, LocalDateTime start, LocalDateTime end) {}
}
