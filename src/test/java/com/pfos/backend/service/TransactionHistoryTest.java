package com.pfos.backend.service;

import com.pfos.backend.dto.Transaction;
import com.pfos.backend.exception.BusinessValidationException;
import com.pfos.backend.exception.InvalidRequestException;
import com.pfos.backend.repo.*;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockMakers;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests of query construction and validation; no MongoDB connection. */
class TransactionHistoryTest {
    private MongoTemplate mongo;
    private TransactionService service;
    private CategoryRepository categories;
    private TransactionTypeRepository types;
    private PaymentMethodRepository methods;

    private <T> T stub(Class<T> type) {
        return mock(type, withSettings().mockMaker(MockMakers.SUBCLASS));
    }

    @BeforeEach
    void setUp() {
        mongo = stub(MongoTemplate.class);
        categories = stub(CategoryRepository.class);
        types = stub(TransactionTypeRepository.class);
        methods = stub(PaymentMethodRepository.class);
        service = new TransactionService(stub(TransactionRepository.class), categories, types, methods, mongo);
        when(mongo.find(any(Query.class), eq(Transaction.class))).thenReturn(List.of());
    }

    private Page<Transaction> history(String from, String to, String category, String type, String method, String search) {
        return service.getHistory(0, 20, from, to, category, type, method, search);
    }

    private Query findQuery() {
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(Transaction.class));
        return query.getValue();
    }

    @Test
    void unfilteredHistoryAlwaysExcludesSoftDeletedRecords() {
        history(null, null, null, null, null, null);
        assertEquals(new Document("deletedAt", null), findQuery().getQueryObject());
    }

    @Test
    void combinesEveryFilterWithAndAndUsesInclusiveDates() {
        history("2026-10-01", "2026-10-31", " cat_food ", " type_debit ", " method_cash ", " milk ");
        Document criteria = findQuery().getQueryObject();
        assertEquals(6, criteria.size());
        assertTrue(criteria.containsKey("deletedAt"));
        assertNull(criteria.get("deletedAt"));
        assertEquals(new Document("$gte", "2026-10-01").append("$lte", "2026-10-31"), criteria.get("transactionDate"));
        assertEquals("cat_food", criteria.get("categoryId"));
        assertEquals("type_debit", criteria.get("transactionTypeId"));
        assertEquals("method_cash", criteria.get("paymentMethodId"));
        assertTrue(((Pattern) criteria.get("description")).matcher("Bought MILK today").find());
    }

    @Test
    void supportsFromOnly() {
        history("2026-10-01", null, null, null, null, null);
        assertEquals(new Document("$gte", "2026-10-01"), findQuery().getQueryObject().get("transactionDate"));
    }

    @Test
    void supportsToOnly() {
        history(null, "2026-10-31", null, null, null, null);
        assertEquals(new Document("$lte", "2026-10-31"), findQuery().getQueryObject().get("transactionDate"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-02-29", "2020-01-01", "2030-12-31"})
    void acceptsSameDayLeapPastAndFutureDates(String date) {
        history(date, date, null, null, null, null);
        assertEquals(new Document("$gte", date).append("$lte", date), findQuery().getQueryObject().get("transactionDate"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "2026-02-29", "2026-02-30", "2026-13-01", "2026-10-32", "2026-1-01", "01-10-2026", " 2026-10-01", "2026-10-01 ", "2026-10-01T00:00:00"})
    void rejectsMalformedDatesForBothBoundsBeforeQuerying(String date) {
        assertThrows(InvalidRequestException.class, () -> history(date, null, null, null, null, null));
        assertThrows(InvalidRequestException.class, () -> history(null, date, null, null, null, null));
        verify(mongo, never()).count(any(Query.class), eq(Transaction.class));
        verify(mongo, never()).find(any(Query.class), eq(Transaction.class));
    }

    @Test
    void rejectsReversedRange() {
        BusinessValidationException error = assertThrows(BusinessValidationException.class,
                () -> history("2026-10-31", "2026-10-01", null, null, null, null));
        assertEquals("from", error.getField());
        verifyNoInteractions(mongo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "\t\n", "\u2003"})
    void ignoresBlankIdsAndSearch(String blank) {
        history(null, null, blank, blank, blank, blank);
        assertEquals(new Document("deletedAt", null), findQuery().getQueryObject());
    }

    @ParameterizedTest
    @ValueSource(strings = {"milk & bread", ".*", "[DEMO]", "a+b?", "(coffee)", "\\E", "^salary$"})
    void searchIsLiteralCaseInsensitiveSubstringAndOnlyTargetsDescription(String search) {
        history(null, null, null, null, null, "  " + search + "  ");
        Document criteria = findQuery().getQueryObject();
        Pattern pattern = (Pattern) criteria.get("description");
        assertTrue(pattern.matcher("prefix " + search.toUpperCase(java.util.Locale.ROOT) + " suffix").find());
        assertFalse(pattern.matcher("unrelated transaction").find());
        assertEquals(2, criteria.size()); // deletedAt + description; notes are not searched.
    }

    @Test
    void searchLengthLimitAppliesAfterTrimming() {
        history(null, null, null, null, null, "  " + "a".repeat(500) + "  ");
        assertNotNull(findQuery().getQueryObject().get("description"));
    }

    @Test
    void rejectsSearchOver500Characters() {
        BusinessValidationException error = assertThrows(BusinessValidationException.class,
                () -> history(null, null, null, null, null, "a".repeat(501)));
        assertEquals("search", error.getField());
        verifyNoInteractions(mongo);
    }

    @Test
    void unknownOrInactiveMasterIdsAreExactFiltersWithoutMasterLookups() {
        Page<Transaction> result = history(null, null, "inactive_category", "unknown_type", "old_method", null);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        assertEquals("inactive_category", findQuery().getQueryObject().get("categoryId"));
        verifyNoInteractions(categories, types, methods);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    void rejectsInvalidPageSizes(int size) {
        BusinessValidationException error = assertThrows(BusinessValidationException.class,
                () -> service.getHistory(0, size, null, null, null, null, null, null));
        assertEquals("size", error.getField());
        verifyNoInteractions(mongo);
    }

    @Test
    void rejectsNegativePage() {
        BusinessValidationException error = assertThrows(BusinessValidationException.class,
                () -> service.getHistory(-1, 20, null, null, null, null, null, null));
        assertEquals("page", error.getField());
        verifyNoInteractions(mongo);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void acceptsPageSizeBoundaries(int size) {
        assertEquals(size, service.getHistory(0, size, null, null, null, null, null, null).getSize());
    }

    @Test
    void countRemainsUnpagedAndFindUsesStableSortingAndRequestedOffset() {
        Transaction row = new Transaction();
        when(mongo.count(any(Query.class), eq(Transaction.class))).thenReturn(45L);
        when(mongo.find(any(Query.class), eq(Transaction.class))).thenReturn(List.of(row, row, row, row, row));
        Page<Transaction> result = service.getHistory(2, 20, null, null, "cat_food", null, null, "milk");
        ArgumentCaptor<Query> countCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongo).count(countCaptor.capture(), eq(Transaction.class));
        Query count = countCaptor.getValue();
        Query find = findQuery();
        assertEquals(0, count.getSkip());
        assertEquals(0, count.getLimit());
        assertTrue(count.getSortObject().isEmpty());
        assertEquals(count.getQueryObject(), find.getQueryObject());
        assertEquals(40, find.getSkip());
        assertEquals(20, find.getLimit());
        assertEquals(new Document("transactionDate", -1).append("createdAt", -1).append("id", -1), find.getSortObject());
        assertEquals(List.of(row, row, row, row, row), result.getContent());
        assertEquals(2, result.getNumber());
        assertEquals(45, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
    }

    @Test
    void pageBeyondLastIsEmptyButRetainsTotal() {
        when(mongo.count(any(Query.class), eq(Transaction.class))).thenReturn(3L);
        Page<Transaction> result = service.getHistory(5, 20, null, null, null, null, null, null);
        assertTrue(result.isEmpty());
        assertEquals(3, result.getTotalElements());
        assertEquals(5, result.getNumber());
    }
}
