package com.banking.service;

import com.banking.exception.InsufficientFundsException;
import com.banking.exception.SelfTransferException;
import com.banking.model.dto.request.TransferRequest;
import com.banking.model.entity.Account;
import com.banking.model.entity.Transaction;
import com.banking.model.entity.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @InjectMocks private TransactionService transactionService;

    private User alice, bob;
    private Account aliceAccount, bobAccount;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(1L).email("alice@test.com").fullName("Alice").build();
        bob   = User.builder().id(2L).email("bob@test.com").fullName("Bob").build();

        aliceAccount = Account.builder()
                .id(1L).accountNumber("ACC100000000001")
                .balance(new BigDecimal("1000.00")).active(true).owner(alice).build();

        bobAccount = Account.builder()
                .id(2L).accountNumber("ACC100000000002")
                .balance(new BigDecimal("500.00")).active(true).owner(bob).build();
    }

    @Test
    @DisplayName("Successful transfer deducts source and credits destination")
    void transfer_success() {
        when(accountRepository.findByAccountNumberWithLock("ACC100000000001"))
                .thenReturn(Optional.of(aliceAccount));
        when(accountRepository.findByAccountNumberWithLock("ACC100000000002"))
                .thenReturn(Optional.of(bobAccount));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransferRequest req = new TransferRequest();
        req.setSourceAccountNumber("ACC100000000001");
        req.setDestinationAccountNumber("ACC100000000002");
        req.setAmount(new BigDecimal("200.00"));

        transactionService.transfer(req, "alice@test.com");

        assertThat(aliceAccount.getBalance()).isEqualByComparingTo("800.00");
        assertThat(bobAccount.getBalance()).isEqualByComparingTo("700.00");
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    @DisplayName("Transfer fails with 402 when balance is insufficient")
    void transfer_insufficientFunds_throws() {
        when(accountRepository.findByAccountNumberWithLock("ACC100000000001"))
                .thenReturn(Optional.of(aliceAccount));
        when(accountRepository.findByAccountNumberWithLock("ACC100000000002"))
                .thenReturn(Optional.of(bobAccount));

        TransferRequest req = new TransferRequest();
        req.setSourceAccountNumber("ACC100000000001");
        req.setDestinationAccountNumber("ACC100000000002");
        req.setAmount(new BigDecimal("9999.00")); // More than balance

        assertThatThrownBy(() -> transactionService.transfer(req, "alice@test.com"))
                .isInstanceOf(InsufficientFundsException.class);

        // Critical: no money should move if the exception is thrown
        assertThat(aliceAccount.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(bobAccount.getBalance()).isEqualByComparingTo("500.00");
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Self-transfer is rejected with 400")
    void transfer_selfTransfer_throws() {
        TransferRequest req = new TransferRequest();
        req.setSourceAccountNumber("ACC100000000001");
        req.setDestinationAccountNumber("ACC100000000001");
        req.setAmount(new BigDecimal("100.00"));

        assertThatThrownBy(() -> transactionService.transfer(req, "alice@test.com"))
                .isInstanceOf(SelfTransferException.class);

        verifyNoInteractions(accountRepository);
    }
}
