package com.stockmarket.domain;

public abstract class Asset {
    protected String symbol;
    protected double marketPrice;

    public Asset(String symbol, double marketPrice) {
        validateSymbol(symbol);
        validatePrice(marketPrice);

        this.symbol = symbol;
        this.marketPrice = marketPrice;
    }

    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset symbol cannot be empty.");
        }
    }

    private void validatePrice(double price) {
        if (price <= 0) {
            throw new IllegalArgumentException("Asset price must be positive.");
        }
    }

    public String getSymbol() {
        return symbol;
    }

    public double getMarketPrice() {
        return marketPrice;
    }

    public abstract double calculateRealValue(int quantity, long daysHeld);

    public abstract double calculateInitialCost(int quantity);

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}