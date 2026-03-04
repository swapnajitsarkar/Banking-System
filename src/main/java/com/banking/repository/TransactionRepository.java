package com.banking.repository;

import com.banking.model.entity.Account;
import com.banking.model.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceId(String referenceId);

    /**
     * Fetches all transactions (incoming OR outgoing) for a given account,
     * sorted by creation date descending — used for account statement generation.
     */
    @Query("SELECT t FROM Transaction t WHERE t.sourceAccount = :account OR t.destinationAccount = :account ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByAccount(Account account, Pageable pageable);

    // Admin: view all transactions across the system
    Page<Transaction> findAll(Pageable pageable);
}
