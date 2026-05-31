package com.recipe.services;

/**
 * Utility that converts recipe complexity inputs into one of three difficulty levels.
 */
public class DifficultyBadgeCalc {

    public static String calculate(int stepCount, int ingredientCount, int cookTimeMinutes) {
        int score = stepCount + ingredientCount + (cookTimeMinutes / 10);

        if (score <= 10) {
            return "Easy";
        }
        if (score <= 20) {
            return "Medium";
        }
        return "Hard";
    }
}
