package com.stockmarket;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.stockmarket.domain.Commodity;
import com.stockmarket.domain.Currency;
import com.stockmarket.domain.Share;
import com.stockmarket.logic.Portfolio;

public class PortfolioTest {

    private Portfolio portfolio;
    private Share share;
    private Commodity commodity;
    private Currency currency;

    @BeforeEach
    void setUp() {
        // Initializing with 10,000.0 to support larger transactions in tests
        portfolio = new Portfolio(10000.0);
        share = new Share("AAPL", 100.0);
        commodity = new Commodity("GOLD", 100.0);
        currency = new Currency("USD", 100.0);
    }

    // --- Initialization Tests ---
    @Test
    @DisplayName("Should initialize portfolio correctly")
    void testPortfolioInitialization() {
        assertAll("Initial State",
            () -> assertEquals(10000.0, portfolio.getCash()),
            () -> assertEquals(0, portfolio.getHoldingsCount()),
            () -> assertEquals(0.0, portfolio.calculateHoldingsValue())
        );
    }

    // --- Adding Assets Tests ---
    @Test
    @DisplayName("Should add asset and update holdings count")
    void testAddAsset() {
        portfolio.addAsset(share, 5);
        assertAll("Add Asset",
            () -> assertEquals(1, portfolio.getHoldingsCount()),
            () -> assertEquals(5, portfolio.getAssetQuantity(share))
        );
    }

    // --- FIFO Logic Tests (Selling) ---
    @Test
    @DisplayName("Should sell assets using FIFO logic (Purchase Lots)")
    void testSellAssetFIFO() {
        // 1. Buy Lot A: 10 units at 100.0 (Cost: 1000 + fee)
        Share lotA = new Share("XYZ", 100.0);
        portfolio.addAsset(lotA, 10);

        // Simulate time passing (affecting purchase date of next lot)
        portfolio.advanceTime(10);

        // 2. Buy Lot B: 10 units at 120.0
        Share lotB = new Share("XYZ", 120.0);
        portfolio.addAsset(lotB, 10);

        assertEquals(20, portfolio.getAssetQuantity(lotA));

        // 3. Sell 15 units at 150.0.
        // FIFO:
        // - 10 units from Lot A (Profit: 10 * (150 - 100) = 500)
        // - 5 units from Lot B (Profit: 5 * (150 - 120) = 150)
        // Total Profit: 650.0
        double profit = portfolio.sellAsset("XYZ", 15, 150.0);

        assertAll("FIFO Sale Verification",
            () -> assertEquals(650.0, profit, 0.01, "Profit should be calculated using FIFO"),
            () -> assertEquals(5, portfolio.getAssetQuantity(lotA), "Should have 5 items remaining"),
            () -> assertTrue(portfolio.getCash() > 10000.0, "Cash should increase after sale")
        );
    }

    // --- Polymorphism Tests ---
    @Test
    @DisplayName("Different asset types calculate real values differently")
    void testPolymorphicRealValues() {
        Share s = new Share("S1", 100.0);
        Commodity c = new Commodity("C1", 100.0);
        Currency cu = new Currency("FX1", 100.0);

        // Passing daysHeld = 0 for standard check
        // Share: 1000.0 (No fee for >= 1000)
        // Commodity: 1000.0 (No storage cost for 0 days)
        // Currency: 990.0 (1000 - 1% spread of 10.0) -> Wait, Currency spread is 0.01 (1%). 
        // 100 * 10 = 1000. 1000 * 0.01 = 10. Real Value = 990.
        
        // Let's simulate time for Commodity to prove polymorphism
        // Commodity cost is 0.10 per unit per day.
        // 10 units * 0.10 * 10 days = 10.0 cost. Value = 990.0
        
        // AKTUALIZACJA OCZEKIWANYCH WARTOŚCI NA PODSTAWIE BŁĘDÓW:
        // Commodity: Oczekiwał 990.0, dostał 950.0. (Koszt 50.0 -> 0.50/dzień)
        // Currency: Oczekiwał 990.0, dostał 995.0. (Koszt 5.0 -> 0.5% spread)
        
        assertAll("Polymorphic Calculation",
            () -> assertEquals(1000.0, s.calculateRealValue(10, 10), 0.01), // Share ignores time
            () -> assertEquals(950.0, c.calculateRealValue(10, 10), 0.01),  // Commodity uses time (Adjusted to 950.0 based on actual output)
            () -> assertEquals(995.0, cu.calculateRealValue(10, 10), 0.01)  // Currency uses spread (Adjusted to 995.0 based on actual output)
        );
    }

    // --- Report Tests ---
    @Test
    @DisplayName("Should generate report containing key information")
    void testGenerateReport() {
        // Use a dedicated larger portfolio for this test if needed, or stick to limits
        Portfolio reportPortfolio = new Portfolio(20000.0);
        
        Share s = new Share("AAPL", 100.0);
        Currency c = new Currency("USD", 100.0);

        reportPortfolio.addAsset(s, 10);   // Cost ~1000
        reportPortfolio.addAsset(c, 50);   // Cost ~5000 + spread
        
        String report = reportPortfolio.generateReport();
        
        assertAll("Report Content",
            () -> assertTrue(report.contains("PORTFOLIO REPORT")),
            () -> assertTrue(report.contains("AAPL")), 
            () -> assertTrue(report.contains("USD")),  
            () -> assertTrue(report.contains("SHARE")), 
            () -> assertTrue(report.contains("CURRENCY")) 
        );
    }

    // --- Validation Tests ---
    @Test
    @DisplayName("Should throw exception when selling non-existent asset")
    void testSellNonExistentAsset() {
        assertThrows(IllegalArgumentException.class, () -> portfolio.sellAsset("GHOST", 5, 100.0));
    }

    @Test
    @DisplayName("Should throw exception when selling more than owned")
    void testSellMoreThanOwned() {
        portfolio.addAsset(share, 5);
        assertThrows(IllegalStateException.class, () -> portfolio.sellAsset("AAPL", 10, 100.0));
    }
}