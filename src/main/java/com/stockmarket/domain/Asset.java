package com.stockmarket.domain;


public abstract class Asset {
    protected String symbol;
    protected double marketPrice;
    protected int quantity;

    public Asset(String symbol, double marketPrice, int quantity) {
        validateSymbol(symbol);
        validatePrice(marketPrice);
        validateQuantity(quantity);

        this.symbol = symbol;
        this.marketPrice = marketPrice;
        this.quantity = quantity;
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

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Asset quantity must be positive.");
        }
    }

    public String getSymbol() {
        return symbol;
    }

    public double getMarketPrice() {
        return marketPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    // Metoda abstrakcyjna: Wymusza obliczenie wartości rzeczywistej (po odliczeniu kosztów)
    public abstract double calculateRealValue();

    // Metoda polimorficzna: Oblicza opłatę początkową przy zakupie (np. prowizja, spread)
    public abstract double calculateInitialCost();

    // Metoda polimorficzna: Sprawdza, czy aktywo jest identyczne (dla Portfolio.addAsset)
    @Override
    public abstract boolean equals(Object o);

    // Wymagane, gdy nadpisujemy equals
    @Override
    public abstract int hashCode();
}