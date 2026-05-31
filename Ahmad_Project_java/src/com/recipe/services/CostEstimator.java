package com.recipe.services;

import com.recipe.models.RecipeIngredient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 *  Purpose
 *  Calculates the total estimated ingredient cost of a recipe
 *  and the cost per serving. Results are displayed in
 *  RecipeDetailPanel and stored in the Recipe.estimatedCost field.
 */

/*
 *  Formula:
 *   ingredientCost = quantity × pricePerUnit       
 *   totalCost      = sum of all ingredientCosts   
 *   costPerServing = totalCost ÷ servings         
 
 *  Used By  :
 *   - RecipeService.saveRecipe()    → calculates and stores cost before DB insert
 *   - RecipeDetailPanel             → displays live cost to user
 */

public class CostEstimator {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private CostEstimator() {
        throw new UnsupportedOperationException(
            "CostEstimator is a static utility class and cannot be instantiated."
        );
    }


    //  Main Cost Calculation Methods


    /**
     * Calculates the TOTAL estimated cost of ALL ingredients in a recipe.
     *
     * Formula per ingredient: cost = quantity × pricePerUnit
     * Total = sum of all ingredient costs
    
     * Called by:
     *   RecipeService.saveRecipe()   → result stored in recipe.setEstimatedCost()
     */

    public static BigDecimal estimateTotalCost(List<RecipeIngredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
        }

        BigDecimal total = BigDecimal.ZERO;

        for (RecipeIngredient ri : ingredients) {
            BigDecimal qty   = ri.getQuantity();
            BigDecimal price = ri.getPricePerUnit();

            // Handle nulls safely / treat missing values as zero
            if (qty == null)   qty   = BigDecimal.ZERO;
            if (price == null) price = BigDecimal.ZERO;

            // ingredientCost = quantity × pricePerUnit
            BigDecimal ingredientCost = qty.multiply(price);
            total = total.add(ingredientCost);
        }

        // Return total rounded to 2 decimal places
        return total.setScale(MONEY_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculates the estimated cost PER SERVING.
     *
     * Formula: costPerServing = totalCost ÷ servings
     
     * Called by:
     *   RecipeDetailPanel.displayCost()
     *   called right after estimateTotalCost().
     */

    public static BigDecimal estimateCostPerServing(BigDecimal totalCost, int servings) {
        if (totalCost == null || totalCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
        }
        if (servings <= 0) {
            throw new IllegalArgumentException(
                "Servings must be greater than 0. Got: " + servings
            );
        }

        // Divide with explicit scale and rounding to avoid ArithmeticException
        // on repeating decimals (e.g., 1 ÷ 3 = 0.33333...)
        return totalCost.divide(new BigDecimal(servings), MONEY_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculates the cost breakdown — returns a Map of each ingredient
     * name to its individual cost. Useful for displaying a detailed
     * cost breakdown in RecipeDetailPanel's "Cost" tab.
  
     * Called by: RecipeDetailPanel (optional detailed cost breakdown view)
     */

    public static Map<String, BigDecimal> getCostBreakdown(List<RecipeIngredient> ingredients) {
        Map<String, BigDecimal> breakdown = new HashMap<>();

        if (ingredients == null || ingredients.isEmpty()) {
            return breakdown;
        }

        for (RecipeIngredient ri : ingredients) {
            BigDecimal qty   = ri.getQuantity()     != null ? ri.getQuantity()     : BigDecimal.ZERO;
            BigDecimal price = ri.getPricePerUnit() != null ? ri.getPricePerUnit() : BigDecimal.ZERO;

            BigDecimal cost = qty.multiply(price).setScale(MONEY_SCALE, ROUNDING_MODE);

            // Get the ingredient name from the embedded Ingredient object
            String ingredientName = "Unknown";
            if (ri.getIngredient() != null) {
                ingredientName = ri.getIngredient().getName();
            }

            // If the same ingredient name appears twice (shouldn't normally happen),
            // add the costs together rather than overwriting
            breakdown.merge(ingredientName, cost, BigDecimal::add);
        }

        return breakdown;
    }

    /**
     * Formats a BigDecimal cost value as a display string with currency symbol.
     * Convenience method for GUI panels that need to show cost as text.
     */

    public static String formatCost(BigDecimal cost) {
        if (cost == null) {
            return "$0.00";
        }
        return "$" + cost.setScale(MONEY_SCALE, ROUNDING_MODE).toPlainString();
    }

    /*
     * Checks whether any cost data is available for a recipe.
     * Returns false if all ingredients have zero or null prices

     * Useful for RecipeDetailPanel to decide whether to show
     * "Cost not available" instead of "$0.00".
     */
    
    public static boolean hasCostData(List<RecipeIngredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return false;
        }
        for (RecipeIngredient ri : ingredients) {
            BigDecimal price = ri.getPricePerUnit();
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
    }
}
