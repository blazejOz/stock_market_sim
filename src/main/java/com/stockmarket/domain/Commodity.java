package com.stockmarket.domain;


public class Commodity extends Asset {
    private static final double STORAGE_COST_PER_UNIT = 0.50; 
    public Commodity(String symbol, double marketPrice, int quantity) {
        super(symbol, marketPrice, quantity);
    }

    
    @Override
    public double calculateRealValue() {
        double nominalValue = super.marketPrice * super.quantity;
        double totalStorageCost = super.quantity * STORAGE_COST_PER_UNIT;
        
        return nominalValue - totalStorageCost;
    }

    @Override
    public double calculateInitialCost() {
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