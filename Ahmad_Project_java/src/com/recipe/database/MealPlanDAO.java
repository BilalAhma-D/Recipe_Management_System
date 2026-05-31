package com.recipe.database;

import com.recipe.models.MealPlan;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  FILE        : MealPlanDAO.java
 *  PACKAGE     : com.recipe.database
 *  AUTHOR      : Uzair
 *  MODULE      : Meal Planning & Nutrition
 * ============================================================
 *
 *  WHAT THIS CLASS DOES:
 *  ----------------------
 *  DAO for the MEAL_PLANS table. Handles INSERT, SELECT,
 *  and DELETE for meal plan entries.
 *
 *  TABLE SCHEMA EXPECTED:
 *  ----------------------
 *  CREATE TABLE meal_plans (
 *      plan_id         SERIAL PRIMARY KEY,
 *      user_id         INT REFERENCES users(user_id),
 *      recipe_id       INT REFERENCES recipes(recipe_id),
 *      day_of_week     VARCHAR(10),   -- 'Monday' … 'Sunday'
 *      meal_type       VARCHAR(15),   -- 'Breakfast' / 'Lunch' / 'Dinner'
 *      week_start_date DATE           -- The Monday of the planned week
 *  );
 *
 *  VIVA TIP:
 *  ---------
 *  "MealPlanDAO.getMealPlanByUser() does a JOIN with the recipes
 *   table so we get the recipe name in one query — avoiding N+1
 *   queries (one extra query per meal plan entry)."
 */
public class MealPlanDAO {

    // -------------------------------------------------------
    //  insertMealPlanEntry()
    // -------------------------------------------------------

    /**
     * Saves a new meal plan slot to the database.
     *
     * Called by: MealPlanService.assignRecipe()
     *
     * @param plan A MealPlan object with userId, recipeId, dayOfWeek,
     *             mealType, and weekStartDate already set.
     * @return true if INSERT succeeded, false otherwise
     *
     * SQL: INSERT INTO meal_plans (user_id, recipe_id, day_of_week,
     *                              meal_type, week_start_date)
     *      VALUES (?, ?, ?, ?, ?)
     */
    public boolean insertMealPlanEntry(MealPlan plan) {

        String sql = "INSERT INTO meal_plans "
                   + "(user_id, recipe_id, day_of_week, meal_type, week_start_date) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, plan.getUserId());
            ps.setInt(2, plan.getRecipeId());
            ps.setString(3, plan.getDayOfWeek());
            ps.setString(4, plan.getMealType());
            ps.setDate(5, Date.valueOf(plan.getWeekStartDate()));  // LocalDate → java.sql.Date

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[MealPlanDAO] insertMealPlanEntry() failed: " + e.getMessage());
            return false;
        }
    }


    // -------------------------------------------------------
    //  getMealPlanByUser()
    // -------------------------------------------------------

    /**
     * Loads the full week plan for a user.
     *
     * JOINs with recipes to fill in the recipe name so the GUI
     * can display it without a second lookup per entry.
     *
     * Called by: MealPlanService.getWeekPlan()
     *
     * @param userId        The logged-in user's ID
     * @param weekStartDate The Monday of the week to load
     * @return A List of MealPlan objects (may be empty if nothing planned yet)
     *
     * SQL: SELECT mp.*, r.title AS recipe_name
     *      FROM meal_plans mp
     *      JOIN recipes r ON mp.recipe_id = r.recipe_id
     *      WHERE mp.user_id = ? AND mp.week_start_date = ?
     */
    public List<MealPlan> getMealPlanByUser(int userId, LocalDate weekStartDate) {

        List<MealPlan> results = new ArrayList<>();

        String sql = "SELECT mp.plan_id, mp.user_id, mp.recipe_id, "
                   + "mp.day_of_week, mp.meal_type, mp.week_start_date, "
                   + "r.title AS recipe_name "
                   + "FROM meal_plans mp "
                   + "JOIN recipes r ON mp.recipe_id = r.recipe_id "
                   + "WHERE mp.user_id = ? AND mp.week_start_date = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(weekStartDate));

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                MealPlan mp = new MealPlan(
                    rs.getInt("plan_id"),
                    rs.getInt("user_id"),
                    rs.getInt("recipe_id"),
                    rs.getString("day_of_week"),
                    rs.getString("meal_type"),
                    rs.getDate("week_start_date").toLocalDate()
                );
                mp.setRecipeName(rs.getString("recipe_name"));
                results.add(mp);
            }

        } catch (SQLException e) {
            System.err.println("[MealPlanDAO] getMealPlanByUser() failed: " + e.getMessage());
        }

        return results;
    }


    // -------------------------------------------------------
    //  deleteMealPlanEntry()
    // -------------------------------------------------------

    /**
     * Removes one meal slot from the plan (e.g. user clears a cell).
     *
     * Called by: MealPlanService.removeMeal()
     *
     * @param userId        The user's ID (safety check — can't delete another user's plan)
     * @param dayOfWeek     Which day to clear (e.g. "Monday")
     * @param mealType      Which meal to clear (e.g. "Lunch")
     * @param weekStartDate The week this slot belongs to
     * @return true if something was deleted, false if the slot was already empty
     *
     * SQL: DELETE FROM meal_plans
     *      WHERE user_id=? AND day_of_week=? AND meal_type=? AND week_start_date=?
     */
    public boolean deleteMealPlanEntry(int userId, String dayOfWeek,
                                        String mealType, LocalDate weekStartDate) {

        String sql = "DELETE FROM meal_plans "
                   + "WHERE user_id=? AND day_of_week=? "
                   + "AND meal_type=? AND week_start_date=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, dayOfWeek);
            ps.setString(3, mealType);
            ps.setDate(4, Date.valueOf(weekStartDate));

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[MealPlanDAO] deleteMealPlanEntry() failed: " + e.getMessage());
            return false;
        }
    }


    // -------------------------------------------------------
    //  getRecipeIdsForWeek()
    // -------------------------------------------------------

    /**
     * Returns all distinct recipe IDs planned for a given user's week.
     * Used by GroceryListBuilder to know which recipes' ingredients to fetch.
     *
     * @param userId        The user's ID
     * @param weekStartDate The Monday of the week
     * @return A List of recipe IDs (no duplicates — DISTINCT in SQL)
     *
     * SQL: SELECT DISTINCT recipe_id FROM meal_plans
     *      WHERE user_id = ? AND week_start_date = ?
     */
    public List<Integer> getRecipeIdsForWeek(int userId, LocalDate weekStartDate) {

        List<Integer> ids = new ArrayList<>();

        String sql = "SELECT DISTINCT recipe_id FROM meal_plans "
                   + "WHERE user_id = ? AND week_start_date = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(weekStartDate));

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt("recipe_id"));
            }

        } catch (SQLException e) {
            System.err.println("[MealPlanDAO] getRecipeIdsForWeek() failed: " + e.getMessage());
        }

        return ids;
    }
}
