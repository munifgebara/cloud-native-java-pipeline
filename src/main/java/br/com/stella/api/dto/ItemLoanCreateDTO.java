package br.com.stella.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ItemLoanCreateDTO(
        @NotNull(message = "Instance is required.")
        UUID itemInstanceId,

        @NotNull(message = "Person is required.")
        UUID personId,

        LocalDate expectedReturnDate,

        @Size(max = 1000, message = "Observation must not exceed 1000 characters.")
        String notes
) {}
