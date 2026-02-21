package com.example.retirementsavings.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransactionOutput extends ExpenseInput {

	@NotNull
	private Double ceiling;

	@NotNull
	private Double remanent;

	private Boolean inKPeriod;

	public TransactionOutput(LocalDateTime date, Double amount, Double ceiling, Double remanent) {
		this(date, amount, ceiling, remanent, null);
	}

	public TransactionOutput(LocalDateTime date, Double amount, Double ceiling, Double remanent, Boolean inKPeriod) {
		super(date, amount);
		this.ceiling = ceiling;
		this.remanent = remanent;
		this.inKPeriod = inKPeriod;
	}
}
