package com.banking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// ── Base ──────────────────────────────────────────────────────────────────────

class BankingException extends RuntimeException {
    public BankingException(String message) { super(message); }
}

// ── Domain Exceptions ─────────────────────────────────────────────────────────

@ResponseStatus(HttpStatus.NOT_FOUND)
class ResourceNotFoundException extends BankingException {
    public ResourceNotFoundException(String resource, String identifier) {
        super(resource + " not found: " + identifier);
    }
}

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
class InsufficientFundsException extends BankingException {
    public InsufficientFundsException(String accountNumber) {
        super("Insufficient funds in account: " + accountNumber);
    }
}

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateResourceException extends BankingException {
    public DuplicateResourceException(String message) { super(message); }
}

@ResponseStatus(HttpStatus.FORBIDDEN)
class UnauthorizedAccountAccessException extends BankingException {
    public UnauthorizedAccountAccessException(String accountNumber) {
        super("You are not authorized to access account: " + accountNumber);
    }
}

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
class AccountInactiveException extends BankingException {
    public AccountInactiveException(String accountNumber) {
        super("Account is inactive: " + accountNumber);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class SelfTransferException extends BankingException {
    public SelfTransferException() {
        super("Source and destination accounts cannot be the same");
    }
}
