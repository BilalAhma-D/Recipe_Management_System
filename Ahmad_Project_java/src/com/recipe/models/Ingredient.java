package com.recipe.models;

public class Ingredient {

    private int id;
    private String name;
    private String unit;
    private boolean allergenFlag;

    // Constructers

    public Ingredient() {
        this.allergenFlag = false;
    }

    public Ingredient(String name, String unit, boolean allergenFlag) {
        this.name        = name;
        this.unit        = unit;
        this.allergenFlag = allergenFlag;
    }

    // Constructor for loading from DB — includes the DB-assigned id.
    
    public Ingredient(int id, String name, String unit, boolean allergenFlag) {
        this.id          = id;
        this.name        = name;
        this.unit        = unit;
        this.allergenFlag = allergenFlag;
    }

    // Getters

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isAllergen() {
        return allergenFlag;
    }

    // Setters

    //  Sets the DB-generated ID.
    //  Called by RecipeDAO after getGeneratedKeys() on INSERT.

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setAllergenFlag(boolean allergenFlag) {
        this.allergenFlag = allergenFlag;
    }

    // Other Methods
 
    public String toString() {
        return "Ingredient["
                + "id=" + id
                + ", name='" + name + "'"
                + ", unit='" + unit + "'"
                + ", allergen=" + allergenFlag
                + "]";
    }

    
    // Two Ingredient objects are equal if they have the same DB id.
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Ingredient)) return false;
        Ingredient other = (Ingredient) obj;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
