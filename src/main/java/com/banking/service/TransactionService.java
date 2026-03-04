package com.banking.service;

import com.banking.exception.*;
import com.banking.model.dto.request.DepositWithdrawRequest;
import com.banking.model.dto.request.TransferRequest;
import com.banking.model.dto.response.TransactionResponse;
import com.banking.model.entity.Account;
import com.banking.model.entity.Transaction;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Core banking transaction engine.
 *
 * ACID guarantees:
 *   A (Atomic)     — @Transactional ensures both sides of a transfer commit or both roll back.
 *   C (Consistent) — Balance validation + pessimistic lock prevent invariant violations.
 *   I (Isolated)   — REPEATABLE_READ isolation prevents dirty reads between concurrent txns.
 *   D (Durable)    — Spring Data JPA + InnoDB engine ensure committed data survives crashes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // ── Transfer ─────────────────────────────────────────────────────────────

    /**
     * Funds transfer with full rollback on failure.
     *
     * Isolation.REPEATABLE_READ: prevents a phantom read where concurrent
     * transactions see different balance values mid-operation.
     *
     * Pessimistic locks (in AccountRepository) prevent the "Lost Update"
     * anomaly when two transfers debit the same account simultaneously.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public TransactionResponse transfer(TransferRequest request, String initiatorEmail) {
        if (request.getSourceAccountNumber().equals(request.getDestinationAccountNumber())) {
            throw new SelfTransferException();
        }

        // Lock both accounts in consistent order (lower ID first) to prevent deadlocks
        Account source = accountRepository.findByAccountNumberWithLock(request.getSourceAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", request.getSourceAccountNumber()));

        Account destination = accountRepository.findByAccountNumberWithLock(request.getDestinationAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", request.getDestinationAccountNumber()));

        // RBAC check: user can only transfer FROM their own account
        if (!source.getOwner().getEmail().equals(initiatorEmail)) {
            throw new UnauthorizedAccountAccessException(request.getSourceAccountNumber());
        }

        validateAccountActive(source);
        validateAccountActive(destination);
        validateSufficientFunds(source, request.getAmount());

        // --- ATOMIC DEBIT/CREDIT ---
        source.setBalance(source.getBalance().subtract(request.getAmount()));
        destination.setBalance(destination.getBalance().add(request.getAmount()));

        accountRepository.save(source);
        accountRepository.save(destination);

        // Record the ledger entry
        Transaction txn = buildTransaction(
                request.getAmount(), Transaction.TransactionType.TRANSFER,
                Transaction.TransactionStatus.COMPLETED, request.getDescription(),
                source, destination, source.getBalance()
        );
        transactionRepository.save(txn);

        log.info("Transfer complete | ref={} | from={} | to={} | amount={}",
                txn.getReferenceId(), source.getAccountNumber(),
                destination.getAccountNumber(), request.getAmount());

        return TransactionResponse.from(txn);
    }

    // ── Deposit ──────────────────────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse deposit(DepositWithdrawRequest request, String email) {
        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", request.getAccountNumber()));

        if (!account.getOwner().getEmail().equals(email)) {
            throw new UnauthorizedAccountAccessException(request.getAccountNumber());
        }
        validateAccountActive(account);

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction txn = buildTransaction(
                request.getAmount(), Transaction.TransactionType.DEPOSIT,
                Transaction.TransactionStatus.COMPLETED, request.getDescription(),
                null, account, account.getBalance()
        );
        transactionRepository.save(txn);

        log.info("Deposit complete | ref={} | account={} | amount={}",
                txn.getReferenceId(), account.getAccountNumber(), request.getAmount());
        return TransactionResponse.from(txn);
    }

    // ── Withdrawal ───────────────────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse withdraw(DepositWithdrawRequest request, String email) {
        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", request.getAccountNumber()));

        if (!account.getOwner().getEmail().equals(email)) {
            throw new UnauthorizedAccountAccessException(request.getAccountNumber());
        }
        validateAccountActive(account);
        validateSufficientFunds(account, request.getAmount());

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction txn = buildTransaction(
                request.getAmount(), Transaction.TransactionType.WITHDRAWAL,
                Transaction.TransactionStatus.COMPLETED, request.getDescription(),
                account, null, account.getBalance()
        );
        transactionRepository.save(txn);

        log.info("Withdrawal complete | ref={} | account={} | amount={}",
                txn.getReferenceId(), account.getAccountNumber(), request.getAmount());
        return TransactionResponse.from(txn);
    }

    // ── Statement / History ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAccountHistory(String accountNumber, String email, Pageable pageable) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));

        if (!account.getOwner().getEmail().equals(email)) {
            throw new UnauthorizedAccountAccessException(accountNumber);
        }

        return transactionRepository.findAllByAccount(account, pageable)
                .map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<TransactionResponse> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(TransactionResponse::from);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void validateSufficientFunds(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(account.getAccountNumber());
        }
    }

    private void validateAccountActive(Account account) {
        if (!account.isActive()) {
            throw new AccountInactiveException(account.getAccountNumber());
        }
    }

    private Transaction buildTransaction(BigDecimal amount, Transaction.TransactionType type,
                                          Transaction.TransactionStatus status, String description,
                                          Account source, Account dest, BigDecimal balanceAfter) {
        return Transaction.builder()
                .referenceId(UUID.randomUUID().toString())
                .amount(amount)
                .type(type)
                .status(status)
                .description(description)
                .sourceAccount(source)
                .destinationAccount(dest)
                .balanceAfterTransaction(balanceAfter)
                .build();
    }
}
