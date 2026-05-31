package com.recipe.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class Recipe {

    private int id;
    private int userId;
    private int categoryId;
    private String title;
    private String description;
    private int prepTime;
    private int cookTime;
    private int servings;
    private String difficulty;
    private BigDecimal estimatedCost;
    private int cookCount;
    private LocalDateTime createdAt;
    private String photoPath;
    private String steps;

    // CONSTRUCTORS

    public Recipe() {
        this.cookCount     = 0;
        this.estimatedCost = BigDecimal.ZERO;
    }

    public Recipe(String title, String description,
                  int prepTime, int cookTime, int servings, int categoryId) 
    {
        this.title         = title;
        this.description   = description;
        this.prepTime      = prepTime;
        this.cookTime      = cookTime;
        this.servings      = servings;
        this.categoryId    = categoryId;
        this.cookCount     = 0;
        this.estimatedCost = BigDecimal.ZERO;
    }


    // Getters

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPrepTime() {
        return prepTime;
    }

    public int getCookTime() {
        return cookTime;
    }

    public int getTotalTime() {
        return prepTime + cookTime;
    }

    public int getServings() {
        return servings;
    }

    /* Difficulty label: "Easy", "Medium", or "Hard". */
    public String getDifficulty() {
        return difficulty;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public int getCookCount() {
        return cookCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public String getSteps() {
        return steps;
    }


    // Setters

    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrepTime(int prepTime) {
        this.prepTime = prepTime;
    }

    public void setCookTime(int cookTime) {
        this.cookTime = cookTime;
    }

    public void setServings(int servings) {
        this.servings = servings;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public void setCookCount(int cookCount) {
        this.cookCount = cookCount;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }


    // Special Methods

    
    //  Increments the cook count by 1.
    //  Called in RecipeDetailPanel when the user clicks "Start Cooking".
    //  After calling this, RecipeService calls RecipeDAO.incrementCookCount()
    //  to persist the change to the database.
     
    public void incrementCookCount() {
        this.cookCount++;
    }

     // Useful for debug logging and quick console inspection.

    public String toString() {
        return "Recipe["
                + "id=" + id
                + ", title='" + title + "'"
                + ", prepTime=" + prepTime
                + "min, cookTime=" + cookTime
                + "min, servings=" + servings
                + ", difficulty='" + difficulty + "'"
                + ", cost=" + estimatedCost
                + ", cookCount=" + cookCount
                + "]";
    }

    
    // Two Recipe objects are equal if they have the same DB id.
    // Used when comparing lists (e.g., checking if a recipe is already in meal plan).
  
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Recipe)) return false;
        Recipe other = (Recipe) obj;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
