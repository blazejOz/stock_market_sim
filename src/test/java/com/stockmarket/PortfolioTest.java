package com.stockmarket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PortfolioTest {

    private Portfolio portfolio;
    private Stock stockA;
    private Stock stockB;
    private Stock stockC;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio(1000.0);
        stockA = new Stock("AAPL", "Apple Inc.", 150.0);
        stockB = new Stock("GOOG", "Google LLC", 2000.0);
        stockC = new Stock("TSLA", "Tesla Inc.", 800.0);
       
    }

    // --- Initialization ---
    @Test
    @DisplayName("Should initialize with correct cash and zero holdings")
    void testPortfolioInitialization() {
        assertEquals(1000.0, portfolio.getCash());
        assertEquals(0, portfolio.getHoldingsCount());
        assertEquals(0.0, portfolio.calculateStockValue());
    }

    @Test
    @DisplayName("Constructor should throw exception for negative initial cash")
    void testNegativeInitialCashThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Portfolio(-1.0));
    }

    @Test
    @DisplayName("GetStockQuantity should return 0 for non-held stock")
    void testGetQuantityForNonHeldStock() {
        assertEquals(0, portfolio.getStockQuantity(stockC));
    }

    // --- Adding and Updating Stocks ---
    @Test
    @DisplayName("Should add stock and update holdings count")
    void testAddStock() {
        portfolio.addStock(stockA, 5);
        assertEquals(1, portfolio.getHoldingsCount());
        assertEquals(5, portfolio.getStockQuantity(stockA));
    }

    @Test
    @DisplayName("Should increase quantity if same stock is added again")
    void testAddExistingStock() {
        portfolio.addStock(stockA, 5);
        portfolio.addStock(stockA, 3);
        assertEquals(8, portfolio.getStockQuantity(stockA));
        assertEquals(1, portfolio.getHoldingsCount());
    }
    
    @Test
    @DisplayName("Should track multiple unique stocks correctly")
    void testAddMultipleUniqueStocks() {
        portfolio.addStock(stockA, 2);
        portfolio.addStock(stockB, 1);
        
        assertEquals(2, portfolio.getHoldingsCount());
        assertEquals(2, portfolio.getStockQuantity(stockA));
        assertEquals(1, portfolio.getStockQuantity(stockB));
    }

    // --- Value Calculations ---

    @Test
    @DisplayName("Should calculate total value correctly")
    void testCalculateTotalValue() {
        portfolio.addStock(stockA, 2); 
        portfolio.addStock(stockB, 1); 
        
        double expectedStockValue = 300.0 + 2000.0;
        double expectedTotalValue = 1000.0 + expectedStockValue; 
        
        assertEquals(expectedStockValue, portfolio.calculateStockValue());
        assertEquals(expectedTotalValue, portfolio.calculateTotalValue());
    }

    @Test
    @DisplayName("Should update values after stock price changes")
    void testValueUpdateAfterPriceChange() {
        portfolio.addStock(stockA, 10);
        
        stockA.setPrice(160.0);        
        double expectedStockValue = 10 * 160.0;
        assertEquals(expectedStockValue, portfolio.calculateStockValue());
        assertEquals(1000.0 + expectedStockValue, portfolio.calculateTotalValue());
    }

    // --- Border Case: Portfolio Full ---

    @Test
    @DisplayName("Should throw exception when adding unique stock past capacity")
    void testPortfolioFullThrowsException() {
        for (int i = 0; i < 10; i++) {
            portfolio.addStock(new Stock("SYM" + i, "Comp" + i, 10.0), 1);
        }
        
        assertEquals(10, portfolio.getHoldingsCount());

        Stock stock11 = new Stock("S10", "Over Capacity", 5.0);
        
        assertThrows(IllegalStateException.class, () -> { portfolio.addStock(stock11, 1);});
        
        assertEquals(10, portfolio.getHoldingsCount());
    }
    
    @Test
    @DisplayName("Adding existing stock should succeed even when portfolio is full")
    void testAddExistingStockWhenPortfolioIsFull() {
        // fill 9 stock possitions
        for (int i = 0; i < 9; i++){
            portfolio.addStock(new Stock("SYM" + i, "Comp" + i, 10.0), 1);
        }
        portfolio.addStock(stockA, 1); //fill 10th possition
        
        assertEquals(10, portfolio.getHoldingsCount());
        
        // add more quantity to a stock already in full porfolio
        portfolio.addStock(stockA, 5); 
        
        assertEquals(6, portfolio.getStockQuantity(stockA));
        assertEquals(10, portfolio.getHoldingsCount());
    }
}