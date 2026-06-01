package com.recipe.database;

import com.recipe.models.MealPlan;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// ============================================================
//  FILE    : MealPlanDAO.java
//  AUTHOR  : Uzair
//  PURPOSE : All SQL for the meal_plans table.
//            MealPlanService calls this — GUI never does.
// ============================================================
//
//  BUG FIXED (v2):
//  JOIN was using "r.recipe_id" — but Bilal's recipes table PK
//  column is named "id", NOT "recipe_id".
//  Fixed to: JOIN recipes r ON mp.recipe_id = r.id

public class MealPlanDAO {

    // --------------------------------------------------------
    //  insertMealPlanEntry()
    //  Saves one meal slot (user + day + meal + recipe + week).
    // --------------------------------------------------------
    public boolean insertMealPlanEntry(MealPlan plan) {
        String sql = "INSERT INTO meal_plans "
                   + "(user_id, recipe_id, day_of_week, meal_type, week_start_date) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1,    plan.getUserId());
            ps.setInt(2,    plan.getRecipeId());
            ps.setString(3, plan.getDayOfWeek());
            ps.setString(4, plan.getMealType());
            ps.setDate(5,   Date.valueOf(plan.getWeekStartDate()));

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[MealPlanDAO] insertMealPlanEntry() failed: " + e.getMessage());
            return false;
        }
    }

    // --------------------------------------------------------
    //  getMealPlanByUser()
    //  Loads the full week plan for a user.
    //  JOINs with recipes to get the recipe title in one query
    //  (avoids one DB call per row — known as the N+1 problem).
    //
    //  BUG FIXED: was JOIN recipes r ON mp.recipe_id = r.recipe_id
    //             Bilal's PK is "id" not "recipe_id"
    //             Fixed to: JOIN recipes r ON mp.recipe_id = r.id
    // --------------------------------------------------------
    public List<MealPlan> getMealPlanByUser(int userId, LocalDate weekStartDate) {
        List<MealPlan> results = new ArrayList<>();

        String sql = "SELECT mp.plan_id, mp.user_id, mp.recipe_id, "
                   + "mp.day_of_week, mp.meal_type, mp.week_start_date, "
                   + "r.title AS recipe_name "
                   + "FROM meal_plans mp "
                   + "JOIN recipes r ON mp.recipe_id = r.id "   // r.id  ← Bilal's PK name
                   + "WHERE mp.user_id = ? AND mp.week_start_date = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1,  userId);
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

    // --------------------------------------------------------
    //  deleteMealPlanEntry()
    //  Removes one meal slot. userId check prevents deleting
    //  another user's plan by accident.
    // --------------------------------------------------------
    public boolean deleteMealPlanEntry(int userId, String dayOfWeek,
                                       String mealType, LocalDate weekStartDate) {
        String sql = "DELETE FROM meal_plans "
                   + "WHERE user_id=? AND day_of_week=? "
                   + "AND meal_type=? AND week_start_date=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1,    userId);
            ps.setString(2, dayOfWeek);
            ps.setString(3, mealType);
            ps.setDate(4,   Date.valueOf(weekStartDate));

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[MealPlanDAO] deleteMealPlanEntry() failed: " + e.getMessage());
            return false;
        }
    }

    // --------------------------------------------------------
    //  getRecipeIdsForWeek()
    //  Returns distinct recipe IDs planned for a given week.
    //  Used by GroceryListPanel to know which ingredients to show.
    // --------------------------------------------------------
    public List<Integer> getRecipeIdsForWeek(int userId, LocalDate weekStartDate) {
        List<Integer> ids = new ArrayList<>();

        String sql = "SELECT DISTINCT recipe_id FROM meal_plans "
                   + "WHERE user_id = ? AND week_start_date = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1,  userId);
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
