package com.recipe.services;

import com.recipe.database.MealPlanDAO;
import com.recipe.models.MealPlan;
import com.recipe.models.User;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * ============================================================
 *  FILE        : MealPlanService.java
 *  PACKAGE     : com.recipe.services
 *  AUTHOR      : Uzair
 *  MODULE      : Meal Planning & Nutrition
 * ============================================================
 *
 *  WHAT THIS CLASS DOES:
 *  ----------------------
 *  Business logic for meal planning.
 *
 *  It sits between the GUI (MealPlannerPanel) and the DAO
 *  (MealPlanDAO). The GUI never talks to the DAO directly.
 *
 *  LAYER:
 *    MealPlannerPanel  →  MealPlanService  →  MealPlanDAO  →  PostgreSQL
 *
 *  VIVA TIP:
 *  ---------
 *  "MealPlanService validates inputs and derives the correct
 *   weekStartDate automatically using Java's TemporalAdjusters
 *   so the GUI does not need to know about date arithmetic."
 */
public class MealPlanService {

    private final MealPlanDAO mealPlanDAO;

    // Valid constants — used for validation
    public static final String[] DAYS  = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
    public static final String[] MEALS = { "Breakfast", "Lunch", "Dinner" };


    public MealPlanService() {
        this.mealPlanDAO = new MealPlanDAO();
    }


    // -------------------------------------------------------
    //  assignRecipe()
    // -------------------------------------------------------

    /**
     * Assigns a recipe to a specific day + meal slot for the current user.
     *
     * Automatically calculates the weekStartDate (most recent Monday)
     * for whichever week the given date falls in.
     *
     * @param user       The currently logged-in user (from SessionManager)
     * @param recipeId   The recipe to assign
     * @param dayOfWeek  e.g. "Monday"
     * @param mealType   e.g. "Lunch"
     * @param anyDateInWeek Any date within the desired week (usually today)
     * @return "SUCCESS" or an error message string
     */
    public String assignRecipe(User user, int recipeId,
                                String dayOfWeek, String mealType,
                                LocalDate anyDateInWeek) {

        if (user == null)  return "Not logged in.";
        if (recipeId <= 0) return "Invalid recipe selected.";
        if (!isValidDay(dayOfWeek))  return "Invalid day: " + dayOfWeek;
        if (!isValidMeal(mealType))  return "Invalid meal type: " + mealType;

        // Derive the Monday of the given week
        LocalDate weekStart = anyDateInWeek
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        MealPlan plan = new MealPlan();
        plan.setUserId(user.getId());
        plan.setRecipeId(recipeId);
        plan.setDayOfWeek(dayOfWeek);
        plan.setMealType(mealType);
        plan.setWeekStartDate(weekStart);

        boolean saved = mealPlanDAO.insertMealPlanEntry(plan);
        return saved ? "SUCCESS" : "Failed to save meal plan. Please try again.";
    }


    // -------------------------------------------------------
    //  getWeekPlan()
    // -------------------------------------------------------

    /**
     * Loads all meal plan entries for the user's current week.
     *
     * @param user          The logged-in user
     * @param anyDateInWeek Any date in the target week
     * @return List of MealPlan objects (with recipe names pre-filled)
     */
    public List<MealPlan> getWeekPlan(User user, LocalDate anyDateInWeek) {

        if (user == null) return List.of(); // Empty list — not logged in

        LocalDate weekStart = anyDateInWeek
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        return mealPlanDAO.getMealPlanByUser(user.getId(), weekStart);
    }


    // -------------------------------------------------------
    //  removeMeal()
    // -------------------------------------------------------

    /**
     * Clears a specific meal slot (user clicked "Remove" on a cell).
     *
     * @param user          The logged-in user
     * @param dayOfWeek     The day to clear
     * @param mealType      The meal slot to clear
     * @param anyDateInWeek Any date in the target week
     * @return "SUCCESS" or an error message
     */
    public String removeMeal(User user, String dayOfWeek,
                              String mealType, LocalDate anyDateInWeek) {

        if (user == null) return "Not logged in.";

        LocalDate weekStart = anyDateInWeek
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        boolean deleted = mealPlanDAO.deleteMealPlanEntry(
                user.getId(), dayOfWeek, mealType, weekStart);

        return deleted ? "SUCCESS" : "Nothing to remove.";
    }


    // -------------------------------------------------------
    //  getCurrentWeekStart()
    // -------------------------------------------------------

    /**
     * Convenience method — returns the Monday of the current week.
     * Used by the GUI to determine the default week to display.
     *
     * @return Today's week's Monday as a LocalDate
     */
    public LocalDate getCurrentWeekStart() {
        return LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }


    // -------------------------------------------------------
    //  PRIVATE HELPERS
    // -------------------------------------------------------

    private boolean isValidDay(String day) {
        for (String d : DAYS) {
            if (d.equalsIgnoreCase(day)) return true;
        }
        return false;
    }

    private boolean isValidMeal(String meal) {
        for (String m : MEALS) {
            if (m.equalsIgnoreCase(meal)) return true;
        }
        return false;
    }
}
