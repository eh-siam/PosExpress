package com.example.posexpress.model;

import androidx.annotation.Keep;

/**
 * Category Model for POSExpress.
 */
@Keep
public class Category {
    private String id;
    private String name;

    // Required empty constructor for Firebase
    public Category() {}

    public Category(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name;
    }
}
