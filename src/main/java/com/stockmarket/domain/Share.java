package com.stockmarket.domain;

public class Share extends Asset {
    
    private static final double SMALL_TRANSACTION_THRESHOLD = 1000; 
    private static final double TRANSACTION_FEE = 5.0; 

    public Share(String symbol, double marketPrice, int quantity) {
        super(symbol, marketPrice, quantity);
    }

    
    @Override
    public double calculateRealValue() {
        double nominalValue = super.marketPrice * super.quantity;
       
        return nominalValue - calculateInitialCost();
    }

    @Override
    public double calculateInitialCost() {
        double transactionValue = super.quantity * super.marketPrice;
        
        if (transactionValue < SMALL_TRANSACTION_THRESHOLD){
            return TRANSACTION_FEE;
        } else {
            return 0.0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Share share = (Share) o;
        return super.symbol.equals(share.symbol);
    }

    @Override
    public int hashCode() {
        return super.symbol.hashCode();
    }
}