package com.recipe.models;

import java.time.LocalDate;

/**
 * ============================================================
 *  FILE        : MealPlan.java
 *  PACKAGE     : com.recipe.models
 *  AUTHOR      : Uzair
 *  MODULE      : Meal Planning & Nutrition
 * ============================================================
 *
 *  WHAT THIS CLASS IS:
 *  -------------------
 *  Mirrors ONE row in the MEAL_PLANS table.
 *  Each row says: "On this day, for this meal type,
 *  the user has planned this recipe."
 *
 *  FIELDS MAP TO DB COLUMNS:
 *   id            ←→  meal_plans.plan_id        (INT, PK)
 *   userId        ←→  meal_plans.user_id         (FK → users)
 *   recipeId      ←→  meal_plans.recipe_id       (FK → recipes)
 *   dayOfWeek     ←→  meal_plans.day_of_week     (VARCHAR: Monday … Sunday)
 *   mealType      ←→  meal_plans.meal_type       (VARCHAR: Breakfast/Lunch/Dinner)
 *   weekStartDate ←→  meal_plans.week_start_date (DATE — Monday of that week)
 *
 *  WHY weekStartDate?
 *  ------------------
 *  Storing the Monday date of the week lets us query a full week easily:
 *    WHERE user_id = ? AND week_start_date = ?
 *  Without it, we'd need date arithmetic in every query.
 *
 *  VIVA TIP:
 *  ---------
 *  "MealPlan is a join between users, recipes, and a calendar week.
 *   The combination of (userId, dayOfWeek, mealType, weekStartDate)
 *   is logically unique — a user can only have one recipe per meal slot."
 */
public class MealPlan {

    private int       id;
    private int       userId;
    private int       recipeId;
    private String    dayOfWeek;      // "Monday" … "Sunday"
    private String    mealType;       // "Breakfast" / "Lunch" / "Dinner"
    private LocalDate weekStartDate;  // The Monday of the planned week

    // Convenience field — not stored in DB, filled by the service layer
    // so the GUI can display a recipe title without a second lookup.
    private String recipeName;

    // ---- Constructors ----

    public MealPlan() {}

    public MealPlan(int id, int userId, int recipeId,
                    String dayOfWeek, String mealType,
                    LocalDate weekStartDate) {
        this.id            = id;
        this.userId        = userId;
        this.recipeId      = recipeId;
        this.dayOfWeek     = dayOfWeek;
        this.mealType      = mealType;
        this.weekStartDate = weekStartDate;
    }

    // ---- Getters ----

    public int       getId()            { return id; }
    public int       getUserId()        { return userId; }
    public int       getRecipeId()      { return recipeId; }
    public String    getDayOfWeek()     { return dayOfWeek; }
    public String    getMealType()      { return mealType; }
    public LocalDate getWeekStartDate() { return weekStartDate; }
    public String    getRecipeName()    { return recipeName; }

    // ---- Setters ----

    public void setId(int id)                       { this.id = id; }
    public void setUserId(int userId)               { this.userId = userId; }
    public void setRecipeId(int recipeId)           { this.recipeId = recipeId; }
    public void setDayOfWeek(String dayOfWeek)      { this.dayOfWeek = dayOfWeek; }
    public void setMealType(String mealType)        { this.mealType = mealType; }
    public void setWeekStartDate(LocalDate d)       { this.weekStartDate = d; }
    public void setRecipeName(String recipeName)    { this.recipeName = recipeName; }

    @Override
    public String toString() {
        return "MealPlan{userId=" + userId
                + ", recipe=" + (recipeName != null ? recipeName : recipeId)
                + ", " + dayOfWeek + " " + mealType
                + ", week=" + weekStartDate + "}";
    }
}
