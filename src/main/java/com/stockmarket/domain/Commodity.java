package com.stockmarket.domain;

public class Commodity extends Asset {
    private static final double STORAGE_COST_PER_UNIT = 0.50;

    public Commodity(String symbol, double marketPrice) {
        super(symbol, marketPrice);
    }

    @Override
    public double calculateRealValue(int quantity, int daysHeld) {
        double nominalValue = super.marketPrice * quantity;
        int daysCharged = Math.max(1, daysHeld);
        double totalStorageCost = quantity * STORAGE_COST_PER_UNIT * daysCharged;
        
        return nominalValue - totalStorageCost;
    }

    @Override
    public double calculateInitialCost(int quantity) {
        return 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Commodity that = (Commodity) o;
        return super.symbol.equals(that.symbol);
    }

    @Override
    public int hashCode() {
        return super.symbol.hashCode();
    }
}