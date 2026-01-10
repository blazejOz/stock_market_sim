package com.stockmarket.logic;

import com.stockmarket.domain.Asset;
import com.stockmarket.domain.AssetType;
import com.stockmarket.domain.PurchaseLot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Portfolio {

    private double cash;
    private int currentDay;

    // UPROSZCZENIE: Tylko jedna mapa.
    // Kluczem jest symbol (np. "AAPL"), wartością jest obiekt trzymający wszystko co wiemy o tym aktywie.
    private final Map<String, AssetEntry> holdings;

    public Portfolio(double initialCash) {
        if (initialCash < 0) {
            throw new IllegalArgumentException("Initial cash cannot be negative.");
        }
        this.cash = initialCash;
        this.currentDay = 0;
        this.holdings = new HashMap<>();
    }

    public void advanceTime(int days) {
        if (days > 0) this.currentDay += days;
    }

    // --- Klasa Wewnętrzna: Agreguje wiedzę o jednym aktywie ---
    // Trzyma zarówno definicję (cenę/typ) jak i historię zakupów (partie).
    private static class AssetEntry {
        Asset assetDefinition; // Potrzebne do logiki wyceny (polimorfizm)
        List<PurchaseLot> lots; // Historia zakupów (FIFO)

        AssetEntry(Asset asset) {
            this.assetDefinition = asset;
            this.lots = new ArrayList<>();
        }

        void addLot(int purchaseDay, double price, int quantity) {
            lots.add(new PurchaseLot(purchaseDay, price, quantity));
        }

        // IMPLEMENTACJA FIFO (First-In, First-Out)
        // Zwraca zysk (Profit/Loss) z transakcji
        double processSale(int quantityToSell, double currentMarketPrice) {
            int remainingToSell = quantityToSell;
            double totalCostBase = 0.0; // Koszt zakupu sprzedawanych partii
            List<PurchaseLot> emptyLots = new ArrayList<>();

            // Iteracja po partiach od najstarszej (indeks 0)
            for (PurchaseLot lot : lots) {
                if (remainingToSell == 0) break;

                // Ile możemy wziąć z tej partii?
                int quantityFromLot = Math.min(remainingToSell, lot.getQuantity());

                // Obliczamy koszt zakupu tej części (Cena zakupu partii * ilość)
                totalCostBase += quantityFromLot * lot.getUnitPrice();

                // Aktualizujemy partię i licznik
                lot.decreaseQuantity(quantityFromLot);
                remainingToSell -= quantityFromLot;

                // Jeśli partia jest pusta, oznaczamy do usunięcia
                if (lot.getQuantity() == 0) {
                    emptyLots.add(lot);
                }
            }

            // Usuwamy zużyte partie (Sprzątanie)
            lots.removeAll(emptyLots);

            // Obliczenie wyniku finansowego
            double revenue = quantityToSell * currentMarketPrice; // Przychód ze sprzedaży
            return revenue - totalCostBase; // Zysk = Przychód - Koszt Zakupu
        }

        double calculateValue(int currentDay) {
            double value = 0.0;
            // Prosta pętla po partiach - to tutaj dzieje się magia wyceny
            for (PurchaseLot lot : lots) {
                if (lot.getQuantity() > 0) {
                    int daysHeld = currentDay - (int) lot.getPurchaseDate();
                    // Asset sam liczy wartość dla danej ilości i czasu
                    value += assetDefinition.calculateRealValue(lot.getQuantity(), daysHeld);
                }
            }
            return value;
        }

        int getTotalQuantity() {
            int total = 0;
            for (PurchaseLot lot : lots) {
                total += lot.getQuantity();
            }
            return total;
        }
    }
    // ----------------------------------------------------------

    public void addAsset(Asset asset, int quantity) {
        if (asset == null) throw new IllegalArgumentException("Asset cannot be null.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");

        // 1. Sprawdzenie kosztów (bez zmian)
        double nominalCost = asset.getMarketPrice() * quantity;
        double initialCost = asset.calculateInitialCost(quantity);
        double totalCost = nominalCost + initialCost;

        if (totalCost > this.cash) {
            throw new IllegalStateException("Insufficient funds. Cost: " + totalCost);
        }

        // 2. Realizacja zakupu
        this.cash -= totalCost;
        String symbol = asset.getSymbol();

        // 3. Logika dodawania do mapy (UPROSZCZONA)
        // Jeśli nie mamy tego aktywa, tworzymy wpis.
        if (!holdings.containsKey(symbol)) {
            holdings.put(symbol, new AssetEntry(asset));
        }
        
        // Pobieramy wpis i zlecamy mu dodanie partii.
        // Aktualizujemy też definicję aktywa (np. nowa cena rynkowa).
        AssetEntry entry = holdings.get(symbol);
        entry.assetDefinition = asset; 
        entry.addLot(this.currentDay, asset.getMarketPrice(), quantity);
    }

    // Nowa metoda: Sprzedaż aktywów (FIFO)
    // Zwraca zysk/stratę z transakcji
    public double sellAsset(String symbol, int quantity, double currentPrice) {
        if (!holdings.containsKey(symbol)) {
            throw new IllegalArgumentException("Asset not found in portfolio: " + symbol);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to sell must be positive.");
        }

        AssetEntry entry = holdings.get(symbol);
        if (entry.getTotalQuantity() < quantity) {
            throw new IllegalStateException("Not enough asset quantity to sell.");
        }

        // Delegujemy logikę FIFO do klasy wewnętrznej
        double profit = entry.processSale(quantity, currentPrice);

        // Aktualizujemy gotówkę (Przychód ze sprzedaży)
        double revenue = quantity * currentPrice;
        this.cash += revenue;

        // Jeśli sprzedaliśmy wszystko, możemy usunąć wpis z mapy (opcjonalnie)
        if (entry.getTotalQuantity() == 0) {
            holdings.remove(symbol);
        }

        return profit;
    }

    public double calculateHoldingsValue() {
        double totalValue = 0.0;
        // Iteracja jest teraz czystsza - delegujemy wycenę do AssetEntry
        for (AssetEntry entry : holdings.values()) {
            totalValue += entry.calculateValue(this.currentDay);
        }
        return totalValue;
    }

    public double calculateTotalValue() {
        return this.cash + calculateHoldingsValue();
    }


    // RAPORTOWANIE
    public String generateReport() {
        // 1. Konwersja Mapy na Listę w celu posortowania
        List<AssetEntry> entries = new ArrayList<>(holdings.values());

        // 2. Sortowanie z własnym Comparatorem (bez Stream API)
        // Klucz sortowania: Typ Aktywa -> Wartość Rynkowa (malejąco)
        entries.sort(new Comparator<AssetEntry>() {
            @Override
            public int compare(AssetEntry e1, AssetEntry e2) {
                // Po pierwsze: Sortowanie po typie (kolejność Enum: SHARE, COMMODITY, CURRENCY)
                AssetType type1 = e1.assetDefinition.getType();
                AssetType type2 = e2.assetDefinition.getType();
                int typeComparison = type1.compareTo(type2);
                
                if (typeComparison != 0) {
                    return typeComparison;
                }
                
                // Po drugie: Sortowanie po wartości (malejąco)
                double val1 = e1.calculateValue(currentDay);
                double val2 = e2.calculateValue(currentDay);
                // compare(val2, val1) daje sortowanie malejące
                return Double.compare(val2, val1);
            }
        });

        // 3. Budowanie Stringa (Raport tekstowy)
        StringBuilder report = new StringBuilder();
        report.append(String.format("PORTFOLIO REPORT (Day %d)\n", currentDay));
        report.append("--------------------------------------------------\n");
        report.append(String.format("%-10s | %-10s | %-10s | %s\n", "TYPE", "SYMBOL", "QUANTITY", "VALUE"));
        report.append("--------------------------------------------------\n");

        for (AssetEntry entry : entries) {
            double value = entry.calculateValue(currentDay);
            // Pomijamy puste wpisy
            if (entry.getTotalQuantity() > 0) {
                report.append(String.format("%-10s | %-10s | %-10d | %.2f\n",
                        entry.assetDefinition.getType(),
                        entry.assetDefinition.getSymbol(),
                        entry.getTotalQuantity(),
                        value
                ));
            }
        }
        report.append("--------------------------------------------------\n");
        report.append(String.format("CASH: %.2f\n", this.cash));
        report.append(String.format("TOTAL NET WORTH: %.2f\n", calculateTotalValue()));

        return report.toString();
    }

    public double getCash() { return this.cash; }
    public int getHoldingsCount() { return holdings.size(); }

    public int getAssetQuantity(Asset asset) {
        if (asset == null) return 0;
        AssetEntry entry = holdings.get(asset.getSymbol());
        return entry != null ? entry.getTotalQuantity() : 0;
    }
}