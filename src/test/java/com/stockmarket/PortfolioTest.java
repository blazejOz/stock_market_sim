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

    @Test
    @DisplayName("Constructor should throw exception when initial cash is negative")
    void testPortfolioConstructorWithNegativeCash() {
        // Sprawdzamy czy rzucany jest wyjątek IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new Portfolio(-100.0);
        });
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
    
        @Test
    @DisplayName("Constructor should throw exception when selling val is negative")
    void testSellAssetNegativeValue() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            portfolio.sellAsset("APPL", -100, 100);
        });
    }


    // --- Polymorphism Tests ---
    @Test
    @DisplayName("Different asset types calculate real values differently")
    void testPolymorphicRealValues() {
        Share s = new Share("S1", 100.0);
        Commodity c = new Commodity("C1", 100.0);
        Currency cu = new Currency("FX1", 100.0);
        
        assertAll("Polymorphic Calculation",
            () -> assertEquals(1000.0, s.calculateRealValue(10, 10), 0.01), 
            () -> assertEquals(950.0, c.calculateRealValue(10, 10), 0.01),  
            () -> assertEquals(995.0, cu.calculateRealValue(10, 10), 0.01)  
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

     // --- Report Tests ---
    @Test
    @DisplayName("Should generate report containing key information")
    void testGenerateReport() {
        Portfolio reportPortfolio = new Portfolio(20000.0);
        
        Share s = new Share("AAPL", 100.0);
        Currency c = new Currency("USD", 100.0);

        reportPortfolio.addAsset(s, 10);   
        reportPortfolio.addAsset(c, 50);  
        
        String report = reportPortfolio.generateReport();
        
        assertAll("Report Content",
            () -> assertTrue(report.contains("PORTFOLIO REPORT")),
            () -> assertTrue(report.contains("AAPL")), 
            () -> assertTrue(report.contains("USD")),  
            () -> assertTrue(report.contains("SHARE")), 
            () -> assertTrue(report.contains("CURRENCY")) 
        );
    }

        @Test
    @DisplayName("Should generate sorted report correctly (Comparator logic check)")
    void testGenerateReportSorting() {
        // Przygotowanie danych do sortowania
        // Kolejność typów w Enum: SHARE, COMMODITY, CURRENCY
        
        // SHARE (wartość 5000) - powinien być pierwszy
        Share expensiveShare = new Share("SHR_HIGH", 500.0);
        portfolio.addAsset(expensiveShare, 1); 

        // SHARE (wartość 1000) - powinien być drugi
        Share cheapShare = new Share("SHR_LOW", 100.0);
        portfolio.addAsset(cheapShare, 1);

        // COMMODITY (wartość 2000) - powinien być trzeci
        Commodity gold = new Commodity("GOLD", 200.0);
        portfolio.addAsset(gold, 1);

        // CURRENCY (wartość 10000) - powinien być ostatni (mimo najwyższej wartości, bo typ ma priorytet)
        Currency usd = new Currency("USD", 100.0);
        portfolio.addAsset(usd, 10);

        String report = portfolio.generateReport();
        
        // Sprawdzenie kolejności występowania w raporcie
        // Szukamy indeksów (pozycji) symboli w wygenerowanym stringu
        int idxHighShare = report.indexOf("SHR_HIGH");
        int idxLowShare = report.indexOf("SHR_LOW");
        int idxGold = report.indexOf("GOLD");
        int idxUsd = report.indexOf("USD");

        assertAll("Report Sorting Order",
            // 1. Sprawdzenie sortowania wewnątrz grupy SHARE (po wartości malejąco)
            () -> assertTrue(idxHighShare < idxLowShare, "High value share should appear before low value share"),
            
            // 2. Sprawdzenie sortowania typów (SHARE przed COMMODITY)
            () -> assertTrue(idxLowShare < idxGold, "Shares should appear before Commodities"),
            
            // 3. Sprawdzenie sortowania typów (COMMODITY przed CURRENCY)
            () -> assertTrue(idxGold < idxUsd, "Commodities should appear before Currencies")
        );
    }
}