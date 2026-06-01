package com.recipe.services;

import com.recipe.database.MealPlanDAO;
import com.recipe.database.NutritionDAO;
import com.recipe.models.MealPlan;
import com.recipe.models.Nutrition;
import java.time.LocalDate;
import java.util.List;

// ============================================================
//  FILE    : NutritionCalculator.java
//  AUTHOR  : Uzair
//  PURPOSE : Business logic for nutrition data.
//            Reads rows via NutritionDAO and does the arithmetic.
//            No SQL here, no GUI here — pure calculation logic.
// ============================================================
//
//  LAYER:
//    MealPlannerPanel  →  NutritionCalculator  →  NutritionDAO  →  DB

public class NutritionCalculator {

    private final NutritionDAO nutritionDAO;
    private final MealPlanDAO  mealPlanDAO;

    public NutritionCalculator() {
        this.nutritionDAO = new NutritionDAO();
        this.mealPlanDAO  = new MealPlanDAO();
    }

    // --------------------------------------------------------
    //  getNutrition()
    //  Returns nutrition data for ONE recipe, or null if not set yet.
    // --------------------------------------------------------
    public Nutrition getNutrition(int recipeId) {
        return nutritionDAO.getNutritionByRecipeId(recipeId);
    }

    // --------------------------------------------------------
    //  saveNutrition()
    //  Inserts or updates nutrition data for a recipe.
    //  Checks if a row already exists first to decide INSERT vs UPDATE.
    // --------------------------------------------------------
    public boolean saveNutrition(Nutrition nutrition) {
        Nutrition existing = nutritionDAO.getNutritionByRecipeId(nutrition.getRecipeId());
        if (existing != null) {
            return nutritionDAO.updateNutrition(nutrition);
        } else {
            return nutritionDAO.insertNutrition(nutrition);
        }
    }

    // --------------------------------------------------------
    //  calculateWeekTotals()
    //  Sums ALL macros across every recipe planned for a given week.
    //  Returns a Nutrition object with combined totals.
    //  recipeId = -1 in the result signals "this is a summary, not a real recipe row".
    // --------------------------------------------------------
    public Nutrition calculateWeekTotals(int userId, LocalDate weekStartDate) {
        List<MealPlan> plan = mealPlanDAO.getMealPlanByUser(userId, weekStartDate);

        double totalCal = 0, totalProtein = 0, totalCarbs = 0, totalFat = 0;

        for (MealPlan entry : plan) {
            Nutrition n = nutritionDAO.getNutritionByRecipeId(entry.getRecipeId());
            if (n != null) {
                totalCal     += n.getCalories();
                totalProtein += n.getProteinG();
                totalCarbs   += n.getCarbsG();
                totalFat     += n.getFatG();
            }
        }

        return new Nutrition(-1, -1, totalCal, totalProtein, totalCarbs, totalFat);
    }
}
