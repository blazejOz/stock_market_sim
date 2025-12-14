package com.stockmarket.logic;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.stockmarket.domain.Asset;

public class Portfolio {

    private static final int MAX_HOLDINGS = 10;

    private double cash;
    private AssetHolding[] holdings;
    private int holdingsCount;

    public Portfolio(double initialCash) {
        validateInitialCash(initialCash);

        this.cash = initialCash;
        this.holdings = new AssetHolding[MAX_HOLDINGS];
        this.holdingsCount = 0;
    }

    private void validateInitialCash(double initialCash) {
        if (initialCash < 0) {
            throw new IllegalArgumentException("Initial cash cannot be negative.");
        }
    }

    private static class AssetHolding {
        final Asset asset;
        int quantity;
        final LocalDate purchaseDate;

        AssetHolding(Asset asset, int quantity) {
            if (asset == null) throw new IllegalArgumentException("Asset cannot be null.");
            if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");
            this.asset = asset;
            this.quantity = quantity;
            this.purchaseDate = LocalDate.now();
        }
    }

    public void addAsset(Asset asset, int quantity) {
        if (asset == null) throw new IllegalArgumentException("Asset cannot be null.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");
        
       
        double nominalCost = asset.getMarketPrice() * quantity;
        double initialCost = asset.calculateInitialCost(quantity);
        double required = nominalCost + initialCost;

        if (required > this.cash) {
            throw new IllegalStateException("Insufficient funds to acquire asset. Required cost: " + required);
        }

       //if present add quantity
        for (int i = 0; i < holdingsCount; i++) {
            if (holdings[i].asset.getSymbol().equals(asset.getSymbol())) { 
                holdings[i].quantity += quantity;
                this.cash -= required;
                return;
            }
        }

        // Add new holding
        if (holdingsCount < MAX_HOLDINGS) {
            holdings[holdingsCount] = new AssetHolding(asset, quantity);
            holdingsCount++;
            this.cash -= required;
        } else {
            throw new IllegalStateException("Portfolio limit reached. Cannot add a new unique asset type.");
        }
    }

    // Audyt Portfela
    public double calculateHoldingsValue() {
        double total = 0.0;
        LocalDate now = LocalDate.now();

        for (int i = 0; i < holdingsCount; i++) {
            AssetHolding h = holdings[i];
            int daysHeld = (int)ChronoUnit.DAYS.between(h.purchaseDate, now);
            total += h.asset.calculateRealValue(h.quantity, daysHeld);
        }
        return total;
    }

    public double calculateTotalValue() {
        return this.cash + calculateHoldingsValue();
    }

    public double getCash() { return this.cash; }
    public int getHoldingsCount() { return this.holdingsCount; }

    public int getAssetQuantity(Asset asset) {
        if (asset == null) throw new IllegalArgumentException("Asset cannot be null.");
        
        for (int i = 0; i < holdingsCount; i++) {
            if (holdings[i].asset.getSymbol().equals(asset.getSymbol())) {
                return holdings[i].quantity;
            }
        }
        return 0;
    }
}