
--  AUTHOR      : Ahmad
--  PURPOSE     : Creates all PostgreSQL tables for the
--                Recipe Management System.
--  RUN THIS    : Once, before any code is tested.
--                In pgAdmin: File - Open - run this file.
--                Terminal: psql -U postgres -d recipedb -f schema.sql

--  RecipeDAO.java  : NO CHANGES NEEDED — already correct
--  RecipeService.java : Apply the SessionManager fix (see fix note below)
-- ============================================================


-- ============================================================
--  STEP 0: Create the database (run separately if needed)
-- ============================================================
-- CREATE DATABASE recipedb;
-- \c recipedb


-- ============================================================
--  DROP TABLES — clean re-run during development
--  "CASCADE" drops dependent foreign keys automatically.
-- ============================================================

DROP TABLE IF EXISTS recipe_versions    CASCADE;
DROP TABLE IF EXISTS grocery_lists      CASCADE;
DROP TABLE IF EXISTS meal_plans         CASCADE;
DROP TABLE IF EXISTS user_ratings       CASCADE;
DROP TABLE IF EXISTS recipe_tags        CASCADE;
DROP TABLE IF EXISTS nutrition_data     CASCADE;
DROP TABLE IF EXISTS recipe_ingredients CASCADE;
DROP TABLE IF EXISTS recipes            CASCADE;
DROP TABLE IF EXISTS ingredients        CASCADE;
DROP TABLE IF EXISTS categories         CASCADE;
DROP TABLE IF EXISTS users              CASCADE;

-- Also drop the trigger function so it can be recreated cleanly
DROP FUNCTION IF EXISTS save_recipe_version() CASCADE;


-- ============================================================
--  TABLE 1: categories
--  OWNER: Ahmad
--  FIXED: Added Dessert, Beverage, Other to match AddRecipePanel
--         combobox order exactly (index 0-6 = category_id 1-7)
-- ============================================================

CREATE TABLE categories (
    category_id  SERIAL      PRIMARY KEY,
    name         VARCHAR(80) NOT NULL UNIQUE,
    type         VARCHAR(20) NOT NULL,
    description  TEXT
);

-- IMPORTANT: Insert order MUST match AddRecipePanel's JComboBox order:
-- {"Breakfast","Lunch","Dinner","Dessert","Snack","Beverage","Other"}
-- categoryCombo.getSelectedIndex() + 1 = category_id stored in recipes
INSERT INTO categories (name, type) VALUES
    ('Breakfast',   'MEAL_TYPE'),   -- category_id = 1  ← index 0 in combobox
    ('Lunch',       'MEAL_TYPE'),   -- category_id = 2  ← index 1
    ('Dinner',      'MEAL_TYPE'),   -- category_id = 3  ← index 2
    ('Dessert',     'MEAL_TYPE'),   -- category_id = 4  ← index 3
    ('Snack',       'MEAL_TYPE'),   -- category_id = 5  ← index 4
    ('Beverage',    'MEAL_TYPE'),   -- category_id = 6  ← index 5
    ('Other',       'CUISINE');     -- category_id = 7  ← index 6


-- ============================================================
--  TABLE 2: users
--  OWNER: Ahmad
-- ============================================================

CREATE TABLE users (
    user_id          SERIAL       PRIMARY KEY,
    username         VARCHAR(50)  NOT NULL UNIQUE,
    email            VARCHAR(100) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    theme_preference VARCHAR(10)  DEFAULT 'light',
    created_at       TIMESTAMP    DEFAULT NOW()
);


-- ============================================================
--  TABLE 3: ingredients
--  OWNER: Bilal

CREATE TABLE ingredients (
    id                SERIAL        PRIMARY KEY,        -- WAS: ingredient_id
    name              VARCHAR(100)  NOT NULL UNIQUE,
    unit              VARCHAR(30)   NOT NULL DEFAULT 'unit',
    allergen_flag     BOOLEAN       DEFAULT FALSE,       -- WAS: allergen_tags TEXT
    calories_per_unit DOUBLE PRECISION                   -- kept for Uzair
);


-- ============================================================
--  TABLE 4: recipes
--  OWNER: Bilal
-- ============================================================

CREATE TABLE recipes (
    id             SERIAL          PRIMARY KEY,              -- WAS: recipe_id
    user_id        INT             NOT NULL
                                   REFERENCES users(user_id)
                                   ON DELETE CASCADE,         -- WAS: created_by
    category_id    INT             DEFAULT 1
                                   REFERENCES categories(category_id)
                                   ON DELETE SET DEFAULT,     -- NEW column
    title          VARCHAR(200)    NOT NULL,
    description    TEXT,
    prep_time      INT             DEFAULT 0
                                   CHECK (prep_time >= 0),    -- NEW column
    cook_time      INT             DEFAULT 0
                                   CHECK (cook_time >= 0),    -- WAS: cook_time_mins
    servings       INT             DEFAULT 1
                                   CHECK (servings > 0),
    difficulty     VARCHAR(10)     DEFAULT 'Easy',            -- NEW column
    estimated_cost DECIMAL(10, 2)  DEFAULT 0.00,              -- NEW column
    cook_count     INT             DEFAULT 0,                 -- NEW column
    photo_path     VARCHAR(500),
    steps          TEXT,                                      -- NEW column
    created_at     TIMESTAMP       DEFAULT NOW()
);

CREATE INDEX idx_recipes_user_id    ON recipes(user_id);
CREATE INDEX idx_recipes_cook_count ON recipes(cook_count DESC);


-- ============================================================
--  TABLE 5: recipe_tags  (junction: recipe  category)
--  OWNER: Bilal
--  FIXED: FK now references recipes(id) instead of recipes(recipe_id)
-- ============================================================

CREATE TABLE recipe_tags (
    recipe_id    INT  NOT NULL REFERENCES recipes(id)               ON DELETE CASCADE,  -- WAS: recipes(recipe_id)
    category_id  INT  NOT NULL REFERENCES categories(category_id)   ON DELETE CASCADE,
    PRIMARY KEY (recipe_id, category_id)
);


-- ============================================================
--  TABLE 6: recipe_ingredients  (junction: recipe ingredient)
--  OWNER: Bilal
-- ============================================================

CREATE TABLE recipe_ingredients (
    id             SERIAL         PRIMARY KEY,                        -- NEW: needed by RecipeDAO.getGeneratedKeys()
    recipe_id      INT            NOT NULL
                                  REFERENCES recipes(id)
                                  ON DELETE CASCADE,                  -- WAS: recipes(recipe_id)
    ingredient_id  INT            NOT NULL
                                  REFERENCES ingredients(id),         -- WAS: ingredients(ingredient_id)
    quantity       DECIMAL(10, 3) NOT NULL DEFAULT 0,
    price_per_unit DECIMAL(10, 4) DEFAULT 0                          -- NEW: needed by RecipeDAO INSERT
);

CREATE INDEX idx_ri_recipe_id ON recipe_ingredients(recipe_id);


-- ============================================================
--  TABLE 7: nutrition_data
--  OWNER: Uzair
-- ============================================================

CREATE TABLE nutrition_data (
    recipe_id  INT              PRIMARY KEY
                                REFERENCES recipes(id)
                                ON DELETE CASCADE,                    -- WAS: recipes(recipe_id)
    calories   DOUBLE PRECISION,
    protein_g  DOUBLE PRECISION,
    carbs_g    DOUBLE PRECISION,
    fat_g      DOUBLE PRECISION
);


-- ============================================================
--  TABLE 8: meal_plans
--  OWNER: Uzair
-- ============================================================

CREATE TABLE meal_plans (
    plan_id         SERIAL       PRIMARY KEY,
    user_id         INT          NOT NULL
                                 REFERENCES users(user_id)
                                 ON DELETE CASCADE,
    recipe_id       INT          NOT NULL
                                 REFERENCES recipes(id),              -- WAS: recipes(recipe_id)
    week_start_date DATE         NOT NULL,                            -- WAS: plan_date
    day_of_week     VARCHAR(10)  NOT NULL,                            -- NEW: 'Monday' … 'Sunday'
    meal_slot       VARCHAR(20)  NOT NULL                             -- 'BREAKFAST','LUNCH','DINNER'
);

CREATE INDEX idx_mp_user_week ON meal_plans(user_id, week_start_date);


-- ============================================================
--  TABLE 9: user_ratings
--  OWNER: Raja
-- ============================================================

CREATE TABLE user_ratings (
    user_id    INT       NOT NULL REFERENCES users(user_id)  ON DELETE CASCADE,
    recipe_id  INT       NOT NULL REFERENCES recipes(id),             -- WAS: recipes(recipe_id)
    rating     INT       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    notes      TEXT,
    rated_at   TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, recipe_id)
);


-- ============================================================
--  TABLE 10: grocery_lists
--  OWNER: Uzair 
-- ============================================================

CREATE TABLE grocery_lists (
    list_id          SERIAL  PRIMARY KEY,
    user_id          INT     NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    week_start_date  DATE    NOT NULL,
    aggregated_items TEXT,
    generated_at     TIMESTAMP DEFAULT NOW()
);


-- ============================================================
--  TABLE 11: recipe_versions
--  OWNER: Ahmad / Shared
-- ============================================================

CREATE TABLE recipe_versions (
    version_id     SERIAL    PRIMARY KEY,
    recipe_id      INT       NOT NULL
                             REFERENCES recipes(id)
                             ON DELETE CASCADE,                       -- WAS: recipes(recipe_id)
    changed_by     INT       NOT NULL REFERENCES users(user_id),
    snapshot_json  TEXT      NOT NULL,
    version_number INT       NOT NULL,
    changed_at     TIMESTAMP DEFAULT NOW()
);


CREATE OR REPLACE FUNCTION save_recipe_version()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO recipe_versions (recipe_id, changed_by, snapshot_json, version_number)
    VALUES (
        NEW.id,                                                     
        NEW.user_id,                                                 
        row_to_json(NEW)::TEXT,
        (SELECT COALESCE(MAX(version_number), 0) + 1
         FROM recipe_versions
         WHERE recipe_id = NEW.id)                                  
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER recipe_version_trigger
AFTER INSERT OR UPDATE ON recipes
FOR EACH ROW
EXECUTE FUNCTION save_recipe_version();



SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- Quick column check on the three critical tables:
SELECT 'recipes columns:' AS check_table;
SELECT column_name, data_type FROM information_schema.columns
WHERE table_name = 'recipes' ORDER BY ordinal_position;

SELECT 'ingredients columns:' AS check_table;
SELECT column_name, data_type FROM information_schema.columns
WHERE table_name = 'ingredients' ORDER BY ordinal_position;

SELECT 'recipe_ingredients columns:' AS check_table;
SELECT column_name, data_type FROM information_schema.columns
WHERE table_name = 'recipe_ingredients' ORDER BY ordinal_position;

SELECT 'meal_plans columns:' AS check_table;
SELECT column_name, data_type FROM information_schema.columns
WHERE table_name = 'meal_plans' ORDER BY ordinal_position;