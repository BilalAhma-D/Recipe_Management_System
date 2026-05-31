package com.recipe.models;

// ============================================================
//  FILE    : Nutrition.java
//  AUTHOR  : Uzair
//  PURPOSE : Mirrors ONE row in the nutrition table.
//            Holds data only — no SQL, no GUI.
// ============================================================
//
//  DB TABLE (run in pgAdmin before starting the app):
//    CREATE TABLE nutrition (
//        nutrition_id SERIAL PRIMARY KEY,
//        recipe_id    INT REFERENCES recipes(id),
//        calories     DOUBLE PRECISION DEFAULT 0,
//        protein_g    DOUBLE PRECISION DEFAULT 0,
//        carbs_g      DOUBLE PRECISION DEFAULT 0,
//        fat_g        DOUBLE PRECISION DEFAULT 0
//    );

public class Nutrition {

    private int    nutritionId;  // DB primary key
    private int    recipeId;     // FK → recipes.id  (Bilal's table uses "id" not "recipe_id")
    private double calories;
    private double proteinG;
    private double carbsG;
    private double fatG;

    // ---- No-arg constructor (needed by NutritionDAO) ----
    public Nutrition() {}

    // ---- Full constructor (used in NutritionDAO when reading from DB) ----
    public Nutrition(int nutritionId, int recipeId,
                     double calories, double proteinG,
                     double carbsG,   double fatG) {
        this.nutritionId = nutritionId;
        this.recipeId    = recipeId;
        this.calories    = calories;
        this.proteinG    = proteinG;
        this.carbsG      = carbsG;
        this.fatG        = fatG;
    }

    // ---- Getters ----
    public int    getNutritionId() { return nutritionId; }
    public int    getRecipeId()    { return recipeId; }
    public double getCalories()    { return calories; }
    public double getProteinG()    { return proteinG; }
    public double getCarbsG()      { return carbsG; }
    public double getFatG()        { return fatG; }

    // ---- Setters ----
    public void setNutritionId(int id)       { this.nutritionId = id; }
    public void setRecipeId(int recipeId)    { this.recipeId    = recipeId; }
    public void setCalories(double calories) { this.calories    = calories; }
    public void setProteinG(double proteinG) { this.proteinG    = proteinG; }
    public void setCarbsG(double carbsG)     { this.carbsG      = carbsG; }
    public void setFatG(double fatG)         { this.fatG        = fatG; }

    @Override
    public String toString() {
        return "Nutrition{recipeId=" + recipeId + ", cal=" + calories
             + ", protein=" + proteinG + "g, carbs=" + carbsG
             + "g, fat=" + fatG + "g}";
    }
}
