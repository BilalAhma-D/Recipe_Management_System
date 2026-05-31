package com.recipe.services;

import com.recipe.models.RecipeIngredient;
import com.recipe.models.Ingredient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/*
 *  Purpose:
 *  Recalculates ALL ingredient quantities when the user changes
 *  the serving count in RecipeDetailPanel.

 *  Only uses: RecipeIngredient model, BigDecimal, Java stdlib

 *  Used By  : RecipeDetailPanel (when user spins the serving count JSpinner)
 */

public class ServingScaler {

    private static final int SCALE_DECIMAL_PLACES = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private ServingScaler() {
        throw new UnsupportedOperationException(
            "ServingScaler is a static utility class and cannot be instantiated."
        );
    }


    // ===============================================================
    //  MAIN SCALING METHOD
    // ===============================================================

    /**
     * Scales ALL ingredient quantities in a list from one serving count
   
     * For each RecipeIngredient:
     *   scaledQty = originalQty × (toServings ÷ fromServings)
     
     * Called by: RecipeDetailPanel.onServingChanged(int newServings)
     */

    public static List<RecipeIngredient> scaleIngredients(
            List<RecipeIngredient> originalIngredients,
            int fromServings,
            int toServings) {

        // Validate input — cannot divide by zero
        if (fromServings <= 0) {
            throw new IllegalArgumentException(
                "fromServings must be greater than 0. Got: " + fromServings
            );
        }
        if (toServings <= 0) {
            throw new IllegalArgumentException(
                "toServings must be greater than 0. Got: " + toServings
            );
        }

        List<RecipeIngredient> scaledList = new ArrayList<>();

        for (RecipeIngredient original : originalIngredients) {
            // Create a shallow COPY of the RecipeIngredient
            RecipeIngredient scaled = copyRecipeIngredient(original);

            // Scale the quantity: newQty = oldQty × (toServings / fromServings)
            BigDecimal originalQty = original.getQuantity();
            if (originalQty == null) {
                originalQty = BigDecimal.ZERO;
            }

            BigDecimal scaledQty = scaleValue(originalQty, fromServings, toServings);
            scaled.setQuantity(scaledQty);

            scaledList.add(scaled);
        }

        return scaledList;
    }

    public static BigDecimal scaleValue(BigDecimal value, int fromServings, int toServings) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal from = new BigDecimal(fromServings);
        BigDecimal to   = new BigDecimal(toServings);

        // value × (toServings / fromServings)
        // = (value × to) / from
        // Multiply first, then divide to avoids intermediate rounding errors
        return value
                .multiply(to)
                .divide(from, SCALE_DECIMAL_PLACES, ROUNDING_MODE);
    }

    private static RecipeIngredient copyRecipeIngredient(RecipeIngredient original) {
        RecipeIngredient copy = new RecipeIngredient();

        copy.setId(original.getId());
        copy.setRecipeId(original.getRecipeId());
        copy.setIngredientId(original.getIngredientId());
        copy.setPricePerUnit(original.getPricePerUnit());
        copy.setQuantity(original.getQuantity());       // Will be overwritten with scaled value
        copy.setIngredient(original.getIngredient());   // Shared reference is safe — Ingredient is read-only here

        return copy;
    }
}
