package br.com.stella.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ItemLoanResponseDTO(
        UUID id,
        UUID itemInstanceId,
        String instanceIdentification,
        UUID personId,
        String personName,
        Instant loanDate,
        LocalDate expectedReturnDate,
        Instant returnDate,
        String notes
) {}
