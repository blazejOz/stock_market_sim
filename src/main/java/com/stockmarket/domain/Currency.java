package com.stockmarket.domain;

public class Currency extends Asset {
    private static final double SPREAD_RATE = 0.005;

    public Currency(String symbol, double marketPrice) {
        super(symbol, marketPrice);
    }

    @Override
    public double calculateRealValue(int quantity, long daysHeld) {
        double nominalValue = super.marketPrice * quantity;
        double totalSpreadCost = nominalValue * SPREAD_RATE;
        
        return nominalValue - totalSpreadCost;
    }

    @Override
    public double calculateInitialCost(int quantity) {
        // Initial cost is the spread applied to the full transaction value
        double nominalValue = super.marketPrice * quantity;
        return nominalValue * SPREAD_RATE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Currency currency = (Currency) o;
        return super.symbol.equals(currency.symbol);
    }

    @Override
    public int hashCode() {
        return super.symbol.hashCode();
    }

     @Override
    public AssetType getType() {
        return AssetType.CURRENCY;
    }

}