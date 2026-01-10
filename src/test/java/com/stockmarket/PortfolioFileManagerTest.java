package com.stockmarket;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.stockmarket.domain.Commodity;
import com.stockmarket.domain.Share;
import com.stockmarket.logic.DataIntegrityException;
import com.stockmarket.logic.Portfolio;
import com.stockmarket.logic.PortfolioFileManager;

class PortfolioFileManagerTest {

    private PortfolioFileManager fileManager;
    private static final String TEST_FILENAME = "test_portfolio_save.txt";

    @BeforeEach
    void setUp() {
        fileManager = new PortfolioFileManager();
    }

    @AfterEach
    void tearDown() {
        // Sprzątanie po każdym teście (usuwamy plik tymczasowy)
        File file = new File(TEST_FILENAME);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("Should save and load portfolio state correctly")
    void testSaveAndLoadPortfolio() {
        // 1. Przygotowanie stanu początkowego
        Portfolio original = new Portfolio(10000.0);
        original.advanceTime(5); // Ustawiamy dzień na 5
        
        Share apple = new Share("AAPL", 150.0);
        Commodity gold = new Commodity("GOLD", 1800.0);

        // Kupujemy aktywa (zmieni się gotówka)
        original.addAsset(apple, 10);
        original.addAsset(gold, 2);

        double cashAfterPurchases = original.getCash();

        // 2. Zapis do pliku
        fileManager.savePortfolio(original, TEST_FILENAME);

        // 3. Odczyt z pliku
        Portfolio loaded = fileManager.loadPortfolio(TEST_FILENAME);

        // 4. Weryfikacja spójności danych
        assertAll("Portfolio Reconstruction",
            // Czy gotówka się zgadza?
            () -> assertEquals(cashAfterPurchases, loaded.getCash(), 0.01, "Cash mismatch"),
            // Czy dzień się zgadza?
            () -> assertEquals(5, loaded.getCurrentDay(), "Current day mismatch"),
            // Czy liczba pozycji się zgadza?
            () -> assertEquals(2, loaded.getHoldingsCount(), "Holdings count mismatch"),
            // Czy ilości konkretnych aktywów się zgadzają?
            () -> assertEquals(10, loaded.getAssetQuantity(apple), "Apple quantity mismatch"),
            () -> assertEquals(2, loaded.getAssetQuantity(gold), "Gold quantity mismatch")
        );
    }

    @Test
    @DisplayName("Should throw exception when file header is missing")
    void testLoadCorruptedFileMissingHeader() throws IOException {
        // Tworzymy plik bez nagłówka HEADER
        try (PrintWriter out = new PrintWriter(new FileWriter(TEST_FILENAME))) {
            out.println("LOT|SHARE|AAPL|150.00|10|0");
        }

        assertThrows(DataIntegrityException.class, () -> {
            fileManager.loadPortfolio(TEST_FILENAME);
        }, "Should detect missing header");
    }

    @Test
    @DisplayName("Should throw exception when data format is invalid (non-numeric)")
    void testLoadInvalidNumberFormat() throws IOException {
        // Tworzymy plik z błędem (tekst zamiast liczby)
        try (PrintWriter out = new PrintWriter(new FileWriter(TEST_FILENAME))) {
            out.println("HEADER|10000.00|0");
            out.println("LOT|SHARE|AAPL|NOT_A_NUMBER|10|0");
        }

        assertThrows(DataIntegrityException.class, () -> {
            fileManager.loadPortfolio(TEST_FILENAME);
        }, "Should detect number format errors");
    }

    @Test
    @DisplayName("Should throw exception when file does not exist")
    void testLoadNonExistentFile() {
        assertThrows(DataIntegrityException.class, () -> {
            fileManager.loadPortfolio("ghost_file.txt");
        });
    }
}