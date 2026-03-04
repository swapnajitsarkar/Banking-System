package com.banking.service;

import com.banking.exception.AccountInactiveException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.exception.UnauthorizedAccountAccessException;
import com.banking.model.dto.request.CreateAccountRequest;
import com.banking.model.dto.response.AccountResponse;
import com.banking.model.entity.Account;
import com.banking.model.entity.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import com.banking.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    @Transactional
    public AccountResponse createAccount(String email, CreateAccountRequest request) {
        User user = findUser(email);
        String accountNumber = accountNumberGenerator.generate();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountType(request.getAccountType())
                .owner(user)
                .build();

        account = accountRepository.save(account);
        log.info("Account {} created for user {}", accountNumber, email);
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(String email) {
        User user = findUser(email);
        return accountRepository.findByOwner(user).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber, String email) {
        Account account = findAndVerifyOwnership(accountNumber, email);
        return AccountResponse.from(account);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivateAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));
        account.setActive(false);
        accountRepository.save(account);
        log.info("Account {} deactivated by admin", accountNumber);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public Account findAndVerifyOwnership(String accountNumber, String email) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));
        if (!account.getOwner().getEmail().equals(email)) {
            throw new UnauthorizedAccountAccessException(accountNumber);
        }
        if (!account.isActive()) {
            throw new AccountInactiveException(accountNumber);
        }
        return account;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }
}
