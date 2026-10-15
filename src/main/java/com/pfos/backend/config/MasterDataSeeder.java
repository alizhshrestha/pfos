package com.pfos.backend.config;


import com.pfos.backend.dto.Category;
import com.pfos.backend.dto.PaymentMethod;
import com.pfos.backend.dto.TransactionType;
import com.pfos.backend.repo.CategoryRepository;
import com.pfos.backend.repo.PaymentMethodRepository;
import com.pfos.backend.repo.TransactionTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class MasterDataSeeder {

    private static final Logger log =
            LoggerFactory.getLogger(MasterDataSeeder.class);

    @Bean
    CommandLineRunner seedMasterData(
            CategoryRepository categories,
            TransactionTypeRepository transactionTypes,
            PaymentMethodRepository paymentMethods) {

        return args -> {
            seedCategory(categories, "cat_food",
                    "Food & Groceries", 1);
            seedCategory(categories, "cat_health",
                    "Health", 2);
            seedCategory(categories, "cat_household",
                    "Household", 3);
            seedCategory(categories, "cat_income",
                    "Income", 4);
            seedCategory(categories, "cat_debt",
                    "Loan / Debt", 5);
            seedCategory(categories, "cat_shopping",
                    "Shopping", 6);
            seedCategory(categories, "cat_transfer",
                    "Transfer", 7);
            seedCategory(categories, "cat_transportation",
                    "Transportation", 8);
            seedCategory(categories, "cat_utilities",
                    "Utilities", 9);
            seedCategory(categories, "cat_work",
                    "Work / Project", 10);
            seedCategory(categories, "cat_personal_care",
                    "Personal Care", 11);
            seedCategory(categories, "cat_entertainment",
                    "Entertainment", 12);
            seedCategory(categories, "cat_donation",
                    "Donation", 13);

            seedTransactionType(transactionTypes, "type_debit", "Debit");
            seedTransactionType(transactionTypes, "type_credit", "Credit");

            seedPaymentMethod(paymentMethods, "method_cash", "Cash");
            seedPaymentMethod(paymentMethods, "method_bank", "Bank");
            seedPaymentMethod(paymentMethods, "method_esewa", "eSewa");
            seedPaymentMethod(paymentMethods, "method_connectips", "ConnectIPS");

            log.info(
                    "Master data ready: {} categories, {} transaction types, "
                            + "{} payment methods",
                    categories.count(),
                    transactionTypes.count(),
                    paymentMethods.count()
            );
        };
    }

    private void seedCategory(
            CategoryRepository repository,
            String id,
            String name,
            int sortOrder) {

        if (repository.existsById(id)) {
            return;
        }

        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSortOrder(sortOrder);
        category.setIsActive(true);

        repository.insert(category);
    }

    private void seedTransactionType(
            TransactionTypeRepository repository,
            String id,
            String name) {

        if (repository.existsById(id)) {
            return;
        }

        TransactionType type = new TransactionType();
        type.setId(id);
        type.setName(name);

        repository.insert(type);
    }

    private void seedPaymentMethod(
            PaymentMethodRepository repository,
            String id,
            String name) {

        if (repository.existsById(id)) {
            return;
        }

        PaymentMethod method = new PaymentMethod();
        method.setId(id);
        method.setName(name);
        method.setIsActive(true);

        repository.insert(method);
    }
}