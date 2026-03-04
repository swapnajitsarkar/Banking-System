package com.banking.repository;

import com.banking.model.entity.Account;
import com.banking.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByOwner(User owner);

    /**
     * Pessimistic write lock: prevents concurrent transactions from reading
     * (and modifying) the same account row simultaneously. Critical for
     * preventing race conditions on balance updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
}
