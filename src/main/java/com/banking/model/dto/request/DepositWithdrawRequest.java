package com.banking.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositWithdrawRequest {

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[A-Z0-9]{10,20}$", message = "Invalid account number format")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 10, fraction = 4)
    private BigDecimal amount;

    @Size(max = 255)
    private String description;
}
