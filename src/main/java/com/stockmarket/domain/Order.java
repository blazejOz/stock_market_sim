package com.stockmarket.domain;

public class Order {
    private final String symbol;
    private final AssetType assetType;
    private final double priceLimit; // Cena, po której chcemy kupić/sprzedać
    private final int quantity;
    private final OrderType type;

    public Order(String symbol, AssetType assetType, double priceLimit, int quantity, OrderType type) {
        if (priceLimit <= 0 || quantity <= 0) {
            throw new IllegalArgumentException("Price and quantity must be positive.");
        }
        this.symbol = symbol;
        this.assetType = assetType;
        this.priceLimit = priceLimit;
        this.quantity = quantity;
        this.type = type;
    }

    public String getSymbol() { return symbol; }
    public double getPriceLimit() { return priceLimit; }
    public int getQuantity() { return quantity; }
    public OrderType getType() { return type; }
    public AssetType getAssetType() { return assetType; }

    @Override
    public String toString() {
        return String.format("%s %s %s @ %.2f", type, assetType, symbol, priceLimit);
    }
}