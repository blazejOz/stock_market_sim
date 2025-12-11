package com.stockmarket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        portfolio = new Portfolio(1000.0);
        share = new Share("AAPL", 100.0);
        commodity = new Commodity("GOLD", 100.0);
        currency = new Currency("USD", 100.0);
    }

    // --- Initialization Tests ---
    @Test
    @DisplayName("Should initialize portfolio with correct cash and zero holdings")
    void testPortfolioInitialization() {
        assertEquals(1000.0, portfolio.getCash());
        assertEquals(0, portfolio.getHoldingsCount());
        assertEquals(0.0, portfolio.calculateHoldingsValue());
        assertEquals(1000.0, portfolio.calculateTotalValue());
    }

    @Test
    @DisplayName("Should throw exception for negative initial cash")
    void testNegativeInitialCashThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Portfolio(-1.0));
    }

    // --- Adding Assets Tests ---
    @Test
    @DisplayName("Should add asset and update holdings count")
    void testAddAsset() {
        portfolio.addAsset(share, 5);
        assertEquals(1, portfolio.getHoldingsCount());
        assertEquals(5, portfolio.getAssetQuantity(share));
    }

    @Test
    @DisplayName("Should throw exception when adding NULL asset")
    void testAddNullAssetThrows() {
        assertThrows(IllegalArgumentException.class, () -> portfolio.addAsset(null, 5));
    }

    @Test
    @DisplayName("Should throw exception when adding zero or negative quantity")
    void testAddZeroOrNegativeQuantityThrows() {
        assertThrows(IllegalArgumentException.class, () -> portfolio.addAsset(share, 0));
        assertThrows(IllegalArgumentException.class, () -> portfolio.addAsset(share, -5));
    }

    @Test
    @DisplayName("Should increase quantity if same asset is added again")
    void testAddExistingAsset() {
        portfolio.addAsset(share, 3);
        portfolio.addAsset(share, 2);
        assertEquals(1, portfolio.getHoldingsCount());
        assertEquals(5, portfolio.getAssetQuantity(share));
    }

    @Test
    @DisplayName("Should track multiple unique assets correctly")
    void testAddMultipleUniqueAssets() {
        portfolio.addAsset(share, 2);
        portfolio.addAsset(commodity, 3);
        portfolio.addAsset(currency, 1);
        
        assertEquals(3, portfolio.getHoldingsCount());
        assertEquals(2, portfolio.getAssetQuantity(share));
        assertEquals(3, portfolio.getAssetQuantity(commodity));
        assertEquals(1, portfolio.getAssetQuantity(currency));
    }

    // --- Polymorphic Value Calculation Tests ---
    @Test
    @DisplayName("Different asset types have different real values despite same base price")
    void testPolymorphicRealValues() {
        Share s = new Share("S1", 100.0); // transaction fee applies for small transactions
        Commodity c = new Commodity("C1", 100.0); // storage cost 0.50/unit
        Currency cu = new Currency("FX1", 100.0); // spread 0.005 = 0.5%

        // For 10 units of each:
        double sValue = s.calculateRealValue(10); // 100*10 - 0 (no fee if >= 1000) = 1000
        double cValue = c.calculateRealValue(10); // 100*10 - 0.50*10 = 995
        double cuValue = cu.calculateRealValue(10); // 100*10 - (100*10)*0.005 = 995

        assertEquals(1000.0, sValue);
        assertEquals(995.0, cValue);
        assertEquals(995.0, cuValue);
    }

    @Test
    @DisplayName("Share applies transaction fee for small transactions")
    void testShareTransactionFeeForSmallTransaction() {
        Share smallShare = new Share("SMALL", 100.0);
        // 5 units * 100 = 500 < 1000 (threshold), so fee applies
        double realValue = smallShare.calculateRealValue(5);
        double initialCost = smallShare.calculateInitialCost(5);
        
        assertEquals(5.0, initialCost); // Transaction fee of 5.0
        assertEquals(495.0, realValue); // 500 - 5 = 495
    }

    @Test
    @DisplayName("Portfolio calculates correct total value with mixed assets")
    void testCalculateTotalValueWithMixedAssets() {
        portfolio.addAsset(share, 5); // 100*5 = 500 < 1000, so fee applies = 5, real = 495, cost = 505
        portfolio.addAsset(commodity, 2); // 100*2 - 0.50*2 = 199, cost = 200
        
        double holdingsValue = portfolio.calculateHoldingsValue(); // 495 + 199 = 694
        double expectedCash = 1000.0 - 505.0 - 200.0; // 295
        double totalValue = portfolio.calculateTotalValue();

        assertEquals(expectedCash, portfolio.getCash());
        assertEquals(694.0, holdingsValue);
        assertEquals(expectedCash + holdingsValue, totalValue);
    }

    @Test
    @DisplayName("Portfolio calculates correct total value with smaller purchases")
    void testCalculateTotalValueSmaller() {
        portfolio.addAsset(share, 3); // 300 < 1000, fee applies = 5, real = 295, cost = 305
        portfolio.addAsset(commodity, 2); // 200 - 1 = 199, cost = 200
        portfolio.addAsset(currency, 1); // 100 - 100*0.005 = 99.5, cost = 100 + 0.5 = 100.5
        
        double holdingsValue = portfolio.calculateHoldingsValue(); // 295 + 199 + 99.5 = 593.5
        double remainingCash = 1000.0 - 305.0 - 200.0 - 100.5; // 394.5
        double totalValue = portfolio.calculateTotalValue();

        assertEquals(remainingCash, portfolio.getCash());
        assertEquals(593.5, holdingsValue);
        assertEquals(remainingCash + holdingsValue, totalValue);
    }

    // --- Purchase Validation Tests ---
    @Test
    @DisplayName("Should throw exception when purchase exceeds available cash")
    void testPurchaseInsufficientFunds() {
        Share expensiveShare = new Share("EXP", 300.0);
        // 300*4 = 1200 > 1000
        assertThrows(IllegalStateException.class, () -> portfolio.addAsset(expensiveShare, 4));
    }

    @Test
    @DisplayName("Should allow purchase when funds are exactly sufficient")
    void testPurchaseExactFunds() {
        Share exactShare = new Share("EXACT", 100.0);
        // 100 * 10 = 1000 exactly
        portfolio.addAsset(exactShare, 10);
        assertEquals(0.0, portfolio.getCash());
    }

    // --- Portfolio Capacity Tests ---
    @Test
    @DisplayName("Should throw exception when adding unique asset past capacity (10)")
    void testPortfolioCapacityExceeded() {
        // Fill 10 unique positions with small quantities
        for (int i = 0; i < 10; i++) {
            Share s = new Share("S" + i, 1.0);
            portfolio.addAsset(s, 1);
        }
        assertEquals(10, portfolio.getHoldingsCount());

        // Try to add 11th unique asset
        Share s11 = new Share("S10", 1.0);
        assertThrows(IllegalStateException.class, () -> portfolio.addAsset(s11, 1));
    }

    @Test
    @DisplayName("Should allow increasing quantity of existing asset when portfolio is full")
    void testAddQuantityWhenPortfolioIsFull() {
        // Fill 10 positions
        for (int i = 0; i < 10; i++) {
            Share s = new Share("S" + i, 1.0);
            portfolio.addAsset(s, 1);
        }
        assertEquals(10, portfolio.getHoldingsCount());

        // Try to increase quantity of first asset (should succeed)
        Share targetShare = new Share("S0", 1.0);
        portfolio.addAsset(targetShare, 5);
        assertEquals(6, portfolio.getAssetQuantity(targetShare));
        assertEquals(10, portfolio.getHoldingsCount());
    }

    // --- Edge Cases & Validation ---
    @Test
    @DisplayName("Should return 0 for quantity of asset not in portfolio")
    void testGetQuantityNonHeldAsset() {
        assertEquals(0, portfolio.getAssetQuantity(share));
    }

    @Test
    @DisplayName("Should throw exception when getting quantity of NULL asset")
    void testGetQuantityNullAssetThrows() {
        assertThrows(IllegalArgumentException.class, () -> portfolio.getAssetQuantity(null));
    }

    @Test
    @DisplayName("Commodity real value accounts for storage costs")
    void testCommodityStorageCostDeduction() {
        Commodity c = new Commodity("GOLD", 100.0);
        double realValue = c.calculateRealValue(10);
        // 100*10 - 0.50*10 = 995
        assertEquals(995.0, realValue);
    }

    @Test
    @DisplayName("Currency real value accounts for spread")
    void testCurrencySpreadDeduction() {
        Currency cu = new Currency("EUR", 100.0);
        double realValue = cu.calculateRealValue(10);
        // 100*10 - (100*10)*0.005 = 1000 - 5 = 995
        assertEquals(995.0, realValue);
    }

    @Test
    @DisplayName("Share initial cost is zero for large transactions")
    void testShareNoFeeForLargeTransaction() {
        Share s = new Share("BIG", 100.0);
        // 100 * 15 = 1500 >= 1000 threshold
        double initialCost = s.calculateInitialCost(15);
        assertEquals(0.0, initialCost);
    }
}