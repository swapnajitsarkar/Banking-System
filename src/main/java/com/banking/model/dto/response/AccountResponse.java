package com.banking.model.dto.response;

import com.banking.model.entity.Account;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private Account.AccountType accountType;
    private BigDecimal balance;
    private boolean active;
    private String ownerName;
    private LocalDateTime createdAt;

    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .active(account.isActive())
                .ownerName(account.getOwner().getFullName())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
