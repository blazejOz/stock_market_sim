package com.stockmarket.domain;

public class Share extends Asset {
    private static final double SMALL_TRANSACTION_THRESHOLD = 1000; 
    private static final double TRANSACTION_FEE = 5.0; 

    public Share(String symbol, double marketPrice) {
        super(symbol, marketPrice);
    }

    @Override
    public double calculateRealValue(int quantity) {
        double nominalValue = super.marketPrice * quantity;
       
        return nominalValue - calculateInitialCost(quantity);
    }

    @Override
    public double calculateInitialCost(int quantity) {
        double transactionValue = quantity * super.marketPrice;
        
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