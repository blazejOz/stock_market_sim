package com.stockmarket.domain;


public class Currency extends Asset {
    private static final double SPREAD_RATE = 0.005; 

    public Currency(String symbol, double marketPrice, int quantity) {
        super(symbol, marketPrice, quantity);
    }

   
    @Override
    public double calculateRealValue() {
        double nominalValue = super.marketPrice * super.quantity;
        double totalSpreadCost = nominalValue * SPREAD_RATE;
        
        return nominalValue - totalSpreadCost;
    }

   
    @Override
    public double calculateInitialCost() {
        return (super.marketPrice * super.quantity) * SPREAD_RATE;
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
}