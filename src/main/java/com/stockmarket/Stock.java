package com.stockmarket;

import java.util.Objects;

public class Stock {
    private String symbol;
    private String name;
    private double price;

    public Stock(String symbol, String name, double price) {
 
        validateSymbol(symbol);
        validateName(name);
        validatePrice(price);

        this.symbol = symbol.trim().toUpperCase();
        this.name = name.trim();
        this.price = price;
    }

// Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        validatePrice(price); 
        this.price = price;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return Objects.equals(symbol, stock.symbol);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }

// Validation Methods
    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock symbol cannot be empty.");
        }
        if (symbol.trim().length() > 5) {
            throw new IllegalArgumentException("Stock symbol cannot be longer than 5 characters.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock name cannot be empty.");
        }
    }

    private void validatePrice(double price) {
        if (price < 0) {
            throw new IllegalArgumentException("Stock price must be non-negative.");
        }
    }
}