package com.recipe.database;

import com.recipe.models.Nutrition;
import java.sql.*;

// ============================================================
//  FILE    : NutritionDAO.java
//  AUTHOR  : Uzair
//  PURPOSE : Handles all SQL for the nutrition table.
//            NutritionCalculator (service) calls this — GUI never does.
// ============================================================
//
//  LAYER:
//    NutritionPanel  →  NutritionCalculator  →  NutritionDAO  →  PostgreSQL
//                                                     ↑
//                                           DatabaseConnection (Ahmad's)

public class NutritionDAO {

    // --------------------------------------------------------
    //  getNutritionByRecipeId()
    //  Returns the nutrition row for one recipe, or null if none saved yet.
    // --------------------------------------------------------
    public Nutrition getNutritionByRecipeId(int recipeId) {
        String sql = "SELECT nutrition_id, recipe_id, calories, protein_g, carbs_g, fat_g "
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
        return null;
    }

    // --------------------------------------------------------
    //  insertNutrition()
    //  Saves a new nutrition row. DB auto-assigns nutrition_id.
    // --------------------------------------------------------
    public boolean insertNutrition(Nutrition n) {
        String sql = "INSERT INTO nutrition (recipe_id, calories, protein_g, carbs_g, fat_g) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1,    n.getRecipeId());
            ps.setDouble(2, n.getCalories());
            ps.setDouble(3, n.getProteinG());
            ps.setDouble(4, n.getCarbsG());
            ps.setDouble(5, n.getFatG());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[NutritionDAO] insertNutrition() failed: " + e.getMessage());
            return false;
        }
    }

    // --------------------------------------------------------
    //  updateNutrition()
    //  Overwrites existing nutrition values for a recipe.
    // --------------------------------------------------------
    public boolean updateNutrition(Nutrition n) {
        String sql = "UPDATE nutrition SET calories=?, protein_g=?, carbs_g=?, fat_g=? "
                   + "WHERE recipe_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, n.getCalories());
            ps.setDouble(2, n.getProteinG());
            ps.setDouble(3, n.getCarbsG());
            ps.setDouble(4, n.getFatG());
            ps.setInt(5,    n.getRecipeId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[NutritionDAO] updateNutrition() failed: " + e.getMessage());
            return false;
        }
    }
}
