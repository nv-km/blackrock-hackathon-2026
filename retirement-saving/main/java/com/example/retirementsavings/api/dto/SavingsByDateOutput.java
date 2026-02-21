package com.example.retirementsavings.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record SavingsByDateOutput(
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
		LocalDateTime start,
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
		LocalDateTime end,
		double amount,
		double profit,
		double taxBenefit
) {}
