package com.resturant.menu.Model;

public class Drink extends Product {
    private boolean isCold;

    public Drink(String name, double price, String description, boolean isCold) {
        super(name, price, description);
        this.isCold = isCold;
    }

    public boolean isCold() {
        return isCold;
    }

    public void setCold(boolean cold) {
        isCold = cold;
    }
}
