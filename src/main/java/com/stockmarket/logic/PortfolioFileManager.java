package com.stockmarket.logic; 
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import com.stockmarket.domain.Asset;
import com.stockmarket.domain.AssetType;
import com.stockmarket.domain.Commodity;
import com.stockmarket.domain.Currency;
import com.stockmarket.domain.Share;


public class PortfolioFileManager {

    private static final String SEPARATOR = "\\|"; // Regex do podziału po znaku '|'

    // Zapis do pliku
    public void savePortfolio(Portfolio portfolio, String filename) {
        // Try-with-resources zapewnia zamknięcie pliku
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            
            // 1. Zapisz NAGŁÓWEK (Gotówka i Dzień)
            // Format: HEADER|CASH|DAY
            // Locale.US wymusza kropkę w liczbach zmiennoprzecinkowych (100.50 zamiast 100,50)
            writer.println(String.format(Locale.US, "HEADER|%.2f|%d", 
                portfolio.getCash(), 
                portfolio.getCurrentDay()));

            // 2. Pobierz dane o aktywach i zapisz je linia po linii
            String[] holdingsData = portfolio.getHoldingsData();
            
            for (String line : holdingsData) {
                // Format linii: LOT|TYPE|SYMBOL|PRICE|QTY|PURCHASE_DAY
                writer.println("LOT|" + line);
            }

        } catch (IOException e) {
            throw new DataIntegrityException("Error saving portfolio: " + e.getMessage(), e);
        }
    }

    // Odczyt z pliku
    public Portfolio loadPortfolio(String filename) {
        Portfolio portfolio = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] parts = line.split(SEPARATOR);

                if (parts.length == 0 || line.trim().isEmpty()) continue;

                String recordType = parts[0];

                if ("HEADER".equals(recordType)) {
                    // Walidacja nagłówka
                    if (parts.length < 3) {
                        throw new DataIntegrityException("Invalid header format at line " + lineNumber);
                    }
                    double cash = Double.parseDouble(parts[1]);
                    int day = Integer.parseInt(parts[2]);
                    
                    portfolio = new Portfolio(cash);
                    portfolio.setCurrentDay(day);

                } else if ("LOT".equals(recordType)) {
                    // Walidacja spójności: Nagłówek musi być pierwszy
                    if (portfolio == null) {
                        throw new DataIntegrityException("Missing HEADER before LOT data.");
                    }
                    // Format: LOT|TYPE|SYMBOL|PRICE|QTY|PURCHASE_DAY
                    if (parts.length < 6) {
                        throw new DataIntegrityException("Invalid lot format at line " + lineNumber);
                    }

                    // Parsowanie pól
                    String typeStr = parts[1];
                    String symbol = parts[2];
                    double price = Double.parseDouble(parts[3]);
                    int quantity = Integer.parseInt(parts[4]);
                    int purchaseDay = Integer.parseInt(parts[5]);

                    // Tworzenie obiektu Asset na podstawie Enuma (Factory logic)
                    Asset asset = createAssetFromType(typeStr, symbol, price);
                    
                    // Wczytanie do portfela (omijając logikę zakupową i pobieranie gotówki)
                    portfolio.loadAsset(asset, quantity, purchaseDay);
                } else {
                    // Nieznany typ rekordu
                    throw new DataIntegrityException("Unknown record type at line " + lineNumber + ": " + recordType);
                }
            }
        } catch (FileNotFoundException e) {
            throw new DataIntegrityException("File not found: " + filename, e);
        } catch (IOException e) {
            throw new DataIntegrityException("IO Error reading portfolio", e);
        } catch (IllegalArgumentException e) {
            // Łapie błędy parsowania liczb oraz błędy z Enum.valueOf
            throw new DataIntegrityException("Data format error: " + e.getMessage(), e);
        }

        if (portfolio == null) {
            throw new DataIntegrityException("File was empty or missing valid header");
        }
        return portfolio;
    }

    // Pomocnicza metoda do tworzenia instancji Asset na podstawie stringa z pliku
    private Asset createAssetFromType(String typeStr, String symbol, double price) {
        AssetType type = AssetType.valueOf(typeStr); // Może rzucić IllegalArgumentException
        
        switch (type) {
            case SHARE:
                return new Share(symbol, price);
            case COMMODITY:
                return new Commodity(symbol, price);
            case CURRENCY:
                return new Currency(symbol, price);
            default:
                throw new DataIntegrityException("Unsupported asset type: " + type);
        }
    }
}