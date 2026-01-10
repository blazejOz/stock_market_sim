package com.stockmarket;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.stockmarket.domain.Commodity;
import com.stockmarket.domain.Currency;
import com.stockmarket.domain.PurchaseLot;
import com.stockmarket.domain.Share;

class AssetTest {

    @ParameterizedTest
    @DisplayName("Should throw exception for invalid Symbols (null or empty)")
    @NullAndEmptySource
    @ValueSource(strings = {"   "}) // Tylko białe znaki
    void testInvalidSymbolValidation(String invalidSymbol) {
        assertThrows(IllegalArgumentException.class, () -> {
            // Używamy Share jako konkretnej implementacji Asset do testów
            new Share(invalidSymbol, 100.0);
        });
    }

    @ParameterizedTest
    @DisplayName("Should throw exception for invalid Price (zero or negative)")
    @ValueSource(doubles = {0.0, -100.0})
    void testInvalidPriceValidation(double invalidPrice) {
        assertThrows(IllegalArgumentException.class, () -> {
            new Share("AAPL", invalidPrice);
        });
    }

    @Test
    @DisplayName("Polimorfizm: Różne aktywa liczą wartość inaczej")
    void shouldCalculateDifferentValuesForVariousAssets() {
        double price = 100.0;
        int quantity = 5;
        long daysHeld = 10;

        Share share = new Share("AAPL", price);
        Currency currency = new Currency("USD", price);
        Commodity gold = new Commodity("GOLD", price);

        double shareValue = share.calculateRealValue(quantity, daysHeld);
        double currencyValue = currency.calculateRealValue(quantity, daysHeld);
        double commodityValue = gold.calculateRealValue(quantity, daysHeld);

        assertAll("Sprawdzamy czy każdy typ liczy po swojemu",
            
            // Akcje mają stałą opłatę przy małych kwotach
            () -> assertEquals(495.0, shareValue, 0.01, "Share: brak opłaty 5.0"),

            // Waluty mają spread (0.5%)
            () -> assertEquals(497.5, currencyValue, 0.01, "Currency: brak spreadu"),

            // Surowce tracą na wartości przez magazynowanie (ilość * dni * stawka)
            () -> assertEquals(475.0, commodityValue, 0.01, "Commodity: zły koszt magazynowania"),

            // Sprawdzenie czy wyniki się różnią
            () -> assertNotEquals(shareValue, currencyValue, "Akcja i Waluta nie powinny być równe"),
            () -> assertNotEquals(shareValue, commodityValue, "Akcja i Surowiec nie powinny być równe"),
            () -> assertNotEquals(currencyValue, commodityValue, "Waluta i Surowiec nie powinny być równe")
        );
    }

    @Test
    @DisplayName("Akcje: Sprawdzanie progów opłat")
    void shouldApplyShareTransactionFeeOnlyForSmallTransactions() {
        // GIVEN
        Share cheapShare = new Share("SMALL", 10.0);
        Share expensiveShare = new Share("BIG", 200.0);
        
        // WHEN
        // mała kwota -> powinna być opłata
        double smallValue = cheapShare.calculateRealValue(5, 0);
        
        // duża kwota -> bez dodatkowych kosztów
        double bigValue = expensiveShare.calculateRealValue(10, 0);
        
        // THEN
        assertAll("Czy progi działają",
            () -> assertEquals(45.0, smallValue, 0.01, "Mała transakcja powinna mieć opłatę"),
            () -> assertEquals(2000.0, bigValue, 0.01, "Duża transakcja powinna być bez opłaty")
        );
    }

    @Test
    @DisplayName("Surowce: Koszt magazynowania w czasie")
    void shouldCalculateCommodityStorageCost() {
        // GIVEN
        Commodity oil = new Commodity("OIL", 100.0);
        int quantity = 100;
        long daysHeld = 30; // trzymamy miesiąc

        // WHEN
        double realValue = oil.calculateRealValue(quantity, daysHeld);

        // THEN: 10000 - (spory koszt magazynowania)
        assertEquals(8500.0, realValue, 0.01, "Coś nie tak z kosztem ropy po czasie");
    }

    @Test
    @DisplayName("Test metod equals i hashCode dla wszystkich typów aktywów")
    void testEqualsAndHashCodeContract() {
        
        // Share (Akcje)
        Share apple1 = new Share("AAPL", 100.0);
        Share apple2 = new Share("AAPL", 250.0); // ta sama nazwa, inna cena
        Share google = new Share("GOOG", 100.0); // inna nazwa

        // Commodity (Surowce)
        Commodity gold1 = new Commodity("GOLD", 1800.0);
        Commodity gold2 = new Commodity("GOLD", 1900.0);
        Commodity oil = new Commodity("OIL", 80.0);

        // Currency (Waluty)
        Currency usd1 = new Currency("USD", 4.0);
        Currency usd2 = new Currency("USD", 4.2);
        Currency eur = new Currency("EUR", 4.5);

        assertAll("Weryfikacja kontraktu equals i hashCode",
            
            () -> assertEquals(apple1, apple1, "Share: Obiekt != on sam (Refleksywność)"),
            () -> assertEquals(apple1, apple2, "Share: Różne ceny zepsuły equals (powinny być równe po symbolu)"),
            () -> assertEquals(apple2, apple1, "Share: Symetria nie działa"),
            () -> assertEquals(apple1.hashCode(), apple2.hashCode(), "Share: HashCode się rozjechał dla równych obiektów"),
            () -> assertNotEquals(apple1, google, "Share: Różne symbole są równe?"),
            
            () -> assertEquals(gold1, gold1, "Commodity: Obiekt != on sam"),
            () -> assertEquals(gold1, gold2, "Commodity: Różne ceny zepsuły equals"),
            () -> assertEquals(gold1.hashCode(), gold2.hashCode(), "Commodity: HashCode się rozjechał"),
            () -> assertNotEquals(gold1, oil, "Commodity: Różne symbole są równe?"),

            () -> assertEquals(usd1, usd1, "Currency: Obiekt != on sam"),
            () -> assertEquals(usd1, usd2, "Currency: Różne ceny zepsuły equals"),
            () -> assertEquals(usd1.hashCode(), usd2.hashCode(), "Currency: HashCode się rozjechał"),
            () -> assertNotEquals(usd1, eur, "Currency: Różne symbole są równe?"),

            () -> assertNotEquals(apple1, gold1, "Akcja nie może być równa Surowcowi"),
            () -> assertNotEquals(apple1, usd1, "Akcja nie może być równa Walucie"),
            () -> assertNotEquals(gold1, usd1, "Surowiec nie może być równy Walucie"),
            
         
            () -> assertNotEquals(new Share("GOLD", 100.0), gold1, "Share 'GOLD' nie może być równy Commodity 'GOLD'"),

  
            () -> assertNotEquals(null, apple1, "Share: Null nie może być równy obiektowi"),
            () -> assertNotEquals(null, gold1, "Commodity: Null nie może być równy obiektowi"),
            () -> assertNotEquals(null, usd1, "Currency: Null nie może być równy obiektowi")
        );
    }

    @Test
    @DisplayName("Should create lot with valid data")
    void testPurchaseLotCreation() {
        // Given
        int day = 10;
        double price = 150.0;
        int quantity = 5;

        // When
        PurchaseLot lot = new PurchaseLot(day, price, quantity);

        // Then
        assertAll("Lot State",
            () -> assertEquals(day, lot.getPurchaseDate()),
            () -> assertEquals(price, lot.getUnitPrice()),
            () -> assertEquals(quantity, lot.getQuantity())
        );
    }

    @Test
    @DisplayName("Should throw exception for invalid constructor arguments")
    void testInvalidConstructorArguments() {
        assertAll("Invalid Inputs",
            // Ilość <= 0
            () -> assertThrows(IllegalArgumentException.class, () -> new PurchaseLot(10, 150.0, 0)),
            () -> assertThrows(IllegalArgumentException.class, () -> new PurchaseLot(10, 150.0, -5)),
            
            // Cena < 0
            () -> assertThrows(IllegalArgumentException.class, () -> new PurchaseLot(10, -10.0, 5))
        );
    }

    @Test
    @DisplayName("Should decrease quantity correctly")
    void testDecreaseQuantity() {
        // Given
        PurchaseLot lot = new PurchaseLot(10, 150.0, 10);

        // When (Sprzedajemy część)
        lot.decreaseQuantity(3);

        // Then
        assertEquals(7, lot.getQuantity());
        
        // When (Sprzedajemy resztę)
        lot.decreaseQuantity(7);
        assertEquals(0, lot.getQuantity());
    }

    @Test
    @DisplayName("Should throw exception when decreasing more than quantity")
    void testDecreaseQuantityTooMuch() {
        // Given
        PurchaseLot lot = new PurchaseLot(10, 150.0, 5);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> lot.decreaseQuantity(6));
    }
}