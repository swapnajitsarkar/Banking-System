package com.banking.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "Source account number is required")
    @Pattern(regexp = "^[A-Z0-9]{10,20}$", message = "Invalid account number format")
    private String sourceAccountNumber;

    @NotBlank(message = "Destination account number is required")
    @Pattern(regexp = "^[A-Z0-9]{10,20}$", message = "Invalid account number format")
    private String destinationAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero")
    @DecimalMax(value = "999999.9999", message = "Transfer amount exceeds maximum limit")
    @Digits(integer = 10, fraction = 4, message = "Invalid monetary amount format")
    private BigDecimal amount;

    @Size(max = 255)
    private String description;
}
