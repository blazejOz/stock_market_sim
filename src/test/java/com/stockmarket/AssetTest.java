package com.stockmarket;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.stockmarket.domain.Commodity;
import com.stockmarket.domain.Currency;
import com.stockmarket.domain.Share;

class AssetTest {

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
    @DisplayName("Test metod equals i hashCode")
    void testEqualsAndHashCodeContract() {
        // GIVEN
        Share apple1 = new Share("AAPL", 100.0);
        Share apple2 = new Share("AAPL", 250.0); // ta sama nazwa, inna cena
        
        Share google = new Share("GOOG", 100.0); // inna nazwa
        
        // To samo ID, ale inny typ obiektu
        Commodity appleCommodity = new Commodity("AAPL", 100.0); 

        assertAll("Czy equals działa jak trzeba",
            // czy obiekt jest równy sam sobie
            () -> assertEquals(apple1, apple1, "Obiekt != on sam"),

            // czy cena jest ignorowana przy porównaniu
            () -> assertEquals(apple1, apple2, "Różne ceny zepsuły equals"),
            () -> assertEquals(apple2, apple1, "Symetria nie działa"),

            // czy hashCode jest taki sam dla równych obiektów
            () -> assertEquals(apple1.hashCode(), apple2.hashCode(), "HashCode się rozjechał"),

            // czy różne symbole dają false
            () -> assertNotEquals(apple1, google, "Różne symbole są równe?"),
            
            // czy typy są sprawdzane (Akcja vs Surowiec)
            () -> assertNotEquals(apple1, appleCommodity, "Akcja nie może być równa Surowcowi"),
            
            // null check
            () -> assertNotEquals(null, apple1, "Null nie może być równy obiektowi")
        );
    }
}