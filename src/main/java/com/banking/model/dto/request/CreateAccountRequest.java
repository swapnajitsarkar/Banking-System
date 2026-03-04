package com.banking.model.dto.request;

import com.banking.model.entity.Account;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {

    @NotNull(message = "Account type is required")
    private Account.AccountType accountType;
}
