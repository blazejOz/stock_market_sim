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
        portfolio = new Portfolio(10000.0);
        share = new Share("AAPL", 100.0);
        commodity = new Commodity("GOLD", 100.0);
        currency = new Currency("USD", 100.0);
    }

    // --- Initialization Tests ---
    @Test
    @DisplayName("Should initialize portfolio with correct cash and zero holdings")
    void testPortfolioInitialization() {
        assertAll("Sprawdzenie stanu początkowego",
            () -> assertEquals(10000.0, portfolio.getCash()),
            () -> assertEquals(0, portfolio.getHoldingsCount()),
            () -> assertEquals(0.0, portfolio.calculateHoldingsValue()),
            () -> assertEquals(10000.0, portfolio.calculateTotalValue())
        );
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
        assertAll("Dodawanie aktywa",
            () -> assertEquals(1, portfolio.getHoldingsCount(), "Licznik holdingów powinien wzrosnąć"),
            () -> assertEquals(5, portfolio.getAssetQuantity(share), "Ilość powinna się zgadzać")
        );
    }

    @Test
    @DisplayName("Should throw exception when adding NULL asset")
    void testAddNullAssetThrows() {
        assertThrows(IllegalArgumentException.class, () -> portfolio.addAsset(null, 5));
    }

    @Test
    @DisplayName("Should throw exception when adding zero or negative quantity")
    void testAddZeroOrNegativeQuantityThrows() {
        assertAll("Walidacja ilości",
            () -> assertThrows(IllegalArgumentException.class, () -> portfolio.addAsset(share, 0)),
            () -> assertThrows(IllegalArgumentException.class, () -> portfolio.addAsset(share, -5))
        );
    }

    @Test
    @DisplayName("Should increase quantity if same asset is added again (Merge)")
    void testAddExistingAsset() {
        // Given
        portfolio.addAsset(share, 3);
        
        // When
        portfolio.addAsset(share, 2);
        
        // Then
        assertAll("Merge pozycji",
            () -> assertEquals(1, portfolio.getHoldingsCount(), "Nie powinno tworzyć nowej pozycji"),
            () -> assertEquals(5, portfolio.getAssetQuantity(share), "Powinno zsumować ilość")
        );
    }

    @Test
    @DisplayName("Should track multiple unique assets correctly")
    void testAddMultipleUniqueAssets() {
        portfolio.addAsset(share, 2);
        portfolio.addAsset(commodity, 3);
        portfolio.addAsset(currency, 1);
        
        assertAll("Wiele różnych aktywów",
            () -> assertEquals(3, portfolio.getHoldingsCount()),
            () -> assertEquals(2, portfolio.getAssetQuantity(share)),
            () -> assertEquals(3, portfolio.getAssetQuantity(commodity)),
            () -> assertEquals(1, portfolio.getAssetQuantity(currency))
        );
    }

    // --- Polymorphic Calculation Integration Tests ---
    @Test
    @DisplayName("Portfolio calculates correct total value with mixed assets (Integration)")
    void testCalculateTotalValueWithMixedAssets() {
        // SCENARIUSZ:
        // 1. Share: 5 szt * 100 = 500. Mała transakcja -> Opłata 5.0. 
        //    Koszt zakupu: 505. Wartość (Value): 495.
        portfolio.addAsset(share, 5); 

        // 2. Commodity: 5 szt * 100 = 500. 
        //    Koszt zakupu: 500. Wartość: 500 - (5 szt * 0.50 * 1 dzień min) = 497.5.
        portfolio.addAsset(commodity, 5); 
        
        // 3. Currency: 5 szt * 100 = 500. Spread 0.5%.
        //    Koszt zakupu (Spread przy kupnie): 500 + 2.5 = 502.5. 
        //    Wartość (Spread przy wycenie): 500 - 2.5 = 497.5.
        portfolio.addAsset(currency, 5);
        
        // Obliczenia oczekiwane:
        double initialCash = 10000.0;
        double totalCost = 505.0 + 500.0 + 502.5; // 1507.5
        double expectedCash = initialCash - totalCost; // 8492.5
        
        double expectedHoldingsValue = 495.0 + 497.5 + 497.5; // 1490.0

        assertAll("Kompleksowy audyt portfela",
            () -> assertEquals(expectedCash, portfolio.getCash(), 0.01, "Stan gotówki po zakupach"),
            () -> assertEquals(expectedHoldingsValue, portfolio.calculateHoldingsValue(), 0.01, "Wartość aktywów (polimorfizm)"),
            () -> assertEquals(expectedCash + expectedHoldingsValue, portfolio.calculateTotalValue(), 0.01, "Całkowita wartość portfela")
        );
    }

    // --- Purchase Validation Tests ---
    @Test
    @DisplayName("Should throw exception when purchase exceeds available cash (checking hidden costs)")
    void testPurchaseInsufficientFunds() {
        // Mamy 10,000. Chcemy kupić za 10,000.
        // Ale Currency ma ukryty koszt (spread), więc realny koszt > 10,000.
        Currency eur = new Currency("EUR", 1.0);
        int quantity = 10000; 

        assertThrows(IllegalStateException.class, () -> portfolio.addAsset(eur, quantity),
            "Powinien zablokować zakup, bo spread przekracza budżet");
    }

    @Test
    @DisplayName("Should allow purchase when funds are exactly sufficient")
    void testPurchaseExactFunds() {
        // Commodity nie ma kosztu wejścia (tylko koszt w czasie), więc koszt = cena * ilość
        Portfolio poorPortfolio = new Portfolio(100.0);
        Commodity gold = new Commodity("GOLD", 10.0);
        
        // 10 * 10 = 100
        poorPortfolio.addAsset(gold, 10);
        
        assertEquals(0.0, poorPortfolio.getCash(), "Powinien wyzerować konto co do grosza");
    }

    // --- Portfolio Capacity Tests ---
    @Test
    @DisplayName("Should throw exception when adding unique asset past capacity (10)")
    void testPortfolioCapacityExceeded() {
        // Wypełniamy 10 slotów
        for (int i = 0; i < 10; i++) {
            Share s = new Share("S" + i, 1.0);
            portfolio.addAsset(s, 1);
        }

        // Próba dodania 11-tego
        Share s11 = new Share("S11", 1.0);
        assertThrows(IllegalStateException.class, () -> portfolio.addAsset(s11, 1));
    }

    @Test
    @DisplayName("Should allow increasing quantity of existing asset when portfolio is full")
    void testAddQuantityWhenPortfolioIsFull() {
        // Wypełniamy 10 slotów
        for (int i = 0; i < 10; i++) {
            Share s = new Share("S" + i, 1.0);
            portfolio.addAsset(s, 1);
        }

        // Dokupujemy do pierwszego slotu (S0)
        Share targetShare = new Share("S0", 1.0);
        portfolio.addAsset(targetShare, 5);
        
        assertAll("Dokupowanie przy pełnym portfelu",
            () -> assertEquals(6, portfolio.getAssetQuantity(targetShare)),
            () -> assertEquals(10, portfolio.getHoldingsCount(), "Liczba unikalnych pozycji nie powinna wzrosnąć")
        );
    }

    // --- Edge Cases ---
    @Test
    @DisplayName("Should return 0 for quantity of asset not in portfolio")
    void testGetQuantityNonHeldAsset() {
        assertEquals(0, portfolio.getAssetQuantity(share));
    }
}