package com.example.retirementsavings.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class InvalidTransactionOutput extends TransactionOutput {

	private String message;

	public InvalidTransactionOutput(
			LocalDateTime date,
			Double amount,
			Double ceiling,
			Double remanent,
			String message
	) {
		super(date, amount, ceiling, remanent, null);
		this.message = message;
	}

	public InvalidTransactionOutput(LocalDateTime date, Double amount, String message) {
		super(date, amount, null, null, null);
		this.message = message;
	}
}
