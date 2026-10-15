package com.pfos.backend.controller;

import com.pfos.backend.dto.response.ApiResponse;
import com.pfos.backend.dto.response.CategoryResponse;
import com.pfos.backend.dto.response.PaymentMethodResponse;
import com.pfos.backend.dto.response.TransactionTypeResponse;
import com.pfos.backend.service.MasterDataService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MasterDataController {

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> getCategories() {
        return ApiResponse.ok(masterDataService.getCategories());
    }

    @GetMapping("/transaction-types")
    public ApiResponse<List<TransactionTypeResponse>> getTransactionTypes() {
        return ApiResponse.ok(masterDataService.getTransactionTypes());
    }

    @GetMapping("/payment-methods")
    public ApiResponse<List<PaymentMethodResponse>> getPaymentMethods() {
        return ApiResponse.ok(masterDataService.getPaymentMethods());
    }
}