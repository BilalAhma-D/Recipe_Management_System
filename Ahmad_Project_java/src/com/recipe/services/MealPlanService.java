package com.recipe.services;

import com.recipe.database.MealPlanDAO;
import com.recipe.models.MealPlan;
import com.recipe.models.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

// ============================================================
//  FILE    : MealPlanService.java
//  AUTHOR  : Uzair
//  PURPOSE : Business logic for meal planning.
//            Sits between MealPlannerPanel (GUI) and MealPlanDAO (DB).
//            GUI never calls MealPlanDAO directly.
// ============================================================
//
//  BUG FIXED (v2):
//  Was using List.of() which requires Java 9+.
//  Fixed to: new ArrayList<>()  (works on Java 8+)

public class MealPlanService {

    private final MealPlanDAO mealPlanDAO;

    // Days and meal types used by the GUI grid and for validation
    public static final String[] DAYS  = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
    public static final String[] MEALS = { "Breakfast", "Lunch", "Dinner" };

    public MealPlanService() {
        this.mealPlanDAO = new MealPlanDAO();
    }

    // --------------------------------------------------------
    //  assignRecipe()
    //  Assigns a recipe to a day + meal slot for the current user.
    //  Automatically calculates the Monday (weekStartDate) of
    //  whichever week 'anyDateInWeek' belongs to.
    // --------------------------------------------------------
    public String assignRecipe(User user, int recipeId,
                                String dayOfWeek, String mealType,
                                LocalDate anyDateInWeek) {

        if (user     == null)          return "Not logged in.";
        if (recipeId <= 0)             return "Invalid recipe selected.";
        if (!isValidDay(dayOfWeek))    return "Invalid day: "       + dayOfWeek;
        if (!isValidMeal(mealType))    return "Invalid meal type: " + mealType;

        LocalDate weekStart = getWeekStart(anyDateInWeek);

        MealPlan plan = new MealPlan();
        plan.setUserId(user.getId());
        plan.setRecipeId(recipeId);
        plan.setDayOfWeek(dayOfWeek);
        plan.setMealType(mealType);
        plan.setWeekStartDate(weekStart);

        return mealPlanDAO.insertMealPlanEntry(plan)
               ? "SUCCESS"
               : "Failed to save meal plan. Please try again.";
    }

    // --------------------------------------------------------
    //  getWeekPlan()
    //  Loads all meal entries for the user's chosen week.
    //  Returns an empty list (not null) when nothing is planned.
    // --------------------------------------------------------
    public List<MealPlan> getWeekPlan(User user, LocalDate anyDateInWeek) {
        if (user == null) return new ArrayList<>();  // BUG FIX: was List.of() — requires Java 9+

        LocalDate weekStart = getWeekStart(anyDateInWeek);
        return mealPlanDAO.getMealPlanByUser(user.getId(), weekStart);
    }

    // --------------------------------------------------------
    //  removeMeal()
    //  Clears one meal slot (user clicked Remove on a cell).
    // --------------------------------------------------------
    public String removeMeal(User user, String dayOfWeek,
                              String mealType, LocalDate anyDateInWeek) {

        if (user == null) return "Not logged in.";

        LocalDate weekStart = getWeekStart(anyDateInWeek);

        return mealPlanDAO.deleteMealPlanEntry(user.getId(), dayOfWeek, mealType, weekStart)
               ? "SUCCESS"
               : "Nothing to remove.";
    }

    // --------------------------------------------------------
    //  getCurrentWeekStart()
    //  Returns today's week's Monday.
    //  Called by MealPlannerPanel to set the default displayed week.
    // --------------------------------------------------------
    public LocalDate getCurrentWeekStart() {
        return getWeekStart(LocalDate.now());
    }

    // --------------------------------------------------------
    //  PRIVATE HELPERS
    // --------------------------------------------------------

    // Finds the Monday of whichever week the given date falls in.
    private LocalDate getWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private boolean isValidDay(String day) {
        for (String d : DAYS)  if (d.equalsIgnoreCase(day))  return true;
        return false;
    }

    private boolean isValidMeal(String meal) {
        for (String m : MEALS) if (m.equalsIgnoreCase(meal)) return true;
        return false;
    }
}
