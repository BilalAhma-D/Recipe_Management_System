package com.recipe.gui;

import com.recipe.database.MealPlanDAO;
import com.recipe.models.User;
import com.recipe.services.MealPlanService;
import com.recipe.services.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  FILE        : GroceryListPanel.java
 *  PACKAGE     : com.recipe.gui
 *  AUTHOR      : Uzair
 *  MODULE      : Meal Planning & Nutrition
 * ============================================================
 *
 *  WHAT THIS SCREEN DOES:
 *  -----------------------
 *  Shows a checklist of all ingredients needed for the current
 *  week's meal plan.
 *
 *  Each ingredient is shown as a JCheckBox. When checked, the
 *  text is struck through (like a real shopping list).
 *  The user can clear all checks or refresh the list from the DB.
 *
 *  HOW IT GETS ITS DATA:
 *  ----------------------
 *  1. MealPlanDAO.getRecipeIdsForWeek() → List of recipe IDs
 *  2. For each recipe ID, query recipe_ingredients JOIN ingredients
 *     to get ingredient names + quantities.
 *  3. Display one JCheckBox per ingredient line.
 *
 *  NOTE FOR INTEGRATION:
 *  ----------------------
 *  The loadIngredients() method currently shows a placeholder list.
 *  Replace the placeholder block with a real query once Bilal's
 *  RecipeIngredient table is set up. The connection code pattern
 *  is already written for you — just swap the data source.
 *
 *  VIVA TIP:
 *  ---------
 *  "GroceryListPanel uses a custom renderer on each JCheckBox to
 *   apply a strikethrough style when checked, giving visual
 *   feedback that an item has been picked up. The renderer uses
 *   HTML formatting inside the JCheckBox label."
 */

public class GroceryListPanel extends JPanel {

    // =====================================================
    // COLORS
    // =====================================================

    private static final Color BG_DARK =
            new Color(15, 15, 30);

    private static final Color CARD_BG =
            new Color(25, 28, 50);

    private static final Color ACCENT_BLUE =
            new Color(79, 195, 247);

    private static final Color TEXT_WHITE =
            new Color(230, 235, 245);

    private static final Color TEXT_MUTED =
            new Color(140, 145, 165);

    private static final Color BORDER_DIM =
            new Color(60, 65, 100);

    // =====================================================
    // SERVICES
    // =====================================================

    private MealPlanDAO mealPlanDAO;

    private MealPlanService mealPlanService;

    // =====================================================
    // UI COMPONENTS
    // =====================================================

    private JPanel checklistPanel;

    private JLabel countLabel;

    private JLabel weekLabel;

    // Store checkboxes
    private List<JCheckBox> checkBoxes =
            new ArrayList<>();

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public GroceryListPanel() {

        mealPlanDAO = new MealPlanDAO();

        mealPlanService = new MealPlanService();

        buildLayout();

        loadIngredients();
    }

    // =====================================================
    // BUILD LAYOUT
    // =====================================================

    private void buildLayout() {

        setLayout(new BorderLayout());

        setBackground(BG_DARK);

        // =================================================
        // TOP BAR
        // =================================================

        JPanel topBar =
                new JPanel(new BorderLayout());

        topBar.setBackground(CARD_BG);

        topBar.setBorder(
                new EmptyBorder(15, 20, 15, 20)
        );

        JLabel title =
                new JLabel("Grocery List");

        title.setForeground(TEXT_WHITE);

        title.setFont(
                new Font("Segoe UI", Font.BOLD, 20)
        );

        topBar.add(title, BorderLayout.WEST);

        // Right buttons
        JPanel buttonPanel =
                new JPanel(new FlowLayout());

        buttonPanel.setBackground(CARD_BG);

        JButton refreshButton =
                createButton("↻ Refresh");

        JButton clearButton =
                createButton("✓ Clear All");

        refreshButton.addActionListener(e -> {

            loadIngredients();
        });

        clearButton.addActionListener(e -> {

            clearAllChecks();
        });

        buttonPanel.add(refreshButton);

        buttonPanel.add(clearButton);

        topBar.add(buttonPanel, BorderLayout.EAST);

        // =================================================
        // SUB BAR
        // =================================================

        JPanel subBar =
                new JPanel(new BorderLayout());

        subBar.setBackground(BG_DARK);

        subBar.setBorder(
                new EmptyBorder(8, 20, 5, 20)
        );

        LocalDate weekStart =
                mealPlanService.getCurrentWeekStart();

        weekLabel =
                new JLabel("Week of " + weekStart);

        weekLabel.setForeground(ACCENT_BLUE);

        countLabel = new JLabel(" ");

        countLabel.setForeground(TEXT_MUTED);

        subBar.add(weekLabel, BorderLayout.WEST);

        subBar.add(countLabel, BorderLayout.EAST);

        // =================================================
        // CHECKLIST PANEL
        // =================================================

        checklistPanel = new JPanel();

        checklistPanel.setLayout(
                new BoxLayout(
                        checklistPanel,
                        BoxLayout.Y_AXIS
                )
        );

        checklistPanel.setBackground(BG_DARK);

        checklistPanel.setBorder(
                new EmptyBorder(10, 20, 20, 20)
        );

        JScrollPane scrollPane =
                new JScrollPane(checklistPanel);

        scrollPane.setBorder(null);

        scrollPane.getViewport()
                .setBackground(BG_DARK);

        // =================================================
        // NORTH PANEL
        // =================================================

        JPanel northPanel =
                new JPanel(new BorderLayout());

        northPanel.setBackground(BG_DARK);

        northPanel.add(topBar, BorderLayout.NORTH);

        northPanel.add(subBar, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);

        add(scrollPane, BorderLayout.CENTER);
    }

    // =====================================================
    // LOAD INGREDIENTS
    // =====================================================

    public void loadIngredients() {

        checklistPanel.removeAll();

        checkBoxes.clear();

        User user =
                SessionManager.getInstance()
                        .getCurrentUser();

        // No user
        if (user == null) {

            addSectionHeader(
                    "Please log in first."
            );

            checklistPanel.revalidate();

            checklistPanel.repaint();

            return;
        }

        LocalDate weekStart =
                mealPlanService.getCurrentWeekStart();

        List<Integer> recipeIds =
                mealPlanDAO.getRecipeIdsForWeek(
                        user.getId(),
                        weekStart
                );

        // No recipes
        if (recipeIds.isEmpty()) {

            addSectionHeader(
                    "No recipes planned this week."
            );

            addHintLabel(
                    "Use Meal Planner to add meals."
            );
        }

        // Recipes found
        else {

            addSectionHeader(
                    "Ingredients for this week"
            );

            // Placeholder
            addRow(
                    "🥕 Integration placeholder",
                    "",
                    ""
            );

            addRow(
                    "📋 Recipe IDs: " + recipeIds,
                    "",
                    ""
            );
        }

        updateCountLabel();

        checklistPanel.revalidate();

        checklistPanel.repaint();
    }

    // =====================================================
    // ADD ROW
    // =====================================================

    public void addRow(
            String name,
            String quantity,
            String unit
    ) {

        String text = name;

        // Add quantity if exists
        if (!quantity.isEmpty()) {

            text =
                    name + " - " + quantity + " " + unit;
        }

        JCheckBox checkBox =
                new JCheckBox(text);

        checkBox.setForeground(TEXT_WHITE);

        checkBox.setBackground(BG_DARK);

        checkBox.setFont(
                new Font("Segoe UI", Font.PLAIN, 14)
        );

        checkBox.setBorder(
                new EmptyBorder(5, 5, 5, 5)
        );

        checkBox.setFocusPainted(false);

        String originalText = text;

        // Checkbox click
        checkBox.addActionListener(e -> {

            // Checked
            if (checkBox.isSelected()) {

                checkBox.setText(
                        "<html><strike>"
                                + originalText
                                + "</strike></html>"
                );
            }

            // Unchecked
            else {

                checkBox.setText(originalText);
            }

            updateCountLabel();
        });

        checkBoxes.add(checkBox);

        checklistPanel.add(checkBox);

        // Separator line
        JSeparator separator =
                new JSeparator();

        separator.setForeground(BORDER_DIM);

        checklistPanel.add(separator);
    }

    // =====================================================
    // SECTION HEADER
    // =====================================================

    private void addSectionHeader(String text) {

        JLabel label =
                new JLabel(text);

        label.setForeground(ACCENT_BLUE);

        label.setFont(
                new Font("Segoe UI", Font.BOLD, 14)
        );

        label.setBorder(
                new EmptyBorder(10, 0, 10, 0)
        );

        checklistPanel.add(label);
    }

    // =====================================================
    // HINT LABEL
    // =====================================================

    private void addHintLabel(String text) {

        JLabel label =
                new JLabel(text);

        label.setForeground(TEXT_MUTED);

        label.setFont(
                new Font("Segoe UI", Font.ITALIC, 12)
        );

        checklistPanel.add(label);
    }

    // =====================================================
    // CLEAR ALL CHECKS
    // =====================================================

    private void clearAllChecks() {

        for (JCheckBox checkBox : checkBoxes) {

            checkBox.setSelected(false);

            String text =
                    checkBox.getText();

            // Remove HTML tags
            text = text.replace(
                    "<html><strike>",
                    ""
            );

            text = text.replace(
                    "</strike></html>",
                    ""
            );

            checkBox.setText(text);
        }

        updateCountLabel();
    }

    // =====================================================
    // UPDATE COUNT LABEL
    // =====================================================

    private void updateCountLabel() {

        int remaining = 0;

        int total = checkBoxes.size();

        // Count unchecked items
        for (JCheckBox checkBox : checkBoxes) {

            if (!checkBox.isSelected()) {

                remaining++;
            }
        }

        countLabel.setText(
                remaining
                        + " / "
                        + total
                        + " items remaining"
        );
    }

    // =====================================================
    // CREATE BUTTON
    // =====================================================

    private JButton createButton(String text) {

        JButton button =
                new JButton(text);

        button.setBackground(
                new Color(40, 60, 90)
        );

        button.setForeground(TEXT_WHITE);

        button.setFocusPainted(false);

        button.setPreferredSize(
                new Dimension(110, 30)
        );

        return button;
    }
}