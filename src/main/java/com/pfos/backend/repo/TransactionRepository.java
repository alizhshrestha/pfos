package com.pfos.backend.repo;

import com.pfos.backend.dto.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TransactionRepository
        extends MongoRepository<Transaction, String> {

    Optional<Transaction> findByIdAndDeletedAtIsNull(String id);

    Page<Transaction> findByDeletedAtIsNull(Pageable pageable);
}