package com.recipe.services;

import com.recipe.database.RecipeDAO;
import com.recipe.models.Ingredient;
import com.recipe.models.Recipe;
import com.recipe.models.RecipeIngredient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/*
 *  Purpose:
 *  The single access point for ALL recipe operations in the application.
 *  GUI panels (RecipeListPanel, AddRecipePanel, RecipeDetailPanel) call
 *  RecipeService methods ONLY , they NEVER call RecipeDAO directly.

 *  WHAT RecipeService Does:
 *  1. Validates input data before any DB operation
 *  2. Attaches the current user's ID from SessionManager (Ahmad's class)
 *  3. Calls CostEstimator to calculate estimated cost
 *  4. Calls DifficultyBadgeCalc (Raja's class) to auto-set difficulty
 *  5. Delegates to RecipeDAO for the actual SQL operations
 *  6. Handles errors gracefully — never crashes the GUI
 
 *  Dependencies:
 *   - RecipeDAO           (Bilal's own DAO class)
 *   - CostEstimator       (Bilal's own utility class)
 *   - SessionManager      (Ahmad's gets current user)
 *   - DifficultyBadgeCalc (Raja's utility -- auto-calculates difficulty)
 
 *  How to use:
 *   // In MainWindow constructor:
 *   RecipeDAO recipeDAO = new RecipeDAO();
 *   RecipeService recipeService = new RecipeService(recipeDAO);
 *
 *   // In any GUI panel:
 *   List<Recipe> all = recipeService.getAllRecipes();
 *   boolean saved    = recipeService.saveRecipe(recipe, ingredientList);

 *  Used By : RecipeListPanel, AddRecipePanel, RecipeDetailPanel,
 *             Uzair's MealPlanService
 */

public class RecipeService {

    private final RecipeDAO recipeDAO;

    // Constructor

    /*
     * Constructor — takes a RecipeDAO instance (dependency injection).
     * Called ONCE when the application starts (in MainWindow).
     *
     * USAGE:
     *   RecipeDAO dao = new RecipeDAO();
     *   RecipeService service = new RecipeService(dao);
    
     * - RecipeService doesn't create its own DAO
     * - Ahmad creates DatabaseConnection; Bilal creates RecipeDAO;
     *   both are connected together here.
    
     */

    public RecipeService(RecipeDAO recipeDAO) {
        if (recipeDAO == null) {
            throw new IllegalArgumentException("RecipeDAO cannot be null.");
        }
        this.recipeDAO = recipeDAO;
    }


    //  SAVE / CREATE

    /**
     * Saves a NEW recipe to the database.
     
     * Called by: AddRecipePanel.validateAndSave()
     */

    public boolean saveRecipe(Recipe recipe, List<RecipeIngredient> ingredients) {

        // Validate title
        if (recipe.getTitle() == null || recipe.getTitle().trim().isEmpty()) {
            System.err.println("[RecipeService] Save failed: title is blank.");
            return false;
        }

        // Validate numeric fields
        if (recipe.getPrepTime() < 0 || recipe.getCookTime() < 0) {
            System.err.println("[RecipeService] Save failed: negative time values.");
            return false;
        }
        if (recipe.getServings() <= 0) {
            System.err.println("[RecipeService] Save failed: servings must be > 0.");
            return false;
        }

        // Attach the current user's ID
        // SessionManager is Ahmad's  — getCurrentUser() returns the logged-in User
        try {
            int currentUserId = SessionManager.getInstance().getCurrentUser().getId();
            recipe.setUserId(currentUserId);
        } catch (NullPointerException e) {
            System.err.println("[RecipeService] Save failed: no user logged in. " +
                               "SessionManager.getCurrentUser() returned null.");
            return false;
        }

        // Auto-calculate difficulty
        // DifficultyBadgeCalc is Raja's utility class
        // It needs: step count, ingredient count, total cook time
        int stepCount = countSteps(recipe.getSteps());
        int ingredientCount = (ingredients != null) ? ingredients.size() : 0;
        String calculatedDifficulty = DifficultyBadgeCalc.calculate(
                stepCount,
                ingredientCount,
                recipe.getCookTime()
        );
        recipe.setDifficulty(calculatedDifficulty);

        // Calculate estimated cost
        if (ingredients != null && !ingredients.isEmpty()) {
            BigDecimal totalCost = CostEstimator.estimateTotalCost(ingredients);
            recipe.setEstimatedCost(totalCost);
        } else {
            recipe.setEstimatedCost(BigDecimal.ZERO);
        }

        // Insert recipe into DB
        boolean recipeInserted = recipeDAO.insertRecipe(recipe);
        if (!recipeInserted) {
            System.err.println("[RecipeService] DB insert of recipe failed.");
            return false;
        }
        // After insertRecipe(), recipe.getId() now has the DB-generated id

        // Save ingredient rows
        if (ingredients != null && !ingredients.isEmpty()) {
            boolean ingredientsSaved = saveRecipeIngredients(recipe.getId(), ingredients);
            if (!ingredientsSaved) {
                System.err.println("[RecipeService] Warning: some ingredient rows failed to save.");
                // Recipe was saved but ingredients partially failed.
                // Return true anyway — recipe exists, ingredients can be re-added.
            }
        }

        System.out.println("[RecipeService] Recipe saved: " + recipe);
        return true;
    }

    
    //  Updates an Existing recipe in the database.
     
    public boolean updateRecipe(Recipe recipe, List<RecipeIngredient> ingredients) {

        // Validate id — must be a saved recipe, not a new one
        if (recipe.getId() <= 0) {
            System.err.println("[RecipeService] Update failed: recipe has no valid id.");
            return false;
        }

        // Validate title
        if (recipe.getTitle() == null || recipe.getTitle().trim().isEmpty()) {
            System.err.println("[RecipeService] Update failed: title is blank.");
            return false;
        }

        // Recalculate difficulty with updated data
        int stepCount       = countSteps(recipe.getSteps());
        int ingredientCount = (ingredients != null) ? ingredients.size() : 0;
        String difficulty   = DifficultyBadgeCalc.calculate(
                stepCount, ingredientCount, recipe.getCookTime()
        );
        recipe.setDifficulty(difficulty);

        // Recalculate cost with updated ingredients
        if (ingredients != null && !ingredients.isEmpty()) {
            recipe.setEstimatedCost(CostEstimator.estimateTotalCost(ingredients));
        }

        // Update the recipe row in DB
        boolean updated = recipeDAO.updateRecipe(recipe);
        if (!updated) {
            System.err.println("[RecipeService] DB update of recipe failed.");
            return false;
        }

        // Replace ingredient rows: delete old ones, insert new ones
        if (ingredients != null) {
            // The old rows will be deleted and new rows inserted
            // (RecipeDAO.deleteRecipe handles ingredient cleanes,
            //  here we use saveRecipeIngredients which replaces all rows)
            saveRecipeIngredients(recipe.getId(), ingredients);
        }

        System.out.println("[RecipeService] Recipe updated: " + recipe);
        return true;
    }

    /*
     * Returns ALL recipes from the database, newest first.
     * Called by RecipeListPanel.loadRecipes() to populate the recipe grid.
     */

    public List<Recipe> getAllRecipes() {
        return recipeDAO.getAllRecipes();
    }

    /*
     * Returns ONE recipe by its primary key.
     * Called when user clicks a recipe card in RecipeListPanel.
     */

    public Recipe getRecipeById(int id) {
        return recipeDAO.getRecipeById(id);
    }

    /*
     * Returns all recipes belonging to the currently logged-in user.
     * Used on the Profile page to show "My Recipes".
     */

    public List<Recipe> getRecipesByUser(int userId) {
        return recipeDAO.getRecipesByUser(userId);
    }

    /**
     * Returns the top N most-cooked recipes.
     * Used by the Dashboard screen's "Most Cooked" stat card.
     */
    public List<Recipe> getMostCooked(int n) {
        if (n <= 0) return new ArrayList<>();
        return recipeDAO.getMostCooked(n);
    }

    /**
     * Returns all ingredient rows for a specific recipe, with Ingredient
     * objects populated via JOIN.
     * Called by RecipeDetailPanel to display the ingredient list,
     * by ServingScaler to get quantities to scale,
     * by CostEstimator to calculate total cost.
     */
    public List<RecipeIngredient> getIngredientsForRecipe(int recipeId) {
        return recipeDAO.getIngredientsByRecipe(recipeId);
    }

    // Delete

    /**
     * Deletes a recipe and ALL its ingredient rows from the database.
     * The GUI (RecipeDetailPanel) shows a JOptionPane confirm dialog
     * BEFORE calling this method — this method does not ask again.
     */
    
    public boolean deleteRecipe(int id) {
        if (id <= 0) {
            System.err.println("[RecipeService] deleteRecipe: invalid id = " + id);
            return false;
        }
        boolean deleted = recipeDAO.deleteRecipe(id);
        if (deleted) {
            System.out.println("[RecipeService] Recipe id=" + id + " deleted.");
        }
        return deleted;
    }


    /**
     * Creates a Duplicate of an existing recipe with a new title.
    
     * Called by: RecipeDetailPanel.onDuplicateClicked()
     */
    public Recipe duplicateRecipe(int originalId) {
        Recipe original = recipeDAO.getRecipeById(originalId);
        if (original == null) {
            System.err.println("[RecipeService] duplicateRecipe: original not found, id=" + originalId);
            return null;
        }

        // Build the duplicate Recipe object
        Recipe duplicate = new Recipe();
        duplicate.setTitle("Copy of " + original.getTitle());
        duplicate.setDescription(original.getDescription());
        duplicate.setPrepTime(original.getPrepTime());
        duplicate.setCookTime(original.getCookTime());
        duplicate.setServings(original.getServings());
        duplicate.setCategoryId(original.getCategoryId());
        duplicate.setDifficulty(original.getDifficulty());
        duplicate.setEstimatedCost(original.getEstimatedCost());
        duplicate.setPhotoPath(original.getPhotoPath());
        duplicate.setSteps(original.getSteps());
        duplicate.setCookCount(0);   // Start fresh — not pre-cooked
        // id stays 0 (default) — insertRecipe() will assign a new one
        // userId will be set by saveRecipe() from SessionManager

        // Get original ingredient rows
        List<RecipeIngredient> originalIngredients = recipeDAO.getIngredientsByRecipe(originalId);

        // Reset the ingredient IDs and recipeId - they'll be new rows
        List<RecipeIngredient> duplicateIngredients = new ArrayList<>();
        for (RecipeIngredient ri : originalIngredients) {
            RecipeIngredient copy = new RecipeIngredient();
            copy.setIngredientId(ri.getIngredientId());
            copy.setQuantity(ri.getQuantity());
            copy.setPricePerUnit(ri.getPricePerUnit());
            copy.setIngredient(ri.getIngredient());
            // recipeId will be set by saveRecipeIngredients() after insert
            duplicateIngredients.add(copy);
        }

        // Save the duplicate (this sets duplicate.getId() with the new DB id)
        boolean saved = saveRecipe(duplicate, duplicateIngredients);
        if (!saved) {
            System.err.println("[RecipeService] duplicateRecipe: save failed.");
            return null;
        }

        System.out.println("[RecipeService] Recipe duplicated. New id=" + duplicate.getId());
        return duplicate;
    }

    /*
     * Increments the cook count for a recipe both in the Recipe object
     * (in memory) and in the database.
    
     * Called by: RecipeDetailPanel.startCookingMode()
     *             when user clicks "Start Cooking" button
     */
    public void incrementCookCount(Recipe recipe) {
        if (recipe == null || recipe.getId() <= 0) return;

        recipe.incrementCookCount();              // Update in-memory object
        recipeDAO.incrementCookCount(recipe.getId());  // Persist to DB
        System.out.println("[RecipeService] Cook count incremented for recipe id=" + recipe.getId());
    }

    /**
     * Checks whether an ingredient name already exists in the DB.
     * If it does, returns the existing Ingredient. If not, inserts it and returns the new one.
    
     * This prevents duplicate "Onion" entries in the ingredients table.
     * 
     * Called by: saveRecipeIngredients()
     */
    public Ingredient findOrCreateIngredient(String name, String unit, boolean allergenFlag) {
        // First try to find existing ingredient by name
        Ingredient existing = recipeDAO.findIngredientByName(name);
        if (existing != null) {
            return existing;   // Already exists - reuse it
        }

        // Does not exist - create and insert a new one
        Ingredient newIngredient = new Ingredient(name, unit, allergenFlag);
        boolean inserted = recipeDAO.insertIngredient(newIngredient);
        if (!inserted) {
            System.err.println("[RecipeService] Failed to insert new ingredient: " + name);
            return null;
        }
        return newIngredient;
    }

    // Helper Method

    /**
     * Saves ALL ingredient rows for a recipe to the recipe_ingredients table.
     * Called after insertRecipe() - at that point recipe.getId() has a valid id.
     */
    private boolean saveRecipeIngredients(int recipeId, List<RecipeIngredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) return true;

        int failed = 0;
        for (RecipeIngredient ri : ingredients) {
            ri.setRecipeId(recipeId);   // Link each row to this recipe's id
            boolean saved = recipeDAO.insertRecipeIngredient(ri);
            if (!saved) {
                failed++;
                System.err.println("[RecipeService] Failed to save ingredient row: " + ri);
            }
        }

        return failed == 0;
    }

    /*
     * Counts the number of cooking steps in a steps text string.
     * Steps are separated by newline "\n".
     * Used to calculate difficulty via DifficultyBadgeCalc.
     */
    private int countSteps(String stepsText) {
        if (stepsText == null || stepsText.trim().isEmpty()) {
            return 0;
        }
        String[] lines = stepsText.split("\n");
        int count = 0;
        for (String line : lines) {
            if (!line.trim().isEmpty()) count++;
        }
        return count;
    }
}
