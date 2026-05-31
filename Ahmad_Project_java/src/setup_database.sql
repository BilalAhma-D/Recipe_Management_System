-- ================================================================
--  FILE   : setup_database.sql
--  RUN IN : pgAdmin 4  →  right-click your database → Query Tool
--           Paste everything here and press F5 (or the Run button)
--  ORDER  : Tables must be created in this exact order because
--           of foreign key dependencies (parent before child).
-- ================================================================


-- ================================================================
--  1. USERS  (Ahmad's table — creates this first as other tables FK to it)
-- ================================================================
CREATE TABLE IF NOT EXISTS users (
    user_id          SERIAL PRIMARY KEY,
    username         VARCHAR(50)  UNIQUE NOT NULL,
    email            VARCHAR(100) UNIQUE NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    theme_preference VARCHAR(10)  DEFAULT 'light',
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);


-- ================================================================
--  2. CATEGORIES  (Bilal's lookup table for recipe categories)
-- ================================================================
CREATE TABLE IF NOT EXISTS categories (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- Seed default categories (matches the combo box in AddRecipePanel)
INSERT INTO categories (name) VALUES
    ('Breakfast'),
    ('Lunch'),
    ('Dinner'),
    ('Snack'),
    ('Dessert'),
    ('Drink')
ON CONFLICT (name) DO NOTHING;


-- ================================================================
--  3. RECIPES  (Bilal's main table)
--  NOTE: PK is named "id" — NOT "recipe_id".
--        Uzair's MealPlanDAO references this as recipes(id).
-- ================================================================
CREATE TABLE IF NOT EXISTS recipes (
    id             SERIAL PRIMARY KEY,
    user_id        INT     REFERENCES users(user_id) ON DELETE SET NULL,
    category_id    INT     REFERENCES categories(id) ON DELETE SET NULL,
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    prep_time      INT     DEFAULT 0,
    cook_time      INT     DEFAULT 0,
    servings       INT     DEFAULT 1,
    difficulty     VARCHAR(10),
    estimated_cost NUMERIC(10,2) DEFAULT 0.00,
    cook_count     INT     DEFAULT 0,
    photo_path     VARCHAR(500),
    steps          TEXT,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- ================================================================
--  4. INGREDIENTS  (Bilal's master ingredient list)
-- ================================================================
CREATE TABLE IF NOT EXISTS ingredients (
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(100) UNIQUE NOT NULL,
    unit         VARCHAR(30),
    allergen_flag BOOLEAN DEFAULT FALSE
);


-- ================================================================
--  5. RECIPE_INGREDIENTS  (Bilal's junction table)
-- ================================================================
CREATE TABLE IF NOT EXISTS recipe_ingredients (
    id            SERIAL PRIMARY KEY,
    recipe_id     INT          REFERENCES recipes(id)     ON DELETE CASCADE,
    ingredient_id INT          REFERENCES ingredients(id) ON DELETE CASCADE,
    quantity      NUMERIC(10,3) DEFAULT 0,
    price_per_unit NUMERIC(10,2) DEFAULT 0.00
);


-- ================================================================
--  6. NUTRITION  (Uzair's table)
--  FK references recipes(id) — Bilal's PK column name
-- ================================================================
CREATE TABLE IF NOT EXISTS nutrition (
    nutrition_id SERIAL PRIMARY KEY,
    recipe_id    INT REFERENCES recipes(id) ON DELETE CASCADE,
    calories     DOUBLE PRECISION DEFAULT 0,
    protein_g    DOUBLE PRECISION DEFAULT 0,
    carbs_g      DOUBLE PRECISION DEFAULT 0,
    fat_g        DOUBLE PRECISION DEFAULT 0
);


-- ================================================================
--  7. MEAL_PLANS  (Uzair's table)
--  FK references recipes(id) — Bilal's PK column name
-- ================================================================
CREATE TABLE IF NOT EXISTS meal_plans (
    plan_id         SERIAL PRIMARY KEY,
    user_id         INT         REFERENCES users(user_id)  ON DELETE CASCADE,
    recipe_id       INT         REFERENCES recipes(id)     ON DELETE CASCADE,
    day_of_week     VARCHAR(10) NOT NULL,   -- 'Monday' … 'Sunday'
    meal_type       VARCHAR(15) NOT NULL,   -- 'Breakfast' / 'Lunch' / 'Dinner'
    week_start_date DATE        NOT NULL    -- always the Monday of that week
);


-- ================================================================
--  VERIFY  — run these SELECT statements to confirm all tables exist
-- ================================================================
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
