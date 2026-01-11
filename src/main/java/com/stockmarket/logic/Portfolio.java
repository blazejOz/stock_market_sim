package com.stockmarket.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Locale;

import com.stockmarket.domain.*;

public class Portfolio {

    private double cash;
    private int currentDay;

    // Mapa przechowująca stan posiadania (Klucz: Symbol)
    private Map<String, AssetEntry> holdings;

    // Kolejki zleceń
    private Queue<Order> buyOrders;
    private Queue<Order> sellOrders;

    public Portfolio(double initialCash) {
        if (initialCash < 0) {
            throw new IllegalArgumentException("Initial cash cannot be negative.");
        }
        this.cash = initialCash;
        this.currentDay = 0;
        this.holdings = new HashMap<>();

        // Inicjalizacja Kolejek Priorytetowych
        this.buyOrders = new PriorityQueue<>(new Comparator<Order>() {
            @Override
            public int compare(Order o1, Order o2) {
                return Double.compare(o2.getPriceLimit(), o1.getPriceLimit());
            }
        });

        this.sellOrders = new PriorityQueue<>(new Comparator<Order>() {
            @Override
            public int compare(Order o1, Order o2) {
                return Double.compare(o1.getPriceLimit(), o2.getPriceLimit());
            }
        });
    }

    // --- Obsługa Czasu ---
    public void advanceTime(int days) {
        if (days > 0) this.currentDay += days;
    }

    // --- Obsługa Zleceń ---
    public void placeOrder(Order order) {
        if (order == null) throw new IllegalArgumentException("Order cannot be null.");

        if (order.getType() == OrderType.BUY) {
            double estimatedCost = order.getQuantity() * order.getPriceLimit();
            if (estimatedCost > cash) {
                throw new IllegalArgumentException("Not enough cash to place BUY order.");
            }
            this.cash -= estimatedCost;
            buyOrders.add(order);
        } else {
            AssetEntry entry = holdings.get(order.getSymbol());
            if (entry == null || entry.getTotalQuantity() < order.getQuantity()) {
                throw new IllegalArgumentException("Not enough assets to place SELL order.");
            }
            sellOrders.add(order);
        }
    }

    // --- Obsługa Aktywów (Add/Sell) ---

    public void addAsset(Asset asset, int quantity) {
        if (asset == null) throw new IllegalArgumentException("Asset cannot be null.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");

        double nominalCost = asset.getMarketPrice() * quantity;
        double initialCost = asset.calculateInitialCost(quantity);
        double totalCost = nominalCost + initialCost;

        if (totalCost > this.cash) {
            throw new IllegalArgumentException("Insufficient funds. Cost: " + totalCost);
        }

        // Realizacja zakupu
        this.cash -= totalCost;
        String symbol = asset.getSymbol();

        // Jeśli nie mamy tego aktywa, tworzymy wpis (bez sprawdzania limitu MAX_HOLDINGS)
        if (!holdings.containsKey(symbol)) {
            holdings.put(symbol, new AssetEntry(asset));
        }
        
        AssetEntry entry = holdings.get(symbol);
        entry.assetDefinition = asset; // Aktualizacja definicji (ceny)
        entry.addLot(this.currentDay, asset.getMarketPrice(), quantity);
    }

    public double sellAsset(String symbol, int quantity, double currentPrice) {
        if (!holdings.containsKey(symbol)) {
            throw new IllegalArgumentException("Asset not found in portfolio: " + symbol);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        AssetEntry entry = holdings.get(symbol);
        if (entry.getTotalQuantity() < quantity) {
            throw new IllegalArgumentException("Not enough asset quantity to sell.");
        }

        // FIFO
        double profit = entry.processSale(quantity, currentPrice);

        this.cash += quantity * currentPrice;

        // Jeśli sprzedaliśmy wszystko, usuwamy wpis z mapy
        if (entry.getTotalQuantity() == 0) {
            holdings.remove(symbol);
        }

        return profit;
    }

    // --- Wycena ---

    public double calculateHoldingsValue() {
        double totalValue = 0.0;
        for (AssetEntry entry : holdings.values()) {
            totalValue += entry.calculateValue(this.currentDay);
        }
        return totalValue;
    }

    public double calculateTotalValue() {
        return this.cash + calculateHoldingsValue();
    }

    // --- RAPORTOWANIE (Naprawione) ---
    public String generateReport() {
        // Kopiujemy wartości z mapy do listy, aby móc posortować
        List<AssetEntry> entries = new ArrayList<>(holdings.values());

        // Sortowanie z własnym Comparatorem
        entries.sort(new Comparator<AssetEntry>() {
            @Override
            public int compare(AssetEntry e1, AssetEntry e2) {
                // 1. Sortowanie po typie (SHARE -> COMMODITY -> CURRENCY)
                AssetType type1 = e1.assetDefinition.getType();
                AssetType type2 = e2.assetDefinition.getType();
                int typeComparison = type1.compareTo(type2);
                
                if (typeComparison != 0) {
                    return typeComparison;
                }
                
                // 2. Sortowanie po wartości (malejąco)
                double val1 = e1.calculateValue(currentDay);
                double val2 = e2.calculateValue(currentDay);
                return Double.compare(val2, val1);
            }
        });

        StringBuilder report = new StringBuilder();
        report.append(String.format("PORTFOLIO REPORT (Day %d)\n", currentDay));
        report.append("--------------------------------------------------\n");
        report.append(String.format("%-10s | %-10s | %-10s | %s\n", "TYPE", "SYMBOL", "QUANTITY", "VALUE"));
        report.append("--------------------------------------------------\n");

        for (AssetEntry entry : entries) {
            // Liczymy wartość na bieżący dzień
            double value = entry.calculateValue(currentDay);
            
            if (entry.getTotalQuantity() > 0) {
                report.append(String.format(Locale.US, "%-10s | %-10s | %-10d | %.2f\n",
                        entry.assetDefinition.getType(),
                        entry.assetDefinition.getSymbol(),
                        entry.getTotalQuantity(),
                        value
                ));
            }
        }
        report.append("--------------------------------------------------\n");
        report.append(String.format(Locale.US, "CASH: %.2f\n", this.cash));
        report.append(String.format(Locale.US, "TOTAL NET WORTH: %.2f\n", calculateTotalValue()));

        return report.toString();
    }

    // --- Metody dla I/O ---

    public void setCurrentDay(int day) { this.currentDay = day; }
    public int getCurrentDay() { return this.currentDay; }

    public void loadAsset(Asset asset, int quantity, int purchaseDay) {
        String symbol = asset.getSymbol();
        if (!holdings.containsKey(symbol)) {
            holdings.put(symbol, new AssetEntry(asset));
        }
        AssetEntry entry = holdings.get(symbol);
        entry.addLot(purchaseDay, asset.getMarketPrice(), quantity);
    }

    public String[] getHoldingsData() {
        List<String> dataList = new ArrayList<>();
        for (AssetEntry entry : holdings.values()) {
            for (PurchaseLot lot : entry.lots) {
                if (lot.getQuantity() > 0) {
                    dataList.add(String.format(Locale.US, "%s|%s|%.2f|%d|%d",
                            entry.assetDefinition.getType(),
                            entry.assetDefinition.getSymbol(),
                            lot.getUnitPrice(),
                            lot.getQuantity(),
                            lot.getPurchaseDate()
                    ));
                }
            }
        }
        return dataList.toArray(new String[0]);
    }

    // --- Gettery pomocnicze ---
    public double getCash() { return this.cash; }
    public int getHoldingsCount() { return holdings.size(); }
    
    public int getAssetQuantity(Asset asset) {
        if (asset == null) return 0;
        AssetEntry entry = holdings.get(asset.getSymbol());
        return entry != null ? entry.getTotalQuantity() : 0;
    }

    public Order peekBestBuyOrder() { return buyOrders.peek(); }
    public Order peekBestSellOrder() { return sellOrders.peek(); }

    // --- Klasa Wewnętrzna ---
    private static class AssetEntry {
        Asset assetDefinition; 
        List<PurchaseLot> lots; 

        AssetEntry(Asset asset) {
            this.assetDefinition = asset;
            this.lots = new ArrayList<>();
        }

        void addLot(long purchaseDay, double price, int quantity) {
            lots.add(new PurchaseLot(purchaseDay, price, quantity));
        }

        double processSale(int quantityToSell, double currentMarketPrice) {
            int remainingToSell = quantityToSell;
            double totalCostBase = 0.0;
            List<PurchaseLot> emptyLots = new ArrayList<>();

            for (PurchaseLot lot : lots) {
                if (remainingToSell == 0) break;

                int quantityFromLot = Math.min(remainingToSell, lot.getQuantity());
                totalCostBase += quantityFromLot * lot.getUnitPrice();

                lot.decreaseQuantity(quantityFromLot);
                remainingToSell -= quantityFromLot;

                if (lot.getQuantity() == 0) {
                    emptyLots.add(lot);
                }
            }
            lots.removeAll(emptyLots);

            double revenue = quantityToSell * currentMarketPrice;
            return revenue - totalCostBase;
        }

        double calculateValue(int currentDay) {
            double value = 0.0;
            for (PurchaseLot lot : lots) {
                if (lot.getQuantity() > 0) {
                    int daysHeld = currentDay - (int) lot.getPurchaseDate();
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
}