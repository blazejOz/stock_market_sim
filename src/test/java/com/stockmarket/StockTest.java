package com.stockmarket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class StockTest {

    //Getters and Setters Tests
    @Test
    @DisplayName("Should initialize stock and use getters")
    void testInitializationAndCapitalization() {
        Stock stock = new Stock("goog", "Google Inc.", 1200.0);
        assertEquals("GOOG", stock.getSymbol());
        assertEquals("Google Inc.", stock.getName());
    }

    @Test
    @DisplayName("Should set new price correctly")
    void testSetPrice() {
        Stock stock = new Stock("MSFT", "Microsoft", 100.0);
        stock.setPrice(150.5);
        assertEquals(150.5, stock.getPrice());
    }

    // Symbol Validation Tests
    @ParameterizedTest()
    @DisplayName("Should throw exception for ALL invalid Symbols in constructor")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "ABCDEF", "GGLONG"}) 
    void testInvalidSymbolValidation(String invalidSymbol) {
        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            new Stock(invalidSymbol, "Valid Name", 1.0);
        });
    }

    // Name Validation Tests
    @ParameterizedTest()
    @DisplayName("Should throw exception for ALL invalid Names in constructor")
    @NullAndEmptySource
    @ValueSource(strings = {" "}) 
    void testInvalidNameValidation(String invalidName) {
        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            new Stock("TEST", invalidName, 1.0);
        });
    }

    // Price Validation Tests
    @ParameterizedTest()
    @DisplayName("Constructor: Throws exception for NEGATIVE Price")
    @ValueSource(doubles = {-0.01, -100.0})
    void testNegativePriceConstructor(double negativePrice) {
        // Constructor test
        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            new Stock("TEST", "Name", negativePrice);
        });
    }

    @ParameterizedTest()
    @DisplayName("SetPrice: Throws exception for NEGATIVE Price")
    @ValueSource(doubles = {-0.01, -100.0})
    void testNegativePriceSetter(double negativePrice) {
        Stock stock = new Stock("TEST", "Name", 100.0);
        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            stock.setPrice(negativePrice);
        });
    }

    // --- EQUALS TESTS ---    
    @Test
    @DisplayName("Should return true when comparing stock to itself")
    void testEqualsSelfReference() {
        Stock stock1 = new Stock("TSLA", "Tesla Motors", 300.0);
        assertTrue(stock1.equals(stock1));
    }

    @Test
    @DisplayName("Should return false when comparing stock to null or wrong type")
    void testEqualsNullAndType() {
        Stock stock1 = new Stock("TSLA", "Tesla Motors", 300.0);
        Object nonStockObject = "Not a stock";

        assertFalse(stock1.equals(null));
        assertFalse(stock1.equals(nonStockObject));
    }

    @Test
    @DisplayName("Should return true when Symbols match (case-insensitive due to constructor)")
    void testEqualsTrueMatchingSymbols() {
        Stock stock1 = new Stock("TSLA", "Tesla Motors", 300.0);
        Stock stock2 = new Stock("tsla", "Tesla Inc.", 350.0);
        assertTrue(stock1.equals(stock2));
    }

    @Test
    @DisplayName("Should return false when Symbols differ")
    void testEqualsFalseDifferentSymbols() {
        Stock stock1 = new Stock("TSLA", "Tesla Motors", 300.0);
        Stock stock3 = new Stock("GOOG", "Google", 1000.0);
        assertFalse(stock1.equals(stock3));
    }
    
    // --- HASHCODE TESTS ---
    @Test
    @DisplayName("Should return equal hashCodes for equal objects")
    void testHashCodeConsistencyForEqualObjects() {
        Stock stock1 = new Stock("TSLA", "Tesla Motors", 300.0);
        Stock stock2 = new Stock("tsla", "Tesla Inc.", 350.0); 
        assertEquals(stock1.hashCode(), stock2.hashCode());
    }

    @Test
    @DisplayName("Should return different hashCodes for unequal objects")
    void testHashCodeInequalityForUnequalObjects() {
        Stock stock1 = new Stock("TSLA", "Tesla Motors", 300.0);
        Stock stock3 = new Stock("GOOG", "Google", 1000.0);
        assertNotEquals(stock1.hashCode(), stock3.hashCode());
    }
}