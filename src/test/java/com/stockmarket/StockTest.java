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
    @DisplayName("Initialization and Getters")
    void testInitializationAndCapitalization() {
        Stock stock = new Stock("goog", "Google Inc.", 1200.0);
        assertEquals("GOOG", stock.getSymbol());
        assertEquals("Google Inc.", stock.getName());
    }

    @Test
    @DisplayName("Set new price")
    void testSetPrice() {
        Stock stock = new Stock("MSFT", "Microsoft", 100.0);
        stock.setPrice(150.5);
        assertEquals(150.5, stock.getPrice());
    }

    // Symbol Validation Tests
    @ParameterizedTest()
    @DisplayName("Constructor: exception for ALL invalid Symbols")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "ABCDEF", "GGLONG"}) 
    void testInvalidSymbolValidation(String invalidSymbol) {
        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            new Stock(invalidSymbol, "Valid Name", 1.0);
        });
    }

    // Name Validation Tests
    @ParameterizedTest()
    @DisplayName("Constructor: exception for ALL invalid Names")
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
    void testNegativePriceValidation(double negativePrice) {
        // Constructor test
        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            new Stock("TEST", "Name", negativePrice);
        });
    }
    

    // Equals Tests
    @Test
    @DisplayName("Equals test")
    void testEqualsContractCompliance() {
        Stock stock1 = new Stock("TSLA", "Tesla Motors", 300.0);
        Stock stock2 = new Stock("tsla", "Tesla Inc.", 350.0);
        Stock stock3 = new Stock("GOOG", "Google", 1000.0);
        Object nonStockObject = "Not a stock";

        //Test self-reference (this == o)
        assertTrue(stock1.equals(stock1), "Equals must be true for self-reference.");

        //Test null and different type (covers internal if logic)
        assertFalse(stock1.equals(null), "Equals must be false for null.");
        assertFalse(stock1.equals(nonStockObject), "Equals must be false for different object type.");

        //Test true equality (Core Logic: Symbols match)
        assertTrue(stock1.equals(stock2), "Equals must be true for objects with the same symbol.");

        //Test false equality (Core Logic: Symbols differ)
        assertFalse(stock1.equals(stock3), "Equals must be false for objects with different symbols.");
    }

    // HashCode Tests
    @Test
    @DisplayName("HashCode Test")
    void testHashCodeContractCompliance() {
        Stock stock1 = new Stock("TSLA", "Tesla Motors", 300.0);
        Stock stock2 = new Stock("tsla", "Tesla Inc.", 350.0); 
        Stock stock3 = new Stock("GOOG", "Google", 1000.0);

        assertEquals(stock1.hashCode(), stock2.hashCode());
        
        assertNotEquals(stock1.hashCode(), stock3.hashCode());
    }
}