package com.example.posexpress.model;

import androidx.annotation.Keep;

/**
 * Product Model for POSExpress.
 * Optimized for Firebase Realtime Database serialization.
 */


@Keep
public class Product {
    private int id;
    private String name;
    private double price;
    private String category;
    private String imageUrl;

    // Required empty constructor for Firebase
    public Product() {}

    public Product(int id, String name, double price, String category, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(Object price) {
        if (price instanceof Number) {
            this.price = ((Number) price).doubleValue();
        } else if (price instanceof String) {
            try {
                this.price = Double.parseDouble((String) price);
            } catch (NumberFormatException e) {
                this.price = 0.0;
            }
        }
    }

    public String getCategory() { return category; }
    public void setCategory(Object category) {
        if (category == null) {
            this.category = "General";
        } else {
            this.category = String.valueOf(category);
        }
    }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
