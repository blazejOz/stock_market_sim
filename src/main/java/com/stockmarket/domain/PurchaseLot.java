package com.stockmarket.domain;

public class PurchaseLot {
    private final long purchaseDate; 
    private final double unitPrice;
    private int quantity; 

    public PurchaseLot(long purchaseDate, double unitPrice, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        this.purchaseDate = purchaseDate;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public long getPurchaseDate() {
        return purchaseDate;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void decreaseQuantity(int amount) {
        if (amount > quantity) {
            throw new IllegalArgumentException("Cannot remove more than exists in lot");
        }
        this.quantity -= amount;
    }
}