package com.banking.util;

import com.banking.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates unique, cryptographically random account numbers.
 * Format: "ACC" + 12 digits  →  e.g., ACC482910374821
 */
@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final AccountRepository accountRepository;

    public String generate() {
        String accountNumber;
        do {
            accountNumber = "ACC" + String.format("%012d", Math.abs(RANDOM.nextLong() % 1_000_000_000_000L));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
