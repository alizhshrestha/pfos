package com.pfos.backend.repo;

import com.pfos.backend.dto.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionRepository
        extends MongoRepository<Transaction, String> {
}