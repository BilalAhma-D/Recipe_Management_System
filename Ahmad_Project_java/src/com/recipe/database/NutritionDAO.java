package com.recipe.database;

import com.recipe.models.Nutrition;

import java.sql.*;

/**
 * ============================================================
 *  FILE        : NutritionDAO.java
 *  PACKAGE     : com.recipe.database
 *  AUTHOR      : Uzair
 *  MODULE      : Meal Planning & Nutrition
 * ============================================================
 *
 *  WHAT THIS CLASS DOES:
 *  ----------------------
 *  DAO (Data Access Object) for the NUTRITION table.
 *  Handles all INSERT and SELECT operations for nutrition data.
 *
 *  LAYER:
 *    NutritionCalculator (service)  →  NutritionDAO  →  PostgreSQL
 *                                          ↑
 *                                  DatabaseConnection
 *
 *  TABLE SCHEMA EXPECTED:
 *  ----------------------
 *  CREATE TABLE nutrition (
 *      nutrition_id SERIAL PRIMARY KEY,
 *      recipe_id    INT REFERENCES recipes(recipe_id),
 *      calories     DOUBLE PRECISION,
 *      protein_g    DOUBLE PRECISION,
 *      carbs_g      DOUBLE PRECISION,
 *      fat_g        DOUBLE PRECISION
 *  );
 *
 *  VIVA TIP:
 *  ---------
 *  "NutritionDAO uses PreparedStatements to prevent SQL injection
 *   and try-with-resources to guarantee connections are returned
 *   to Ahmad's HikariCP pool even if an exception occurs."
 */
public class NutritionDAO {

    // -------------------------------------------------------
    //  getNutritionByRecipeId()
    // -------------------------------------------------------

    /**
     * Reads the nutrition row for a given recipe.
     *
     * Called by: NutritionCalculator.getNutrition(recipeId)
     *
     * @param recipeId The recipe whose nutrition we want
     * @return A Nutrition object, or null if no row exists yet
     *
     * SQL: SELECT * FROM nutrition WHERE recipe_id = ?
     */
    public Nutrition getNutritionByRecipeId(int recipeId) {

        String sql = "SELECT nutrition_id, recipe_id, calories, "
                   + "protein_g, carbs_g, fat_g "
                   + "FROM nutrition WHERE recipe_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, recipeId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Nutrition(
                    rs.getInt("nutrition_id"),
                    rs.getInt("recipe_id"),
                    rs.getDouble("calories"),
                    rs.getDouble("protein_g"),
                    rs.getDouble("carbs_g"),
                    rs.getDouble("fat_g")
                );
            }

        } catch (SQLException e) {
            System.err.println("[NutritionDAO] getNutritionByRecipeId() failed: " + e.getMessage());
        }

        return null; // No nutrition data recorded for this recipe yet
    }


    // -------------------------------------------------------
    //  insertNutrition()
    // -------------------------------------------------------

    /**
     * Saves a new nutrition row to the database.
     *
     * Called by: NutritionCalculator when new nutrition data is entered.
     *
     * @param nutrition A Nutrition object with all fields set (except id — DB assigns it)
     * @return true if the INSERT succeeded, false otherwise
     *
     * SQL: INSERT INTO nutrition (recipe_id, calories, protein_g, carbs_g, fat_g)
     *      VALUES (?, ?, ?, ?, ?)
     */
    public boolean insertNutrition(Nutrition nutrition) {

        String sql = "INSERT INTO nutrition (recipe_id, calories, protein_g, carbs_g, fat_g) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, nutrition.getRecipeId());
            ps.setDouble(2, nutrition.getCalories());
            ps.setDouble(3, nutrition.getProteinG());
            ps.setDouble(4, nutrition.getCarbsG());
            ps.setDouble(5, nutrition.getFatG());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[NutritionDAO] insertNutrition() failed: " + e.getMessage());
            return false;
        }
    }


    // -------------------------------------------------------
    //  updateNutrition()
    // -------------------------------------------------------

    /**
     * Updates an existing nutrition row for a recipe.
     * Useful if nutritional values are corrected later.
     *
     * @param nutrition Nutrition object with updated values and correct recipeId
     * @return true if the UPDATE succeeded
     *
     * SQL: UPDATE nutrition SET calories=?, protein_g=?, carbs_g=?, fat_g=?
     *      WHERE recipe_id = ?
     */
    public boolean updateNutrition(Nutrition nutrition) {

        String sql = "UPDATE nutrition SET calories=?, protein_g=?, carbs_g=?, fat_g=? "
                   + "WHERE recipe_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, nutrition.getCalories());
            ps.setDouble(2, nutrition.getProteinG());
            ps.setDouble(3, nutrition.getCarbsG());
            ps.setDouble(4, nutrition.getFatG());
            ps.setInt(5, nutrition.getRecipeId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[NutritionDAO] updateNutrition() failed: " + e.getMessage());
            return false;
        }
    }
}
