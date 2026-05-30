-- ============================================================
--  FILE        : schema.sql
--  AUTHOR      : Ahmad
--  PURPOSE     : Creates all PostgreSQL tables for the
--                Recipe Management System.
--  RUN THIS    : Once, before any code is tested.
--                In pgAdmin: File → Open → run this file.
--                In terminal: psql -U postgres -d recipedb -f schema.sql
--
--  TABLE ORDER MATTERS:
--  ----------------------
--  Foreign keys reference other tables, so parent tables must
--  be created BEFORE child tables. Order here is:
--    1. categories  (no dependencies — created first)
--    2. users       (no dependencies)
--    3. recipes     (depends on users, categories)
--    4. ... (Bilal, Uzair, Raja's tables follow)
--
--  AHMAD'S TABLES: users, categories
--  These are the foundation tables for the whole project.
-- ============================================================


-- ============================================================
--  STEP 0: Create the database (run separately if needed)
-- ============================================================
-- CREATE DATABASE recipedb;
-- \c recipedb   -- connect to it in psql


-- ============================================================
--  DROP TABLES (for clean re-runs during development)
--  "CASCADE" drops dependent tables/foreign keys automatically.
--  COMMENT THIS BLOCK OUT in production!
-- ============================================================

DROP TABLE IF EXISTS recipe_versions  CASCADE;
DROP TABLE IF EXISTS grocery_lists    CASCADE;
DROP TABLE IF EXISTS meal_plans       CASCADE;
DROP TABLE IF EXISTS user_ratings     CASCADE;
DROP TABLE IF EXISTS recipe_tags      CASCADE;
DROP TABLE IF EXISTS nutrition_data   CASCADE;
DROP TABLE IF EXISTS recipe_ingredients CASCADE;
DROP TABLE IF EXISTS recipes          CASCADE;
DROP TABLE IF EXISTS ingredients      CASCADE;
DROP TABLE IF EXISTS categories       CASCADE;
DROP TABLE IF EXISTS users            CASCADE;


-- ============================================================
--  TABLE 1: categories
--  OWNER: Ahmad
--  PURPOSE: Lookup table for recipe categories (Breakfast,
--           Vegetarian, etc.). Used by all modules.
-- ============================================================

CREATE TABLE categories (
    -- category_id: auto-incrementing primary key
    -- SERIAL means PostgreSQL assigns 1, 2, 3... automatically
    category_id  SERIAL          PRIMARY KEY,

    -- name: the category name shown in dropdowns (e.g. "Breakfast")
    -- NOT NULL means it must always have a value
    -- UNIQUE means no two categories can have the same name
    name         VARCHAR(80)     NOT NULL UNIQUE,

    -- type: groups categories (e.g. "MEAL_TYPE", "DIETARY", "CUISINE")
    -- This matches the CategoryType enum in Java
    type         VARCHAR(20)     NOT NULL,

    -- description: optional extra info about the category
    description  TEXT
);

-- Add some default categories so the app works from day one
INSERT INTO categories (name, type) VALUES
    ('Breakfast',   'MEAL_TYPE'),
    ('Lunch',       'MEAL_TYPE'),
    ('Dinner',      'MEAL_TYPE'),
    ('Snack',       'MEAL_TYPE'),
    ('Vegetarian',  'DIETARY'),
    ('Vegan',       'DIETARY'),
    ('Gluten-Free', 'DIETARY'),
    ('Italian',     'CUISINE'),
    ('Pakistani',   'CUISINE'),
    ('Chinese',     'CUISINE');


-- ============================================================
--  TABLE 2: users
--  OWNER: Ahmad
--  PURPOSE: Stores registered user accounts.
--           This is the foundation table — nearly every other
--           table has a foreign key pointing here (user_id).
-- ============================================================

CREATE TABLE users (
    -- user_id: auto-incremented primary key
    user_id          SERIAL          PRIMARY KEY,

    -- username: must be unique across all accounts
    -- VARCHAR(50) = max 50 characters
    username         VARCHAR(50)     NOT NULL UNIQUE,

    -- email: must be unique (one account per email)
    email            VARCHAR(100)    NOT NULL UNIQUE,

    -- password_hash: the BCrypt hash of the password
    -- BCrypt hashes are always exactly 60 characters long
    -- We never store the real password — only the hash
    password_hash    VARCHAR(255)    NOT NULL,

    -- theme_preference: "light" or "dark"
    -- DEFAULT 'light' means if no theme is specified, use "light"
    theme_preference VARCHAR(10)     DEFAULT 'light',

    -- created_at: timestamp of when the account was made
    -- DEFAULT NOW() means PostgreSQL fills this automatically
    created_at       TIMESTAMP       DEFAULT NOW()
);


-- ============================================================
--  TABLE 3: ingredients
--  OWNER: Bilal (created here because other tables need it)
-- ============================================================

CREATE TABLE ingredients (
    ingredient_id  SERIAL        PRIMARY KEY,
    name           VARCHAR(100)  NOT NULL UNIQUE,
    unit           VARCHAR(30)   NOT NULL,
    calories_per_unit DOUBLE PRECISION,
    allergen_tags  TEXT          -- comma-separated allergen names
);


-- ============================================================
--  TABLE 4: recipes
--  OWNER: Bilal
--  NOTE: References users(user_id) — users table must exist first
-- ============================================================

CREATE TABLE recipes (
    recipe_id     SERIAL         PRIMARY KEY,
    created_by    INT            NOT NULL REFERENCES users(user_id)  ON DELETE CASCADE,
    title         VARCHAR(200)   NOT NULL,
    description   TEXT,
    servings      INT,
    cook_time_mins INT,
    photo_path    VARCHAR(500),
    created_at    TIMESTAMP      DEFAULT NOW()
);


-- ============================================================
--  TABLE 5: recipe_tags (junction: recipe ↔ category)
--  OWNER: Bilal
-- ============================================================

CREATE TABLE recipe_tags (
    recipe_id    INT  NOT NULL REFERENCES recipes(recipe_id)     ON DELETE CASCADE,
    category_id  INT  NOT NULL REFERENCES categories(category_id) ON DELETE CASCADE,
    PRIMARY KEY (recipe_id, category_id)  -- composite primary key
);


-- ============================================================
--  TABLE 6: recipe_ingredients (junction: recipe ↔ ingredient)
--  OWNER: Bilal
-- ============================================================

CREATE TABLE recipe_ingredients (
    recipe_id     INT             NOT NULL REFERENCES recipes(recipe_id)     ON DELETE CASCADE,
    ingredient_id INT             NOT NULL REFERENCES ingredients(ingredient_id),
    quantity      DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (recipe_id, ingredient_id)
);


-- ============================================================
--  TABLE 7: nutrition_data
--  OWNER: Uzair
-- ============================================================

CREATE TABLE nutrition_data (
    recipe_id  INT             PRIMARY KEY REFERENCES recipes(recipe_id) ON DELETE CASCADE,
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
    plan_id        SERIAL       PRIMARY KEY,
    user_id        INT          NOT NULL REFERENCES users(user_id)   ON DELETE CASCADE,
    recipe_id      INT          NOT NULL REFERENCES recipes(recipe_id),
    plan_date      DATE         NOT NULL,
    meal_slot      VARCHAR(20)  NOT NULL  -- "BREAKFAST", "LUNCH", "DINNER"
);


-- ============================================================
--  TABLE 9: user_ratings
--  OWNER: Raja
-- ============================================================

CREATE TABLE user_ratings (
    user_id    INT       NOT NULL REFERENCES users(user_id)   ON DELETE CASCADE,
    recipe_id  INT       NOT NULL REFERENCES recipes(recipe_id),
    rating     INT       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    notes      TEXT,
    rated_at   TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, recipe_id)   -- one rating per user per recipe
);


-- ============================================================
--  TABLE 10: grocery_lists
--  OWNER: Uzair
-- ============================================================

CREATE TABLE grocery_lists (
    list_id          SERIAL  PRIMARY KEY,
    user_id          INT     NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    week_start_date  DATE    NOT NULL,
    aggregated_items TEXT,   -- JSON string of merged ingredient list
    generated_at     TIMESTAMP DEFAULT NOW()
);


-- ============================================================
--  TABLE 11: recipe_versions  (trigger-populated)
--  OWNER: Ahmad / Shared
--  PURPOSE: Automatically logs a snapshot of a recipe every
--           time it is changed. Filled by a PostgreSQL trigger.
-- ============================================================

CREATE TABLE recipe_versions (
    version_id     SERIAL   PRIMARY KEY,
    recipe_id      INT      NOT NULL REFERENCES recipes(recipe_id) ON DELETE CASCADE,
    changed_by     INT      NOT NULL REFERENCES users(user_id),
    snapshot_json  TEXT     NOT NULL,   -- full recipe data as JSON at time of change
    version_number INT      NOT NULL,
    changed_at     TIMESTAMP DEFAULT NOW()
);


-- ============================================================
--  TRIGGER: recipe_version_trigger
--  PURPOSE: Automatically inserts a row into recipe_versions
--           every time a recipe row is inserted or updated.
--  This means we have a complete history of all recipe changes.
-- ============================================================

-- First, create the trigger function (the logic that runs)
CREATE OR REPLACE FUNCTION save_recipe_version()
RETURNS TRIGGER AS $$
BEGIN
    -- Insert a snapshot of the NEW row into recipe_versions
    -- row_to_json(NEW) converts the updated recipe row to JSON automatically
    INSERT INTO recipe_versions (recipe_id, changed_by, snapshot_json, version_number)
    VALUES (
        NEW.recipe_id,
        NEW.created_by,
        row_to_json(NEW)::TEXT,
        -- Count existing versions for this recipe, then add 1
        (SELECT COALESCE(MAX(version_number), 0) + 1
         FROM recipe_versions
         WHERE recipe_id = NEW.recipe_id)
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Then attach the function to the recipes table
-- AFTER INSERT OR UPDATE: fires after any add or change to a recipe
CREATE TRIGGER recipe_version_trigger
AFTER INSERT OR UPDATE ON recipes
FOR EACH ROW
EXECUTE FUNCTION save_recipe_version();


-- ============================================================
--  VERIFY  — see the tables that were created
-- ============================================================
-- Run this to check everything was created correctly:
-- SELECT table_name FROM information_schema.tables
-- WHERE table_schema = 'public'
-- ORDER BY table_name;
