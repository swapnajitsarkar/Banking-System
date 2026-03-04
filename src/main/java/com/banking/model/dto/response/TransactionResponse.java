package com.banking.model.dto.response;

import com.banking.model.entity.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private String referenceId;
    private BigDecimal amount;
    private Transaction.TransactionType type;
    private Transaction.TransactionStatus status;
    private String description;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal balanceAfterTransaction;
    private LocalDateTime createdAt;

    public static TransactionResponse from(Transaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .referenceId(txn.getReferenceId())
                .amount(txn.getAmount())
                .type(txn.getType())
                .status(txn.getStatus())
                .description(txn.getDescription())
                .sourceAccountNumber(txn.getSourceAccount() != null
                        ? txn.getSourceAccount().getAccountNumber() : null)
                .destinationAccountNumber(txn.getDestinationAccount() != null
                        ? txn.getDestinationAccount().getAccountNumber() : null)
                .balanceAfterTransaction(txn.getBalanceAfterTransaction())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
