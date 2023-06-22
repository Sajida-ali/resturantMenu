package com.resturant.menu.Model;

public class Meal extends Product {
    private boolean isVegan;

    public Meal(String name, double price, String description, boolean isVegan) {
        super(name, price, description);
        this.isVegan = isVegan;
    }

    public boolean isVegan() {
        return isVegan;
    }

    public void setVegan(boolean vegan) {
        isVegan = vegan;
    }
}
