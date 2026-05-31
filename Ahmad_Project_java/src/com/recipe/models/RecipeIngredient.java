package com.recipe.models;

import java.math.BigDecimal;


//  Purpose:
//  Junction / bridge object that links a Recipe to an Ingredient

//  Used By  :
//   - RecipeDAO          reads/writes recipe_ingredients rows
//   - ServingScaler      reads quantity, writes scaled quantity (static methods)
//   - CostEstimator      reads quantity × pricePerUnit (static methods)
//   - AddRecipePanel     builds list of these when user fills ingredient form
//   - RecipeDetailPanel  displays ingredient list, checks allergenFlag
//   - Uzair's GroceryListBuilder → merges quantities for weekly grocery list

public class RecipeIngredient {

    private int id;
    private int recipeId;
    private int ingredientId;
    private BigDecimal quantity;
    private BigDecimal pricePerUnit;
    private Ingredient ingredient;

    // Costructor
    
    public RecipeIngredient() {
        this.quantity     = BigDecimal.ZERO;
        this.pricePerUnit = BigDecimal.ZERO;
    }

    public RecipeIngredient(int ingredientId,
                            BigDecimal quantity,
                            BigDecimal pricePerUnit) {
        this.ingredientId  = ingredientId;
        this.quantity      = quantity;
        this.pricePerUnit  = pricePerUnit;
    }

    public RecipeIngredient(int id, int recipeId, int ingredientId,
                            BigDecimal quantity, BigDecimal pricePerUnit) {
        this.id            = id;
        this.recipeId      = recipeId;
        this.ingredientId  = ingredientId;
        this.quantity      = quantity;
        this.pricePerUnit  = pricePerUnit;
    }

    // Getters

    public int getId() {
        return id;
    }

    public int getRecipeId() {
        return recipeId;
    }
    
    public int getIngredientId() {
        return ingredientId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    // setters

    public void setId(int id) {
        this.id = id;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }

    public void setIngredientId(int ingredientId) {
        this.ingredientId = ingredientId;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    // Sets the full Ingredient object (loaded via JOIN in RecipeDAO).
    // Allows GUI to call ri.getIngredient().getName() without extra DB calls.
     
    public void setIngredient(Ingredient ingredient) {
        this.ingredient  = ingredient;
        // Keep ingredientId in sync
        if (ingredient != null) {
            this.ingredientId = ingredient.getId();
        }
    }

    @Override
    public String toString() {
        String ingredientDisplay = (ingredient != null)
                ? "'" + ingredient.getName() + "'"
                : "id=" + ingredientId;
        return "RecipeIngredient["
                + "recipeId=" + recipeId
                + ", ingredient=" + ingredientDisplay
                + ", qty=" + quantity
                + ", price/unit=" + pricePerUnit
                + "]";
    }
}
