package com.recipe.database;

import com.recipe.models.Ingredient;
import com.recipe.models.Recipe;
import com.recipe.models.RecipeIngredient;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 *  PURPOSE:
 *  Data Access Object - the ONLY class that directly touches
 *  the PostgreSQL 'recipes', 'ingredients', and
 *  'recipe_ingredients' tables with raw SQL.
 
 *  DB Tables Used:
 *   - recipes            (CRUD)
 *   - ingredients        (INSERT, SELECT)
 *   - recipe_ingredients (INSERT, SELECT with JOIN, DELETE)

 *  Used By  : RecipeService ONLY
 *
 *  Dependencies On AHMAD:
 *  DatabaseConnection.getInstance().getConnection()
 *  must be available before ANY method here can run.
 */
public class RecipeDAO {

    // SQL CONSTANTS
    // (Defining SQL as constants keeps methods clean and easy to edit)

    // Recipes table
    private static final String SQL_INSERT_RECIPE =
            "INSERT INTO recipes (user_id, category_id, title, description, " +
            "prep_time, cook_time, servings, difficulty, estimated_cost, " +
            "cook_count, photo_path, steps) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_ALL_RECIPES =
            "SELECT id, user_id, category_id, title, description, prep_time, " +
            "cook_time, servings, difficulty, estimated_cost, cook_count, " +
            "photo_path, steps, created_at " +
            "FROM recipes ORDER BY id DESC";

    private static final String SQL_SELECT_RECIPE_BY_ID =
            "SELECT id, user_id, category_id, title, description, prep_time, " +
            "cook_time, servings, difficulty, estimated_cost, cook_count, " +
            "photo_path, steps, created_at " +
            "FROM recipes WHERE id = ?";

    private static final String SQL_UPDATE_RECIPE =
            "UPDATE recipes SET category_id=?, title=?, description=?, " +
            "prep_time=?, cook_time=?, servings=?, difficulty=?, " +
            "estimated_cost=?, photo_path=?, steps=? " +
            "WHERE id=?";

    private static final String SQL_DELETE_RECIPE =
            "DELETE FROM recipes WHERE id = ?";

    private static final String SQL_GET_MOST_COOKED =
            "SELECT id, user_id, category_id, title, description, prep_time, " +
            "cook_time, servings, difficulty, estimated_cost, cook_count, " +
            "photo_path, steps, created_at " +
            "FROM recipes ORDER BY cook_count DESC LIMIT ?";

    private static final String SQL_GET_RECIPES_BY_USER =
            "SELECT id, user_id, category_id, title, description, prep_time, " +
            "cook_time, servings, difficulty, estimated_cost, cook_count, " +
            "photo_path, steps, created_at " +
            "FROM recipes WHERE user_id = ? ORDER BY id DESC";

    private static final String SQL_INCREMENT_COOK_COUNT =
            "UPDATE recipes SET cook_count = cook_count + 1 WHERE id = ?";

    //Ingredients table
    private static final String SQL_INSERT_INGREDIENT =
            "INSERT INTO ingredients (name, unit, allergen_flag) VALUES (?, ?, ?)";

    private static final String SQL_SELECT_INGREDIENT_BY_NAME =
            "SELECT id, name, unit, allergen_flag FROM ingredients WHERE LOWER(name) = LOWER(?)";

    private static final String SQL_SELECT_INGREDIENT_BY_ID =
            "SELECT id, name, unit, allergen_flag FROM ingredients WHERE id = ?";

    //Recipe_ingredients junction table
    private static final String SQL_INSERT_RECIPE_INGREDIENT =
            "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, price_per_unit) " +
            "VALUES (?, ?, ?, ?)";

    private static final String SQL_SELECT_INGREDIENTS_BY_RECIPE =
            "SELECT ri.id, ri.recipe_id, ri.ingredient_id, ri.quantity, ri.price_per_unit, " +
            "       i.name AS ing_name, i.unit AS ing_unit, i.allergen_flag " +
            "FROM recipe_ingredients ri " +
            "JOIN ingredients i ON ri.ingredient_id = i.id " +
            "WHERE ri.recipe_id = ? " +
            "ORDER BY ri.id ASC";

    private static final String SQL_DELETE_INGREDIENTS_BY_RECIPE =
            "DELETE FROM recipe_ingredients WHERE recipe_id = ?";


    // Recipe Crud Methods

    /*
     * Saves a NEW recipe to the database.
     *
     * Steps:
     * 1. Gets a connection from Ahmad's DatabaseConnection (HikariCP pool).
     * 2. Creates a PreparedStatement with RETURN_GENERATED_KEYS flag.
     * 3. Binds all Recipe fields as parameters (prevents SQL injection).
     * 4. Executes INSERT and checks that exactly 1 row was inserted.
     * 5. Reads back the auto-generated SERIAL id using getGeneratedKeys().
     * 6. Calls recipe.setId(generatedId) so the Recipe object now knows its DB id.
     *
     * Called by: RecipeService.saveRecipe()
     */
    public boolean insertRecipe(Recipe recipe) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_RECIPE,
                     Statement.RETURN_GENERATED_KEYS)) {

            // Bind all fields to '?' placeholders in order
            ps.setInt(1,           recipe.getUserId());
            ps.setInt(2,           recipe.getCategoryId());
            ps.setString(3,        recipe.getTitle());
            ps.setString(4,        recipe.getDescription());
            ps.setInt(5,           recipe.getPrepTime());
            ps.setInt(6,           recipe.getCookTime());
            ps.setInt(7,           recipe.getServings());
            ps.setString(8,        recipe.getDifficulty());
            ps.setBigDecimal(9,    recipe.getEstimatedCost());
            ps.setInt(10,          recipe.getCookCount());   // starts at 0
            ps.setString(11,       recipe.getPhotoPath());
            ps.setString(12,       recipe.getSteps());

            int rowsInserted = ps.executeUpdate();

            if (rowsInserted == 1) {
                // Read back the auto-generated primary key from PostgreSQL SERIAL
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    recipe.setId(generatedId);   // Now recipe knows its DB id
                }
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("[RecipeDAO] insertRecipe() failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves ALL recipes from the database, newest first.
     *
     * 1. Gets a connection from DatabaseConnection.
     * 2. Executes SELECT * FROM recipes ORDER BY id DESC.
     * 3. For each row in ResultSet, calls mapRowToRecipe() to build a Recipe object.
     * 4. Returns the complete list.
     *
     * Called by: RecipeService.getAllRecipes() RecipeListPanel.loadRecipes()
     */
    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL_RECIPES);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                recipes.add(mapRowToRecipe(rs));
            }

        } catch (SQLException e) {
            System.err.println("[RecipeDAO] getAllRecipes() failed: " + e.getMessage());
            e.printStackTrace();
        }

        return recipes;
    }

    /**
     * Retrieves ONE recipe by its primary key ID.
     *
     * Steps:
     * 1. Executes SELECT ... FROM recipes WHERE id = ?
     * 2. If found, maps the row to a Recipe object and returns it.
     * 3. If not found, returns null.
     *
     * Called by: RecipeService.getRecipeById()  RecipeDetailPanel (on card click)
     * 
     */
    public Recipe getRecipeById(int id) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_RECIPE_BY_ID)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRecipe(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("[RecipeDAO] getRecipeById(" + id + ") failed: " + e.getMessage());
            e.printStackTrace();
        }

        return null;  // Recipe with this id was not found
    }

    /**
     * Updates an existing recipe's data in the database.
     *
     * Steps:
     * 1. Builds an UPDATE statement with all editable fields.
     * 2. Binds new values from the Recipe object.
     * 3. The WHERE id=? ensures only THIS recipe is updated.
     *
     * NOTE: userId and created_at are NOT updated - they never change after creation.
     *
     * Called by: RecipeService.updateRecipe()  AddRecipePanel (in edit mode)
     * 
     */
    public boolean updateRecipe(Recipe recipe) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_RECIPE)) {

            ps.setInt(1,        recipe.getCategoryId());
            ps.setString(2,     recipe.getTitle());
            ps.setString(3,     recipe.getDescription());
            ps.setInt(4,        recipe.getPrepTime());
            ps.setInt(5,        recipe.getCookTime());
            ps.setInt(6,        recipe.getServings());
            ps.setString(7,     recipe.getDifficulty());
            ps.setBigDecimal(8, recipe.getEstimatedCost());
            ps.setString(9,     recipe.getPhotoPath());
            ps.setString(10,    recipe.getSteps());
            ps.setInt(11,       recipe.getId());   // WHERE id = ?

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.err.println("[RecipeDAO] updateRecipe() failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a recipe and all its ingredient rows from the database.
     *
     * Steps:
     * 1. First deletes ALL rows in recipe_ingredients where recipe_id = id.
     *    (Required to avoid FK constraint violation — child rows deleted before parent.)
     * 2. Then deletes the recipe row itself from the recipes table.
     * 3. Both operations run inside a single DB transaction — if step 2 fails,
     *    step 1 is rolled back to keep the database consistent.
     *
     * Called by: RecipeService.deleteRecipe()  RecipeDetailPanel (Delete button)
     */
    public boolean deleteRecipe(int id) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);   // Start transaction

            // 1: Delete ingredient rows first (FK child before parent)
            try (PreparedStatement deleteIngredients =
                         conn.prepareStatement(SQL_DELETE_INGREDIENTS_BY_RECIPE)) {
                deleteIngredients.setInt(1, id);
                deleteIngredients.executeUpdate();
            }

            // 2: Delete the recipe itself
            try (PreparedStatement deleteRecipe =
                         conn.prepareStatement(SQL_DELETE_RECIPE)) {
                deleteRecipe.setInt(1, id);
                int rowsDeleted = deleteRecipe.executeUpdate();

                if (rowsDeleted == 1) {
                    conn.commit();   // Both deletes succeeded - commit transaction
                    return true;
                } else {
                    conn.rollback();  // Recipe didn't exist - roll back
                    return false;
                }
            }

        } catch (SQLException e) {
            System.err.println("[RecipeDAO] deleteRecipe(" + id + ") failed: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);  // Restore default
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /*
     * Returns the top N most-cooked recipes, ordered by cook_count descending.
     * Used by the Dashboard screen to show "Most Cooked" stat card.
     *
     * Called by: RecipeService.getMostCooked(n)  DashboardPanel
     */
    public List<Recipe> getMostCooked(int n) {
        List<Recipe> recipes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_MOST_COOKED)) {

            ps.setInt(1, n);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    recipes.add(mapRowToRecipe(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[RecipeDAO] getMostCooked() failed: " + e.getMessage());
            e.printStackTrace();
        }

        return recipes;
    }

    /*
     * Returns all recipes created by a specific user.
     * Used in the Profile page to show "My Recipes".
     *
     * Called by: RecipeService.getRecipesByUser(userId)
     */
    public List<Recipe> getRecipesByUser(int userId) {
        List<Recipe> recipes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_RECIPES_BY_USER)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    recipes.add(mapRowToRecipe(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[RecipeDAO] getRecipesByUser() failed: " + e.getMessage());
            e.printStackTrace();
        }

        return recipes;
    }

    /*
     * Increments the cook_count for a recipe by 1 in the database.
     * Uses a single atomic SQL UPDATE - no need to read the count first.
     * This is safer than read-modify-write in case of concurrent updates.
     *
     * Called by: RecipeService.incrementCookCount(id)
     *             RecipeDetailPanel when user clicks "Start Cooking"

     */
    public void incrementCookCount(int recipeId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INCREMENT_COOK_COUNT)) {

            ps.setInt(1, recipeId);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[RecipeDAO] incrementCookCount() failed: " + e.getMessage());
            e.printStackTrace();
        }
    }


    //  Ingridents Methods

    /*
     * Inserts a NEW ingredient into the master ingredients table.
     * Before inserting, RecipeService should check if an ingredient
     * with the same name already exists (using findOrCreateIngredient).
     *
     * Called by: RecipeService.findOrCreateIngredient()
     */
    public boolean insertIngredient(Ingredient ingredient) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_INGREDIENT,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1,  ingredient.getName());
            ps.setString(2,  ingredient.getUnit());
            ps.setBoolean(3, ingredient.isAllergen());

            int rows = ps.executeUpdate();
            if (rows == 1) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    ingredient.setId(keys.getInt(1));
                }
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("[RecipeDAO] insertIngredient() failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /*
     * Finds an ingredient by name (case-insensitive) in the ingredients table.
     * Used to check if "Onion" already exists before inserting a duplicate.
     *
     * Called by: RecipeService.findOrCreateIngredient()
     */
    public Ingredient findIngredientByName(String name) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_INGREDIENT_BY_NAME)) {

            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToIngredient(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("[RecipeDAO] findIngredientByName() failed: " + e.getMessage());
            e.printStackTrace();
        }

        return null;  // Not found
    }

    //  Recipe_Ingredients function talbe methods

    /**
     * Inserts ONE row into recipe_ingredients linking a recipe to an ingredient
     * with a specific quantity and price.
     *
     * When saving a recipe with 5 ingredients, RecipeService calls this
     * method 5 times - once for each RecipeIngredient in the list.
     *
     * NOTE: recipe.setId() must have already been called with the DB-generated
     * id before this method runs - otherwise recipeId will be 0.
     *
     * Called by: RecipeService.saveRecipeIngredients()
     */
    public boolean insertRecipeIngredient(RecipeIngredient ri) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_RECIPE_INGREDIENT,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1,        ri.getRecipeId());
            ps.setInt(2,        ri.getIngredientId());
            ps.setBigDecimal(3, ri.getQuantity());
            ps.setBigDecimal(4, ri.getPricePerUnit());

            int rows = ps.executeUpdate();
            if (rows == 1) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    ri.setId(keys.getInt(1));
                }
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("[RecipeDAO] insertRecipeIngredient() failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves ALL ingredient rows for a given recipe, with full Ingredient
     * details populated via a JOIN with the ingredients table.
     *
     * SQL used:
     *   SELECT ri.*, i.name, i.unit, i.allergen_flag
     *   FROM recipe_ingredients ri
     *   JOIN ingredients i ON ri.ingredient_id = i.id
     *   WHERE ri.recipe_id = ?
     *   ORDER BY ri.id ASC
     *
     * After calling this, each RecipeIngredient has its Ingredient object set,
     * so the GUI can directly call ri.getIngredient().getName() without extra DB calls.
     *
     * Called by: RecipeService.getIngredientsForRecipe()
     *            RecipeDetailPanel (to display ingredient list)
     *            ServingScaler (to get the list to scale)
     *            CostEstimator (to calculate total cost)
     *
     */
    public List<RecipeIngredient> getIngredientsByRecipe(int recipeId) {
        List<RecipeIngredient> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_INGREDIENTS_BY_RECIPE)) {

            ps.setInt(1, recipeId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Build the RecipeIngredient object from junction table columns
                    RecipeIngredient ri = new RecipeIngredient();
                    ri.setId(rs.getInt("id"));
                    ri.setRecipeId(rs.getInt("recipe_id"));
                    ri.setIngredientId(rs.getInt("ingredient_id"));
                    ri.setQuantity(rs.getBigDecimal("quantity"));
                    ri.setPricePerUnit(rs.getBigDecimal("price_per_unit"));

                    // Build the nested Ingredient object from JOIN columns
                    Ingredient ing = new Ingredient();
                    ing.setId(rs.getInt("ingredient_id"));
                    ing.setName(rs.getString("ing_name"));
                    ing.setUnit(rs.getString("ing_unit"));
                    ing.setAllergenFlag(rs.getBoolean("allergen_flag"));

                    // Embed the Ingredient inside the RecipeIngredient
                    ri.setIngredient(ing);

                    list.add(ri);
                }
            }

        } catch (SQLException e) {
            System.err.println("[RecipeDAO] getIngredientsByRecipe(" + recipeId + ") failed: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    // helper metod

    /*
     * Maps ONE row from a ResultSet to a Recipe object.
     * Called internally by getAllRecipes(), getRecipeById(), etc.
     * Centralising the mapping here means if the DB schema changes,
     * we only update this one method.
     */
    private Recipe mapRowToRecipe(ResultSet rs) throws SQLException {
        Recipe recipe = new Recipe();

        recipe.setId(rs.getInt("id"));
        recipe.setUserId(rs.getInt("user_id"));
        recipe.setCategoryId(rs.getInt("category_id"));
        recipe.setTitle(rs.getString("title"));
        recipe.setDescription(rs.getString("description"));
        recipe.setPrepTime(rs.getInt("prep_time"));
        recipe.setCookTime(rs.getInt("cook_time"));
        recipe.setServings(rs.getInt("servings"));
        recipe.setDifficulty(rs.getString("difficulty"));
        recipe.setEstimatedCost(rs.getBigDecimal("estimated_cost"));
        recipe.setCookCount(rs.getInt("cook_count"));
        recipe.setPhotoPath(rs.getString("photo_path"));
        recipe.setSteps(rs.getString("steps"));

        // created_at is a TIMESTAMP in PostgreSQL  read as Timestamp → convert to LocalDateTime
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        if (createdAtTs != null) {
            recipe.setCreatedAt(createdAtTs.toLocalDateTime());
        }

        return recipe;
    }

    /**
     * Maps ONE row from a ResultSet to an Ingredient object.
     * Called internally by findIngredientByName().
     */
    private Ingredient mapRowToIngredient(ResultSet rs) throws SQLException {
        Ingredient ing = new Ingredient();
        ing.setId(rs.getInt("id"));
        ing.setName(rs.getString("name"));
        ing.setUnit(rs.getString("unit"));
        ing.setAllergenFlag(rs.getBoolean("allergen_flag"));
        return ing;
    }
}
