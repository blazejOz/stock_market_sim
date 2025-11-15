package com.stockmarket;

public class Portfolio {

    private static final int MAX_HOLDINGS = 10;
    
    private double cash;
    private StockHolding[] holdings;
    private int holdingsCount;

    public Portfolio(double initialCash) {
        validateInitialCash(initialCash);

        this.cash = initialCash;
        this.holdings = new StockHolding[MAX_HOLDINGS];
        this.holdingsCount = 0;
    }

    private void validateInitialCash(double initialCash) {
        if (initialCash < 0) {
            throw new IllegalArgumentException("Initial cash cannot be negative.");
        }
    }

    private static class StockHolding {
        Stock stock;
        int quantity;

        StockHolding(Stock stock, int quantity) {
            validateStock(stock);
            validateQuantity(quantity);

            this.stock = stock;
            this.quantity = quantity;
        }
    }

    private static void validateStock(Stock stock) {
        if (stock == null) {
            throw new IllegalArgumentException("Stock cannot be null.");
        }
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
    }

    public void addStock(Stock stock, int quantity) {
        validateStock(stock);
        validateQuantity(quantity);
        
        //Check if stock already exists
        for (int i = 0; i < holdingsCount; i++) {
            if (holdings[i].stock.equals(stock)) {
                holdings[i].quantity += quantity;
                return;
            }
        }
        
        //Add new stock holding
        if (holdingsCount < MAX_HOLDINGS) {
            holdings[holdingsCount] = new StockHolding(stock, quantity);
            holdingsCount++;
        } else {
            System.out.println("Portfolio is full. Cannot add new stock type: " + stock.getSymbol());
        }
    }

    public double calculateStockValue() {
        double totalStockValue = 0.0;
        for (int i = 0; i < holdingsCount; i++) {
            StockHolding holding = holdings[i];
            totalStockValue += holding.quantity * holding.stock.getPrice();
        }
        return totalStockValue;
    }

    public double calculateTotalValue() {
        return this.cash + calculateStockValue();
    }


    // Getters
    public double getCash() {
        return this.cash;
    }
    
    public int getHoldingsCount() {
        return this.holdingsCount;
    }

    public int getStockQuantity(Stock stock) {
        validateStock(stock);
        for (int i = 0; i < holdingsCount; i++) {
            if (holdings[i].stock.equals(stock)) {
                return holdings[i].quantity;
            }
        }
        return 0;
    }
}