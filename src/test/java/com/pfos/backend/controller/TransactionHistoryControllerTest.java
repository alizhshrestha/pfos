package com.pfos.backend.controller;

import com.pfos.backend.dto.Transaction;
import com.pfos.backend.exception.GlobalExceptionHandler;
import com.pfos.backend.repo.*;
import com.pfos.backend.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockMakers;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Standalone HTTP contract tests: real controller/service/advice, mocked persistence. */
class TransactionHistoryControllerTest {
    private MockMvc mvc;
    private MongoTemplate mongo;

    private <T> T stub(Class<T> type) {
        return mock(type, withSettings().mockMaker(MockMakers.SUBCLASS));
    }

    @BeforeEach
    void setUp() {
        mongo = stub(MongoTemplate.class);
        when(mongo.find(any(Query.class), eq(Transaction.class))).thenReturn(List.of());
        TransactionService service = new TransactionService(stub(TransactionRepository.class),
                stub(CategoryRepository.class), stub(TransactionTypeRepository.class),
                stub(PaymentMethodRepository.class), mongo);
        mvc = MockMvcBuilders.standaloneSetup(new TransactionController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void defaultsToFirstPageOf20WithStandardEnvelope() throws Exception {
        mvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0));
    }

    @Test
    void bindsAllQueryParametersIncludingLiteralSearch() throws Exception {
        mvc.perform(get("/api/transactions")
                        .param("page", "2").param("size", "10")
                        .param("from", "2026-10-01").param("to", "2026-10-31")
                        .param("categoryId", "cat_food").param("transactionTypeId", "type_debit")
                        .param("paymentMethodId", "method_cash").param("search", "milk & bread + coffee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(10));
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(captor.capture(), eq(Transaction.class));
        Query query = captor.getValue();
        assertEquals(20, query.getSkip());
        assertEquals("cat_food", query.getQueryObject().get("categoryId"));
        assertEquals("type_debit", query.getQueryObject().get("transactionTypeId"));
        assertEquals("method_cash", query.getQueryObject().get("paymentMethodId"));
        Pattern search = (Pattern) query.getQueryObject().get("description");
        assertTrue(search.matcher("Milk & bread + coffee").find());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page", "size"})
    void nonIntegerPaginationReturnsInvalidRequest(String parameter) throws Exception {
        mvc.perform(get("/api/transactions").param(parameter, "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        verifyNoInteractions(mongo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"from", "to"})
    void invalidDateReturnsInvalidRequest(String parameter) throws Exception {
        mvc.perform(get("/api/transactions").param(parameter, "2026-02-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        verifyNoInteractions(mongo);
    }

    @Test
    void reversedDatesReturnValidationError() throws Exception {
        mvc.perform(get("/api/transactions").param("from", "2026-10-31").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("from: From date must be on or before to date."));
        verifyNoInteractions(mongo);
    }

    @Test
    void excessiveSearchReturnsValidationError() throws Exception {
        mvc.perform(get("/api/transactions").param("search", "x".repeat(501)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(mongo);
    }

    @Test
    void invalidPageSizeReturnsValidationError() throws Exception {
        mvc.perform(get("/api/transactions").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(mongo);
    }
}
