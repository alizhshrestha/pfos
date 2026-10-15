package com.pfos.backend.repo;


import com.pfos.backend.dto.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CategoryRepository
        extends MongoRepository<Category, String> {

    List<Category> findByIsActiveTrueOrderBySortOrderAsc();
}