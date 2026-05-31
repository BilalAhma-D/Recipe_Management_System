package com.recipe.gui;

import com.recipe.models.Recipe;

/**
 * =========================================================
 *  INTERFACE : AppNavigator
 *  PACKAGE   : com.recipe.gui
 *  AUTHOR    : Bilal
 * =========================================================
 *
 *  PURPOSE:
 *  Navigation contract between Bilal's GUI panels and
 *  Ahmad's MainWindow.
 *
 *  HOW TO USE (Ahmad reads this):
 *  MainWindow implements this interface.
 *  MainWindow passes "this" to each of Bilal's panel constructors.
 *
 *  EXAMPLE IN MainWindow:
 *  --------------------------------------------------
 *  public class MainWindow extends JFrame implements AppNavigator {
 *
 *      private RecipeService   recipeService;
 *      private RecipeListPanel   recipeListPanel;
 *      private AddRecipePanel    addRecipePanel;
 *      private RecipeDetailPanel recipeDetailPanel;
 *      private JPanel contentArea;      // CardLayout panel
 *      private CardLayout cardLayout;
 *
 *      private void initBilalsPanels() {
 *          recipeService    = new RecipeService(new RecipeDAO());
 *          recipeListPanel  = new RecipeListPanel(recipeService, this);
 *          addRecipePanel   = null; // created fresh each time
 *          // Add to cardLayout:
 *          contentArea.add(recipeListPanel, "RECIPES");
 *      }
 *
 *      // ── AppNavigator implementation ─────────────────
 *
 *      public void navigateToRecipeList() {
 *          recipeListPanel.loadRecipes();          // refresh
 *          cardLayout.show(contentArea, "RECIPES");
 *      }
 *
 *      public void navigateToAddRecipe() {
 *          AddRecipePanel addPanel = new AddRecipePanel(recipeService, this);
 *          contentArea.add(addPanel, "ADD_RECIPE");
 *          cardLayout.show(contentArea, "ADD_RECIPE");
 *      }
 *
 *      public void navigateToEditRecipe(Recipe recipe) {
 *          AddRecipePanel editPanel = new AddRecipePanel(recipeService, this, recipe);
 *          contentArea.add(editPanel, "EDIT_RECIPE");
 *          cardLayout.show(contentArea, "EDIT_RECIPE");
 *      }
 *
 *      public void navigateToRecipeDetail(Recipe recipe) {
 *          RecipeDetailPanel detailPanel = new RecipeDetailPanel(recipe, recipeService, this);
 *          contentArea.add(detailPanel, "RECIPE_DETAIL");
 *          cardLayout.show(contentArea, "RECIPE_DETAIL");
 *      }
 *  }
 *  --------------------------------------------------
 *
 *  NOTE FOR BILAL'S TAB (TAB 4 in Main.java):
 *  Replace the stub panel with:
 *      tabs.addTab("Recipes", recipeListPanel);
 * =========================================================
 */
public interface AppNavigator {
    void navigateToRecipeList();
    void navigateToAddRecipe();
    void navigateToEditRecipe(Recipe recipe);
    void navigateToRecipeDetail(Recipe recipe);
}
