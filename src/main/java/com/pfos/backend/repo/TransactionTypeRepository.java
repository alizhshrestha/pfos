package com.pfos.backend.repo;

import com.pfos.backend.dto.TransactionType;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionTypeRepository
        extends MongoRepository<TransactionType, String> {
}