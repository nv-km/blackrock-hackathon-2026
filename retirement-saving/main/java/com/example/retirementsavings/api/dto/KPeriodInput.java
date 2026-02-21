package com.example.retirementsavings.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record KPeriodInput(
		@NotNull
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
		LocalDateTime start,
		@NotNull
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
		LocalDateTime end
) {}
