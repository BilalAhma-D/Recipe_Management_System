package com.recipe.models;

/**
 * ============================================================
 *  FILE        : Nutrition.java
 *  PACKAGE     : com.recipe.models
 *  AUTHOR      : Uzair
 *  MODULE      : Meal Planning & Nutrition
 * ============================================================
 *
 *  WHAT THIS CLASS IS:
 *  -------------------
 *  A plain Model class that mirrors ONE row in the NUTRITION table.
 *  It holds data only — no DB logic, no GUI logic.
 *
 *  FIELDS MAP TO DB COLUMNS:
 *   id         ←→  nutrition.nutrition_id  (INT, Primary Key)
 *   recipeId   ←→  nutrition.recipe_id     (FK → recipes.recipe_id)
 *   calories   ←→  nutrition.calories      (DOUBLE)
 *   proteinG   ←→  nutrition.protein_g     (DOUBLE)
 *   carbsG     ←→  nutrition.carbs_g       (DOUBLE)
 *   fatG       ←→  nutrition.fat_g         (DOUBLE)
 *
 *  VIVA TIP:
 *  ---------
 *  "Nutrition is a pure Model class — encapsulation only.
 *   NutritionDAO reads/writes it; NutritionCalculator uses it
 *   for arithmetic; no other layer ever touches the DB directly."
 */
public class Nutrition {

    private int    id;
    private int    recipeId;
    private double calories;
    private double proteinG;
    private double carbsG;
    private double fatG;

    // ---- Constructors ----

    public Nutrition() {}

    public Nutrition(int id, int recipeId,
                     double calories, double proteinG,
                     double carbsG, double fatG) {
        this.id       = id;
        this.recipeId = recipeId;
        this.calories = calories;
        this.proteinG = proteinG;
        this.carbsG   = carbsG;
        this.fatG     = fatG;
    }

    // ---- Getters ----

    public int    getId()       { return id; }
    public int    getRecipeId() { return recipeId; }
    public double getCalories() { return calories; }
    public double getProteinG() { return proteinG; }
    public double getCarbsG()   { return carbsG; }
    public double getFatG()     { return fatG; }

    // ---- Setters ----

    public void setId(int id)             { this.id = id; }
    public void setRecipeId(int recipeId) { this.recipeId = recipeId; }
    public void setCalories(double c)     { this.calories = c; }
    public void setProteinG(double p)     { this.proteinG = p; }
    public void setCarbsG(double c)       { this.carbsG = c; }
    public void setFatG(double f)         { this.fatG = f; }

    @Override
    public String toString() {
        return "Nutrition{recipeId=" + recipeId
                + ", cal=" + calories + ", protein=" + proteinG
                + "g, carbs=" + carbsG + "g, fat=" + fatG + "g}";
    }
}
