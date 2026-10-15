package com.pfos.backend.repo;

import com.pfos.backend.dto.PaymentMethod;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentMethodRepository
        extends MongoRepository<PaymentMethod, String> {

    List<PaymentMethod> findByIsActiveTrueOrderByNameAsc();
}