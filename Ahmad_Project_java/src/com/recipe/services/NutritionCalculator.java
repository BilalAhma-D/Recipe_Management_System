package com.recipe.services;

import com.recipe.database.MealPlanDAO;
import com.recipe.database.NutritionDAO;
import com.recipe.models.MealPlan;
import com.recipe.models.Nutrition;

import java.time.LocalDate;
import java.util.List;

/**
 * ============================================================
 *  FILE        : NutritionCalculator.java
 *  PACKAGE     : com.recipe.services
 *  AUTHOR      : Uzair
 *  MODULE      : Meal Planning & Nutrition
 * ============================================================
 *
 *  WHAT THIS CLASS DOES:
 *  ----------------------
 *  Service class for all nutrition logic.
 *
 *  It reads nutrition rows from NutritionDAO and does arithmetic
 *  to answer questions like:
 *    "How many calories total for Monday?"
 *    "What is the macro breakdown for this week?"
 *
 *  LAYER:
 *    MealPlannerPanel / NutritionPanel  →  NutritionCalculator
 *       →  NutritionDAO  →  PostgreSQL
 *
 *  VIVA TIP:
 *  ---------
 *  "NutritionCalculator is a pure service — no GUI code, no SQL.
 *   It delegates DB work to NutritionDAO and MealPlanDAO, then
 *   performs the arithmetic in Java. This makes the logic easy
 *   to unit-test without needing a database."
 */
public class NutritionCalculator {

    private final NutritionDAO nutritionDAO;
    private final MealPlanDAO  mealPlanDAO;

    public NutritionCalculator() {
        this.nutritionDAO = new NutritionDAO();
        this.mealPlanDAO  = new MealPlanDAO();
    }


    // -------------------------------------------------------
    //  getNutrition()
    // -------------------------------------------------------

    /**
     * Returns the Nutrition object for a single recipe.
     * Returns null if no nutrition data has been entered for that recipe.
     *
     * @param recipeId The recipe to look up
     * @return Nutrition object or null
     */
    public Nutrition getNutrition(int recipeId) {
        return nutritionDAO.getNutritionByRecipeId(recipeId);
    }


    // -------------------------------------------------------
    //  saveNutrition()
    // -------------------------------------------------------

    /**
     * Saves (or updates) nutrition info for a recipe.
     * If a row already exists it is updated; otherwise a new row is inserted.
     *
     * @param nutrition A Nutrition object with all fields filled in
     * @return true if saved successfully
     */
    public boolean saveNutrition(Nutrition nutrition) {
        // Check if a row already exists
        Nutrition existing = nutritionDAO.getNutritionByRecipeId(nutrition.getRecipeId());
        if (existing != null) {
            return nutritionDAO.updateNutrition(nutrition);
        } else {
            return nutritionDAO.insertNutrition(nutrition);
        }
    }


    // -------------------------------------------------------
    //  calculateDayTotal()
    // -------------------------------------------------------

    /**
     * Sums the calories for all recipes planned on a given day.
     *
     * HOW IT WORKS:
     *   1. Load the full week plan from MealPlanDAO
     *   2. Filter entries where dayOfWeek matches the given day
     *   3. For each matching entry, load its nutrition and sum calories
     *
     * @param userId        The logged-in user
     * @param weekStartDate The Monday of the week
     * @param dayOfWeek     E.g. "Monday", "Tuesday", etc.
     * @return Total calories for that day (0.0 if no data)
     */
    public double calculateDayTotal(int userId, LocalDate weekStartDate, String dayOfWeek) {

        List<MealPlan> plan = mealPlanDAO.getMealPlanByUser(userId, weekStartDate);
        double total = 0.0;

        for (MealPlan entry : plan) {
            if (entry.getDayOfWeek().equalsIgnoreCase(dayOfWeek)) {
                Nutrition n = nutritionDAO.getNutritionByRecipeId(entry.getRecipeId());
                if (n != null) {
                    total += n.getCalories();
                }
            }
        }

        return total;
    }


    // -------------------------------------------------------
    //  calculateWeekTotals()
    // -------------------------------------------------------

    /**
     * Returns a Nutrition object whose fields represent the SUM
     * of all macros for the entire week's plan.
     *
     * Useful for the weekly summary card in MealPlannerPanel.
     *
     * @param userId        The logged-in user
     * @param weekStartDate The Monday of the week
     * @return A Nutrition object with summed values (recipeId = -1 as a sentinel)
     */
    public Nutrition calculateWeekTotals(int userId, LocalDate weekStartDate) {

        List<MealPlan> plan = mealPlanDAO.getMealPlanByUser(userId, weekStartDate);

        double totalCal     = 0;
        double totalProtein = 0;
        double totalCarbs   = 0;
        double totalFat     = 0;

        for (MealPlan entry : plan) {
            Nutrition n = nutritionDAO.getNutritionByRecipeId(entry.getRecipeId());
            if (n != null) {
                totalCal     += n.getCalories();
                totalProtein += n.getProteinG();
                totalCarbs   += n.getCarbsG();
                totalFat     += n.getFatG();
            }
        }

        // recipeId = -1 signals this is an aggregate, not a single recipe
        return new Nutrition(-1, -1, totalCal, totalProtein, totalCarbs, totalFat);
    }
}
