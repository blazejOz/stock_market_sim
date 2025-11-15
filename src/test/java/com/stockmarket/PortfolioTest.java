package com.stockmarket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PortfolioTest {

    private Portfolio portfolio;
    private Stock stockA;
    private Stock stockB;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio(1000.0);
        stockA = new Stock("AAPL", "Apple Inc.", 150.0);
        stockB = new Stock("GOOG", "Google LLC", 2000.0);
    }

    @Test
    @DisplayName("Should initialize portfolio with correct initial cash")
    void testPortfolioInitialization() {
        assertEquals(1000.0, portfolio.getCash());
    }

    @Test
    @DisplayName("Should add stock to portfolio")
    void testAddStock() {
        portfolio.addStock(stockA, 5);
        assertEquals(5, portfolio.getStockQuantity(stockA));
    }

    @Test
    @DisplayName("Should increase quantity if same stock is added again")
    void testAddExistingStock() {
        portfolio.addStock(stockA, 5);
        portfolio.addStock(stockA, 3);
        assertEquals(8, portfolio.getStockQuantity(stockA));
    }

    @Test
    @DisplayName("Should calculate total value correctly")
    void testCalculateTotalValue() {
        portfolio.addStock(stockA, 2); // 2 * 150 = 300
        portfolio.addStock(stockB, 1); // 1 * 2000 = 2000
        double expectedTotalValue = 1000.0 + 300.0 + 2000.0; // cash + stocks value
        assertEquals(expectedTotalValue, portfolio.calculateTotalValue());
    }
}