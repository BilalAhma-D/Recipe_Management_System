package com.recipe.gui;

import com.recipe.models.MealPlan;
import com.recipe.models.Nutrition;
import com.recipe.models.User;
import com.recipe.services.MealPlanService;
import com.recipe.services.NutritionCalculator;
import com.recipe.services.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ============================================================
 *  FILE        : MealPlannerPanel.java
 *  PACKAGE     : com.recipe.gui
 *  AUTHOR      : Uzair
 *  MODULE      : Meal Planning & Nutrition
 * ============================================================
 *
 *  WHAT THIS SCREEN DOES:
 *  -----------------------
 *  A 7-day meal planner grid showing 3 rows × 7 columns = 21 cells.
 *  Each cell represents one meal slot (e.g. Monday Breakfast).
 *
 *  Features:
 *    - Each cell shows the planned recipe name or "+ Add"
 *    - Clicking a cell opens a dialog to enter a recipe ID
 *    - Clicking a filled cell shows an option to remove it
 *    - Weekly navigation (Previous / Next week)
 *    - Weekly nutrition summary card below the grid
 *
 *  HOW IT INTEGRATES:
 *    MealPlannerPanel  →  MealPlanService  →  MealPlanDAO  →  DB
 *    MealPlannerPanel  →  NutritionCalculator  →  NutritionDAO  →  DB
 *
 *  VIVA TIP:
 *  ---------
 *  "MealPlannerPanel uses a HashMap keyed by 'DAY|MEAL' (e.g.
 *   'Monday|Lunch') to efficiently look up which recipe is in
 *   each cell when painting the grid, rather than scanning a list
 *   on every paint call."
 *
 *
 * Beginner Friendly Version
 * -------------------------
 * SAME GUI + SAME THEME
 * BUT:
 * - Removed HashMap
 * - Removed custom paintComponent()
 * - Removed RoundRectangle2D
 * - Removed advanced UI tricks
 * - Simplified logic
 * - Easier loops and conditions
 */

public class MealPlannerPanel extends JPanel {

    // =====================================================
    // COLORS
    // =====================================================

    private static final Color BG_DARK = new Color(15, 15, 30);
    private static final Color CARD_BG = new Color(25, 28, 50);
    private static final Color ACCENT_BLUE = new Color(79, 195, 247);
    private static final Color TEXT_WHITE = new Color(230, 235, 245);
    private static final Color TEXT_MUTED = new Color(140, 145, 165);
    private static final Color HEADER_BG = new Color(20, 22, 42);
    private static final Color CELL_FILLED = new Color(40, 70, 90);
    private static final Color CELL_EMPTY = new Color(30, 33, 58);

    // =====================================================
    // VARIABLES
    // =====================================================

    private MealPlanService mealPlanService;
    private NutritionCalculator nutritionCalculator;

    private LocalDate currentWeekStart;

    // UI Components
    private JPanel gridPanel;

    private JLabel weekLabel;

    private JLabel caloriesLabel;
    private JLabel proteinLabel;
    private JLabel carbsLabel;
    private JLabel fatLabel;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MealPlannerPanel() {

        mealPlanService = new MealPlanService();

        nutritionCalculator = new NutritionCalculator();

        currentWeekStart =
                mealPlanService.getCurrentWeekStart();

        setLayout(new BorderLayout());

        setBackground(BG_DARK);

        buildTopBar();

        buildCenterPanel();

        loadWeek();
    }

    // =====================================================
    // TOP BAR
    // =====================================================

    private void buildTopBar() {

        JPanel topBar = new JPanel(new BorderLayout());

        topBar.setBackground(CARD_BG);

        topBar.setBorder(
                new EmptyBorder(15, 20, 15, 20)
        );

        // Title
        JLabel title =
                new JLabel("Weekly Meal Planner");

        title.setForeground(TEXT_WHITE);

        title.setFont(
                new Font("Segoe UI", Font.BOLD, 20)
        );

        topBar.add(title, BorderLayout.WEST);

        // Right side
        JPanel rightPanel =
                new JPanel(new FlowLayout());

        rightPanel.setBackground(CARD_BG);

        weekLabel = new JLabel();

        weekLabel.setForeground(ACCENT_BLUE);

        updateWeekLabel();

        JButton prevButton =
                createButton("◀ Prev Week");

        JButton nextButton =
                createButton("Next Week ▶");

        // Previous week
        prevButton.addActionListener(e -> {

            currentWeekStart =
                    currentWeekStart.minusWeeks(1);

            updateWeekLabel();

            loadWeek();
        });

        // Next week
        nextButton.addActionListener(e -> {

            currentWeekStart =
                    currentWeekStart.plusWeeks(1);

            updateWeekLabel();

            loadWeek();
        });

        rightPanel.add(weekLabel);
        rightPanel.add(prevButton);
        rightPanel.add(nextButton);

        topBar.add(rightPanel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
    }

    // =====================================================
    // CENTER PANEL
    // =====================================================

    private void buildCenterPanel() {

        JPanel centerPanel = new JPanel();

        centerPanel.setLayout(
                new BoxLayout(centerPanel, BoxLayout.Y_AXIS)
        );

        centerPanel.setBackground(BG_DARK);

        centerPanel.setBorder(
                new EmptyBorder(15, 20, 20, 20)
        );

        // Meal grid
        gridPanel = new JPanel();

        gridPanel.setBackground(BG_DARK);

        centerPanel.add(gridPanel);

        centerPanel.add(Box.createVerticalStrut(20));

        // Nutrition card
        JPanel nutritionPanel = new JPanel();

        nutritionPanel.setBackground(CARD_BG);

        nutritionPanel.setBorder(
                new EmptyBorder(20, 20, 20, 20)
        );

        JLabel title =
                new JLabel("Weekly Nutrition Summary");

        title.setForeground(TEXT_WHITE);

        title.setFont(
                new Font("Segoe UI", Font.BOLD, 15)
        );

        nutritionPanel.add(title);

        caloriesLabel = createNutritionLabel();
        proteinLabel = createNutritionLabel();
        carbsLabel = createNutritionLabel();
        fatLabel = createNutritionLabel();

        nutritionPanel.add(caloriesLabel);
        nutritionPanel.add(proteinLabel);
        nutritionPanel.add(carbsLabel);
        nutritionPanel.add(fatLabel);

        centerPanel.add(nutritionPanel);

        JScrollPane scrollPane =
                new JScrollPane(centerPanel);

        scrollPane.setBorder(null);

        scrollPane.getViewport().setBackground(BG_DARK);

        add(scrollPane, BorderLayout.CENTER);
    }

    // =====================================================
    // LOAD WEEK
    // =====================================================

    public void loadWeek() {

        User user =
                SessionManager.getInstance()
                        .getCurrentUser();

        if (user == null) {
            return;
        }

        // Get meals
        List<MealPlan> mealPlans =
                mealPlanService.getWeekPlan(
                        user,
                        currentWeekStart
                );

        String[] days = MealPlanService.DAYS;

        String[] meals = MealPlanService.MEALS;

        // Clear old grid
        gridPanel.removeAll();

        gridPanel.setLayout(
                new GridLayout(
                        meals.length + 1,
                        days.length + 1,
                        4,
                        4
                )
        );

        // Empty top corner
        gridPanel.add(createHeader(""));

        // Days row
        for (String day : days) {

            gridPanel.add(createHeader(day));
        }

        // Meal rows
        for (String meal : meals) {

            gridPanel.add(createRowLabel(meal));

            for (String day : days) {

                MealPlan foundMeal = null;

                // SIMPLE SEARCH
                // No HashMap
                for (MealPlan plan : mealPlans) {

                    boolean sameDay =
                            plan.getDayOfWeek().equals(day);

                    boolean sameMeal =
                            plan.getMealType().equals(meal);

                    if (sameDay && sameMeal) {

                        foundMeal = plan;

                        break;
                    }
                }

                JButton cellButton;

                // =================================================
                // FILLED CELL
                // =================================================

                if (foundMeal != null) {

                    cellButton =
                            createMealButton(
                                    foundMeal.getRecipeName(),
                                    true
                            );

                    MealPlan finalMeal = foundMeal;

                    cellButton.addActionListener(e -> {

                        int confirm =
                                JOptionPane.showConfirmDialog(
                                        this,
                                        "Remove \""
                                                + finalMeal.getRecipeName()
                                                + "\" ?",
                                        "Remove Meal",
                                        JOptionPane.YES_NO_OPTION
                                );

                        if (confirm == JOptionPane.YES_OPTION) {

                            String result =
                                    mealPlanService.removeMeal(
                                            user,
                                            day,
                                            meal,
                                            currentWeekStart
                                    );

                            if (result.equals("SUCCESS")) {

                                loadWeek();
                            }
                        }
                    });

                }

                // =================================================
                // EMPTY CELL
                // =================================================

                else {

                    cellButton =
                            createMealButton("+ Add", false);

                    cellButton.addActionListener(e -> {

                        String input =
                                JOptionPane.showInputDialog(
                                        this,
                                        "Enter Recipe ID:"
                                );

                        if (input == null || input.isEmpty()) {
                            return;
                        }

                        try {

                            int recipeId =
                                    Integer.parseInt(input);

                            String result =
                                    mealPlanService.assignRecipe(
                                            user,
                                            recipeId,
                                            day,
                                            meal,
                                            currentWeekStart
                                    );

                            if (result.equals("SUCCESS")) {

                                loadWeek();

                            } else {

                                JOptionPane.showMessageDialog(
                                        this,
                                        result
                                );
                            }

                        } catch (Exception ex) {

                            JOptionPane.showMessageDialog(
                                    this,
                                    "Enter valid number"
                            );
                        }
                    });
                }

                gridPanel.add(cellButton);
            }
        }

        gridPanel.revalidate();

        gridPanel.repaint();

        updateNutrition(user);
    }

    // =====================================================
    // UPDATE NUTRITION
    // =====================================================

    private void updateNutrition(User user) {

        Nutrition nutrition =
                nutritionCalculator.calculateWeekTotals(
                        user.getId(),
                        currentWeekStart
                );

        caloriesLabel.setText(
                "Calories: "
                        + (int) nutrition.getCalories()
        );

        proteinLabel.setText(
                "Protein: "
                        + nutrition.getProteinG()
                        + " g"
        );

        carbsLabel.setText(
                "Carbs: "
                        + nutrition.getCarbsG()
                        + " g"
        );

        fatLabel.setText(
                "Fat: "
                        + nutrition.getFatG()
                        + " g"
        );
    }

    // =====================================================
    // UPDATE WEEK LABEL
    // =====================================================

    private void updateWeekLabel() {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM d");

        LocalDate endDate =
                currentWeekStart.plusDays(6);

        weekLabel.setText(
                currentWeekStart.format(formatter)
                        + " - "
                        + endDate.format(formatter)
        );
    }

    // =====================================================
    // UI HELPERS
    // =====================================================

    private JLabel createHeader(String text) {

        JLabel label =
                new JLabel(text, SwingConstants.CENTER);

        label.setOpaque(true);

        label.setBackground(HEADER_BG);

        label.setForeground(ACCENT_BLUE);

        label.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );

        return label;
    }

    private JLabel createRowLabel(String text) {

        JLabel label =
                new JLabel(text, SwingConstants.CENTER);

        label.setOpaque(true);

        label.setBackground(HEADER_BG);

        label.setForeground(TEXT_WHITE);

        label.setPreferredSize(
                new Dimension(80, 70)
        );

        return label;
    }

    private JButton createButton(String text) {

        JButton button = new JButton(text);

        button.setBackground(new Color(40, 60, 90));

        button.setForeground(TEXT_WHITE);

        button.setFocusPainted(false);

        return button;
    }

    private JButton createMealButton(
            String text,
            boolean filled
    ) {

        JButton button =
                new JButton(text);

        button.setFocusPainted(false);

        button.setPreferredSize(
                new Dimension(110, 70)
        );

        if (filled) {

            button.setBackground(CELL_FILLED);

            button.setForeground(ACCENT_BLUE);

        } else {

            button.setBackground(CELL_EMPTY);

            button.setForeground(TEXT_MUTED);
        }

        return button;
    }

    private JLabel createNutritionLabel() {

        JLabel label = new JLabel();

        label.setForeground(TEXT_WHITE);

        label.setFont(
                new Font("Segoe UI", Font.PLAIN, 13)
        );

        return label;
    }
}
