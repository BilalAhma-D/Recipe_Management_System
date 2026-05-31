package com.recipe.gui;

import com.recipe.models.Recipe;
import com.recipe.models.RecipeIngredient;
import com.recipe.services.CostEstimator;
import com.recipe.services.RecipeService;
import com.recipe.services.ServingScaler;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static com.recipe.gui.RecipeListPanel.*;

/*
 *  PURPOSE:
 *  Full-screen detail view for a selected Recipe object.
 *  Displays: recipe photo, stats bar, description, allergen warning,
 *  scalable ingredient list, numbered cooking steps, cost info,
 *  and a "Start Cooking" button that increments the cook count.

 *  CONNECTS TO:
 *   - RecipeService.getIngredientsForRecipe(id) → ingredient list
 *   - RecipeService.incrementCookCount(recipe)  → Start Cooking
 *   - RecipeService.deleteRecipe(id)            → Delete
 *   - RecipeService.duplicateRecipe(id)         → Duplicate
 *   - ServingScaler.scaleIngredients(...)       → serving adjustment
 *   - AppNavigator.navigateToRecipeList()       → Back / after delete
 *   - AppNavigator.navigateToEditRecipe(recipe) → Edit button
 *   - AppNavigator.navigateToRecipeDetail(...)  → after duplicate
 *
 *  HOW TO OPEN (from RecipeListPanel / via AppNavigator):
 *   RecipeDetailPanel detail = new RecipeDetailPanel(recipe, recipeService, this);
 *   contentArea.add(detail, "RECIPE_DETAIL");
 *   cardLayout.show(contentArea, "RECIPE_DETAIL");
 
 */
public class RecipeDetailPanel extends JPanel {

    //  FIELDS

    private final RecipeService             recipeService;
    private final AppNavigator              navigator;
    private final Recipe                    recipe;

    /** Original ingredients loaded from DB — NEVER modified (used as base for scaling) */
    private List<RecipeIngredient> originalIngredients = new ArrayList<>();

    // Live UI components (updated at runtime)
    private JPanel  ingredientsListPanel;    // Rebuilt on serving change
    private JSpinner servingsSpinner;        // Controls serving scale
    private JLabel  cookCountLabel;          // Updated after Start Cooking
    private JPanel  allergenBanner;          // Shown when allergens found
    private JPanel  contentBody;             // Outer scrollable body

    //  EXTRA THEME COLORS  (on top of the ones in RecipeListPanel)

    // Allergen warning colours
    private static final Color ALLERGEN_BG     = new Color(50, 18, 18);
    private static final Color ALLERGEN_BORDER = new Color(180, 50, 50);
    private static final Color ALLERGEN_TEXT   = new Color(255, 110, 110);

    // Delete button colours
    private static final Color DELETE_BG       = new Color(110, 20, 20);
    private static final Color DELETE_HOVER    = new Color(160, 30, 30);
    private static final Color DELETE_FG       = new Color(255, 120, 120);

    // Start Cooking button colours
    private static final Color COOK_GREEN      = new Color(34, 160, 80);
    private static final Color COOK_DARK       = new Color(20, 130, 60);

    // Stat box
    private static final Color STAT_BG         = new Color(12, 20, 34);


    //  CONSTRUCTOR

    public RecipeDetailPanel(Recipe recipe, RecipeService recipeService, AppNavigator navigator) {
        this.recipe         = recipe;
        this.recipeService  = recipeService;
        this.navigator      = navigator;
        setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));
        initComponents();
        loadIngredients();   // Background DB call
    }

    //  MAIN UI SETUP

    private void initComponents() {

        //  HEADER  BAR  (non-scrolling)
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(BG_DARK);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
            BorderFactory.createEmptyBorder(13, 22, 13, 22)
        ));

        // Left: Back button
        JButton backBtn = buildSecondaryButton("← Back to Recipes");
        backBtn.addActionListener(e -> navigator.navigateToRecipeList());

        // Centre: Recipe title (truncated if too long)
        String shortTitle = recipe.getTitle().length() > 50
            ? recipe.getTitle().substring(0, 48) + "…"
            : recipe.getTitle();
        JLabel headerTitle = new JLabel(shortTitle);
        headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        headerTitle.setForeground(TEXT_PRIMARY);
        headerTitle.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        // Right: action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setBackground(BG_DARK);

        JButton duplicateBtn = buildSecondaryButton("⧉  Duplicate");
        duplicateBtn.setToolTipText("Create a copy of this recipe");
        duplicateBtn.addActionListener(e -> handleDuplicate());

        JButton editBtn = buildPrimaryButton("✎  Edit Recipe");
        editBtn.setToolTipText("Edit this recipe");
        editBtn.addActionListener(e -> navigator.navigateToEditRecipe(recipe));

        JButton deleteBtn = buildDeleteButton("🗑  Delete");
        deleteBtn.setToolTipText("Permanently delete this recipe");
        deleteBtn.addActionListener(e -> handleDelete());

        actionPanel.add(duplicateBtn);
        actionPanel.add(editBtn);
        actionPanel.add(deleteBtn);

        header.add(backBtn,     BorderLayout.WEST);
        header.add(headerTitle, BorderLayout.CENTER);
        header.add(actionPanel, BorderLayout.EAST);


        //  SCROLLABLE  BODY
        contentBody = new JPanel();
        contentBody.setBackground(BG_DARK);
        contentBody.setLayout(new BoxLayout(contentBody, BoxLayout.Y_AXIS));
        contentBody.setBorder(BorderFactory.createEmptyBorder(22, 28, 32, 28));

        // 1. Hero card: photo + stats
        contentBody.add(buildHeroSection());
        contentBody.add(Box.createVerticalStrut(16));

        // 2. Allergen banner (hidden by default; shown after loadIngredients() finishes)
        allergenBanner = buildAllergenBanner();
        allergenBanner.setVisible(false);
        allergenBanner.setAlignmentX(LEFT_ALIGNMENT);
        contentBody.add(allergenBanner);
        // The strut below it is always present; banner visibility controls whether it looks right
        contentBody.add(Box.createVerticalStrut(8));

        // 3. Ingredients card (with serving scaler)
        contentBody.add(buildIngredientsSection());
        contentBody.add(Box.createVerticalStrut(16));

        // 4. Cooking steps card
        contentBody.add(buildStepsSection());
        contentBody.add(Box.createVerticalStrut(16));

        // 5. Start Cooking centred row
        contentBody.add(buildCookingSection());
        contentBody.add(Box.createVerticalStrut(24));

        JScrollPane scroll = new JScrollPane(contentBody);
        scroll.setBorder(null);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(header, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);
    }


    //  SECTION  BUILDERS

    /**
     * Hero section: large photo on the left, title + stats on the right.
     * Uses GridBagLayout so that the photo occupies a fixed column and
     * the stats fill the remaining space.
     */
    private JPanel buildHeroSection() {
        JPanel card = buildCard();
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill   = GridBagConstraints.BOTH;

        //Photo 
        JPanel photoWrapper = new JPanel(new GridBagLayout());
        photoWrapper.setOpaque(false);
        photoWrapper.setPreferredSize(new Dimension(280, 210));

        JPanel photoContent = loadPhotoPanel(280, 210);
        photoWrapper.add(photoContent);

        c.gridx = 0; c.gridy = 0;
        c.weightx = 0.0; c.weighty = 1.0;
        c.insets = new Insets(18, 18, 18, 10);
        card.add(photoWrapper, c);

        //Stats / info column
        JPanel stats = buildStatsPanel();

        c.gridx = 1; c.gridy = 0;
        c.weightx = 1.0; c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(18, 8, 18, 18);
        card.add(stats, c);

        return card;
    }

    /**
     * Loads the recipe photo from disk and returns a panel showing it.
     * Falls back to a placeholder if the path is null, missing, or unreadable.
     */
    private JPanel loadPhotoPanel(int w, int h) {
        if (recipe.getPhotoPath() != null && !recipe.getPhotoPath().isEmpty()) {
            try {
                BufferedImage img = ImageIO.read(new File(recipe.getPhotoPath()));
                if (img != null) {
                    Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                    JPanel wrapper = new JPanel(new GridBagLayout());
                    wrapper.setPreferredSize(new Dimension(w, h));
                    wrapper.setOpaque(false);
                    JLabel imgLbl = new JLabel(new ImageIcon(scaled));
                    imgLbl.setBorder(new LineBorder(BORDER, 1, true));
                    wrapper.add(imgLbl);
                    return wrapper;
                }
            } catch (IOException ignored) { /* fall through */ }
        }
        return buildPhotoPlaceholder(w, h);
    }

    /**
     * Builds the right-side stats panel inside the hero section.
     * Contains: title, description, difficulty badge, stat boxes
     * (prep / cook / total / servings), estimated cost, cook count, date.
     */
    private JPanel buildStatsPanel() {
        JPanel stats = new JPanel();
        stats.setLayout(new BoxLayout(stats, BoxLayout.Y_AXIS));
        stats.setOpaque(false);

        // Recipe title (large)
        JLabel titleLbl = new JLabel(
            "<html><div style='width:340px;'>" + escapeHtml(recipe.getTitle()) + "</div></html>"
        );
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 21));
        titleLbl.setForeground(TEXT_PRIMARY);
        titleLbl.setAlignmentX(LEFT_ALIGNMENT);

        stats.add(titleLbl);
        stats.add(Box.createVerticalStrut(8));

        // Description (optional)
        if (recipe.getDescription() != null && !recipe.getDescription().trim().isEmpty()) {
            JLabel descLbl = new JLabel(
                "<html><div style='width:340px;color:#82949a'>"
                + escapeHtml(recipe.getDescription()) + "</div></html>"
            );
            descLbl.setFont(FONT_BODY);
            descLbl.setForeground(TEXT_SECONDARY);
            descLbl.setAlignmentX(LEFT_ALIGNMENT);
            stats.add(descLbl);
            stats.add(Box.createVerticalStrut(12));
        }

        // Difficulty badge
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeRow.setOpaque(false);
        badgeRow.setAlignmentX(LEFT_ALIGNMENT);
        badgeRow.add(buildDifficultyBadge(recipe.getDifficulty()));
        stats.add(badgeRow);
        stats.add(Box.createVerticalStrut(14));

        // Stat boxes row (prep / cook / total / servings)
        JPanel statBoxRow = new JPanel(new GridLayout(1, 4, 10, 0));
        statBoxRow.setOpaque(false);
        statBoxRow.setAlignmentX(LEFT_ALIGNMENT);
        statBoxRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        statBoxRow.add(buildStatBox("⏱ Prep",   recipe.getPrepTime()  + " min"));
        statBoxRow.add(buildStatBox("🍳 Cook",   recipe.getCookTime()  + " min"));
        statBoxRow.add(buildStatBox("⏰ Total",  recipe.getTotalTime() + " min"));
        statBoxRow.add(buildStatBox("🍽 Serves", String.valueOf(recipe.getServings())));
        stats.add(statBoxRow);
        stats.add(Box.createVerticalStrut(10));

        // Estimated cost (shown only if > 0)
        if (recipe.getEstimatedCost() != null
                && recipe.getEstimatedCost().compareTo(BigDecimal.ZERO) > 0) {
            JLabel costLbl = new JLabel("Estimated Cost: $"
                + recipe.getEstimatedCost().setScale(2, RoundingMode.HALF_UP));
            costLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            costLbl.setForeground(ACCENT);
            costLbl.setAlignmentX(LEFT_ALIGNMENT);
            stats.add(costLbl);
            stats.add(Box.createVerticalStrut(6));
        }

        // Cook count
        cookCountLabel = new JLabel(formatCookCount(recipe.getCookCount()));
        cookCountLabel.setFont(FONT_SMALL);
        cookCountLabel.setForeground(TEXT_SECONDARY);
        cookCountLabel.setAlignmentX(LEFT_ALIGNMENT);
        stats.add(cookCountLabel);

        // Created date (shown if available)
        if (recipe.getCreatedAt() != null) {
            stats.add(Box.createVerticalStrut(3));
            JLabel dateLbl = new JLabel("Created: "
                + recipe.getCreatedAt().toLocalDate().toString());
            dateLbl.setFont(FONT_SMALL);
            dateLbl.setForeground(TEXT_SECONDARY);
            dateLbl.setAlignmentX(LEFT_ALIGNMENT);
            stats.add(dateLbl);
        }

        return stats;
    }

    /**
     * Red allergen warning banner.
     * Hidden by default; shown by checkAndShowAllergenBanner()
     * after ingredients are loaded.
     */
    private JPanel buildAllergenBanner() {
        JPanel banner = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ALLERGEN_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(ALLERGEN_BORDER);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        banner.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JLabel icon = new JLabel("⚠️");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));

        JLabel text = new JLabel(
            "<html><b style='color:#ff6464'>ALLERGY WARNING:</b>"
            + "<span style='color:#ffaaaa'> This recipe contains one or more known allergens."
            + " Allergen rows are marked with ⚠️ below.</span></html>"
        );
        text.setFont(FONT_BODY);

        banner.add(icon, BorderLayout.WEST);
        banner.add(text, BorderLayout.CENTER);
        return banner;
    }

    /**
     * Ingredients section card.
     * Header row: section title on the left, serving spinner on the right.
     * The ingredientsListPanel below is rebuilt each time the spinner changes.
     */
    private JPanel buildIngredientsSection() {
        JPanel card = buildCard();
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Header: title + serving scaler
        JPanel headerRow = new JPanel(new BorderLayout(10, 0));
        headerRow.setOpaque(false);
        headerRow.add(buildSectionTitle("🥗  Ingredients"), BorderLayout.WEST);

        JPanel scalerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        scalerPanel.setOpaque(false);

        JLabel scalerLbl = new JLabel("Servings:");
        scalerLbl.setFont(FONT_BODY);
        scalerLbl.setForeground(TEXT_SECONDARY);

        int base = Math.max(recipe.getServings(), 1);
        SpinnerNumberModel model = new SpinnerNumberModel(base, 1, 200, 1);
        servingsSpinner = new JSpinner(model);
        servingsSpinner.setPreferredSize(new Dimension(78, 32));
        servingsSpinner.setBackground(BG_INPUT);
        servingsSpinner.setForeground(TEXT_PRIMARY);
        servingsSpinner.setFont(FONT_BODY);
        JSpinner.NumberEditor spinnerEditor = new JSpinner.NumberEditor(servingsSpinner, "#");
        spinnerEditor.getTextField().setBackground(BG_INPUT);
        spinnerEditor.getTextField().setForeground(TEXT_PRIMARY);
        spinnerEditor.getTextField().setCaretColor(ACCENT);
        spinnerEditor.getTextField().setFont(FONT_BODY);
        servingsSpinner.setEditor(spinnerEditor);
        servingsSpinner.setBorder(new LineBorder(BORDER, 1, true));
        servingsSpinner.addChangeListener(e -> onServingsChanged());

        scalerPanel.add(scalerLbl);
        scalerPanel.add(servingsSpinner);
        headerRow.add(scalerPanel, BorderLayout.EAST);

        // Ingredient list (rebuilt when spinner changes)
        ingredientsListPanel = new JPanel();
        ingredientsListPanel.setLayout(new BoxLayout(ingredientsListPanel, BoxLayout.Y_AXIS));
        ingredientsListPanel.setOpaque(false);

        // Loading placeholder (replaced after loadIngredients() finishes)
        JLabel loading = new JLabel("Loading ingredients...");
        loading.setFont(FONT_BODY);
        loading.setForeground(TEXT_SECONDARY);
        loading.setAlignmentX(LEFT_ALIGNMENT);
        ingredientsListPanel.add(loading);

        card.add(headerRow,            BorderLayout.NORTH);
        card.add(ingredientsListPanel, BorderLayout.CENTER);
        return card;
    }

    /**
     * Cooking steps card.
     * Each non-empty line of recipe.getSteps() is rendered as a numbered step
     * with a filled circle on the left.
     */
    private JPanel buildStepsSection() {
        JPanel card = buildCard();
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        card.add(buildSectionTitle("📋  Cooking Steps"), BorderLayout.NORTH);

        JPanel stepsBody = new JPanel();
        stepsBody.setLayout(new BoxLayout(stepsBody, BoxLayout.Y_AXIS));
        stepsBody.setOpaque(false);

        String stepsText = recipe.getSteps();
        if (stepsText != null && !stepsText.trim().isEmpty()) {
            String[] lines = stepsText.split("\\n");
            int stepNum = 1;
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    stepsBody.add(buildStepRow(stepNum++, trimmed));
                    stepsBody.add(Box.createVerticalStrut(10));
                }
            }
        } else {
            JLabel noSteps = new JLabel("No cooking steps have been added yet.");
            noSteps.setFont(FONT_BODY);
            noSteps.setForeground(TEXT_SECONDARY);
            noSteps.setAlignmentX(LEFT_ALIGNMENT);
            stepsBody.add(noSteps);
        }

        card.add(stepsBody, BorderLayout.CENTER);
        return card;
    }

    /**
     * Centred "Start Cooking" panel at the bottom of the detail view.
     * Clicking increments the cook count in the DB via RecipeService
     * and updates the cookCountLabel in the hero section.
     */
    private JPanel buildCookingSection() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);

        JButton startBtn = buildStartCookingButton("🍳  Start Cooking");
        startBtn.addActionListener(e -> handleStartCooking(startBtn));
        panel.add(startBtn);
        return panel;
    }


    //  DATA  LOADING  (SwingWorker — runs on background thread)

    /**
     * Loads ingredients from the DB via RecipeService.getIngredientsForRecipe()
     * in a background thread, then rebuilds the ingredient list on the EDT.
     * Also triggers allergen banner check.
     */
    private void loadIngredients() {
        SwingWorker<List<RecipeIngredient>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<RecipeIngredient> doInBackground() {
                return recipeService.getIngredientsForRecipe(recipe.getId());
            }

            @Override
            protected void done() {
                try {
                    originalIngredients = get();
                    rebuildIngredientsList(originalIngredients);
                    checkAndShowAllergenBanner(originalIngredients);
                } catch (Exception ex) {
                    ingredientsListPanel.removeAll();
                    JLabel err = new JLabel("Could not load ingredients. Check the database.");
                    err.setFont(FONT_BODY);
                    err.setForeground(ALLERGEN_TEXT);
                    err.setAlignmentX(LEFT_ALIGNMENT);
                    ingredientsListPanel.add(err);
                    ingredientsListPanel.revalidate();
                    ingredientsListPanel.repaint();
                }
            }
        };
        worker.execute();
    }

    //  INGREDIENT  LIST  RENDERING

    /**
     * Clears and rebuilds the ingredientsListPanel with the provided list.
     * Called once on load and again every time the serving spinner changes.
     *
     * @param ingredients May be the original list or a scaled copy.
     */
    private void rebuildIngredientsList(List<RecipeIngredient> ingredients) {
        ingredientsListPanel.removeAll();

        if (ingredients == null || ingredients.isEmpty()) {
            JLabel noIng = new JLabel("No ingredients listed for this recipe.");
            noIng.setFont(FONT_BODY);
            noIng.setForeground(TEXT_SECONDARY);
            noIng.setAlignmentX(LEFT_ALIGNMENT);
            ingredientsListPanel.add(noIng);
        } else {
            // Column headers
            ingredientsListPanel.add(buildIngredientHeader());
            ingredientsListPanel.add(Box.createVerticalStrut(4));

            // Separator line
            JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
            sep.setForeground(BORDER);
            sep.setBackground(BORDER);
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            sep.setAlignmentX(LEFT_ALIGNMENT);
            ingredientsListPanel.add(sep);
            ingredientsListPanel.add(Box.createVerticalStrut(6));

            // One row per ingredient
            for (RecipeIngredient ri : ingredients) {
                ingredientsListPanel.add(buildIngredientRow(ri));
                ingredientsListPanel.add(Box.createVerticalStrut(5));
            }
        }

        ingredientsListPanel.revalidate();
        ingredientsListPanel.repaint();
    }

    /**
     * Called when the serving spinner value changes.
     * Uses ServingScaler to produce a scaled copy of the ingredient list,
     * then rebuilds the UI. The original list is preserved.
     */
    private void onServingsChanged() {
        int newServings  = (Integer) servingsSpinner.getValue();
        int baseServings = Math.max(recipe.getServings(), 1);
        if (originalIngredients.isEmpty()) return;

        List<RecipeIngredient> scaled =
            ServingScaler.scaleIngredients(originalIngredients, baseServings, newServings);
        rebuildIngredientsList(scaled);
    }

    /**
     * Checks if any loaded ingredient has allergenFlag = true.
     * If so, makes the allergenBanner visible and revalidates the layout.
     */
    private void checkAndShowAllergenBanner(List<RecipeIngredient> ingredients) {
        boolean hasAllergen = ingredients.stream()
            .anyMatch(ri -> ri.getIngredient() != null && ri.getIngredient().isAllergen());
        allergenBanner.setVisible(hasAllergen);
        if (hasAllergen) {
            contentBody.revalidate();
            contentBody.repaint();
        }
    }


    //  ACTION  HANDLERS

    /**
     * Increments the cook count via RecipeService in a background thread,
     * then updates cookCountLabel and shows a friendly dialog.
     * The Start Cooking button is disabled during the DB call.
     */
    private void handleStartCooking(JButton startBtn) {
        startBtn.setEnabled(false);
        startBtn.setText("Saving...");

        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                recipeService.incrementCookCount(recipe);
                return null;
            }

            @Override
            protected void done() {
                cookCountLabel.setText(formatCookCount(recipe.getCookCount()));
                startBtn.setText("✓  Cook Again");
                startBtn.setEnabled(true);
                JOptionPane.showMessageDialog(
                    RecipeDetailPanel.this,
                    "Enjoy cooking \"" + recipe.getTitle() + "\"! 🍳\n"
                    + "Cook count is now " + recipe.getCookCount() + ".",
                    "Cooking Started",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        };
        w.execute();
    }

    /**
     * Shows a confirmation dialog, then deletes the recipe via RecipeService.
     * On success, navigates back to the recipe list.
     * On failure, shows an error dialog.
     */
    private void handleDelete() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Delete \"" + recipe.getTitle() + "\"?\nThis will permanently remove the recipe and all its data.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) return;

        SwingWorker<Boolean, Void> w = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return recipeService.deleteRecipe(recipe.getId());
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        navigator.navigateToRecipeList();
                    } else {
                        JOptionPane.showMessageDialog(
                            RecipeDetailPanel.this,
                            "Could not delete the recipe. Please try again.",
                            "Delete Failed",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        RecipeDetailPanel.this,
                        "Error deleting recipe: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        w.execute();
    }

    /**
     * Duplicates the recipe via RecipeService in a background thread.
     * On success, opens the detail panel for the newly created copy.
     * On failure, shows an error dialog.
     */
    private void handleDuplicate() {
        SwingWorker<Recipe, Void> w = new SwingWorker<>() {
            @Override
            protected Recipe doInBackground() {
                return recipeService.duplicateRecipe(recipe.getId());
            }

            @Override
            protected void done() {
                try {
                    Recipe copy = get();
                    if (copy != null) {
                        JOptionPane.showMessageDialog(
                            RecipeDetailPanel.this,
                            "Recipe duplicated as \"" + copy.getTitle() + "\".",
                            "Duplicated",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        navigator.navigateToRecipeDetail(copy);
                    } else {
                        JOptionPane.showMessageDialog(
                            RecipeDetailPanel.this,
                            "Could not duplicate this recipe.",
                            "Duplicate Failed",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        RecipeDetailPanel.this,
                        "Error duplicating recipe: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        w.execute();
    }

    //  COMPONENT  BUILDER  HELPERS

    /**
     * Rounded card background panel.
     * Uses custom paintComponent for the BG_PANEL fill + BORDER outline.
     * Callers set the layout and add children after getting this panel.
     */
    private JPanel buildCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }

            @Override public boolean isOpaque() { return false; }
        };
        card.setAlignmentX(LEFT_ALIGNMENT);
        return card;
    }

    /** Bold section heading label (e.g. "🥗  Ingredients") */
    private JLabel buildSectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lbl.setForeground(TEXT_PRIMARY);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return lbl;
    }

    /**
     * Small stat box with a label on top and a value below.
     * Used for: Prep / Cook / Total / Serves.
     */
    private JPanel buildStatBox(String label, String value) {
        JPanel box = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(STAT_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }

            @Override public boolean isOpaque() { return false; }
        };
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        JLabel lblTop = new JLabel(label, SwingConstants.CENTER);
        lblTop.setFont(FONT_SMALL);
        lblTop.setForeground(TEXT_SECONDARY);
        lblTop.setAlignmentX(CENTER_ALIGNMENT);

        JLabel valLbl = new JLabel(value, SwingConstants.CENTER);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        valLbl.setForeground(ACCENT);
        valLbl.setAlignmentX(CENTER_ALIGNMENT);

        box.add(lblTop);
        box.add(Box.createVerticalStrut(4));
        box.add(valLbl);
        return box;
    }

    /**
     * Column header row for the ingredient table.
     * Shows: Ingredient | Quantity | Unit | Allergen
     */
    private JPanel buildIngredientHeader() {
        JPanel row = new JPanel(new GridLayout(1, 4, 8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.setAlignmentX(LEFT_ALIGNMENT);

        String[] cols = {"Ingredient", "Quantity", "Unit", "Status"};
        for (String col : cols) {
            JLabel lbl = new JLabel(col);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setForeground(TEXT_SECONDARY);
            row.add(lbl);
        }
        return row;
    }

    /**
     * One data row in the ingredient list.
     * Allergen rows are highlighted in red; safe rows use TEXT_PRIMARY.
     */
    private JPanel buildIngredientRow(RecipeIngredient ri) {
        boolean allergen = ri.getIngredient() != null && ri.getIngredient().isAllergen();

        JPanel row = new JPanel(new GridLayout(1, 4, 8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(allergen ? new Color(40, 14, 14) : BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                if (allergen) {
                    g2.setColor(new Color(90, 25, 25));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                }
                g2.dispose();
            }

            @Override public boolean isOpaque() { return false; }
        };
        row.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        row.setAlignmentX(LEFT_ALIGNMENT);

        String name     = ri.getIngredient() != null ? ri.getIngredient().getName() : "—";
        String qtyStr   = ri.getQuantity() != null
            ? ri.getQuantity().stripTrailingZeros().toPlainString() : "—";
        String unitStr  = (ri.getIngredient() != null
            && ri.getIngredient().getUnit() != null) ? ri.getIngredient().getUnit() : "—";

        // Ingredient name cell (with ⚠️ prefix if allergen)
        JLabel nameLbl = new JLabel(allergen ? "⚠️  " + name : name);
        nameLbl.setFont(FONT_BODY);
        nameLbl.setForeground(allergen ? ALLERGEN_TEXT : TEXT_PRIMARY);

        JLabel qtyLbl = new JLabel(qtyStr);
        qtyLbl.setFont(FONT_BODY);
        qtyLbl.setForeground(TEXT_PRIMARY);

        JLabel unitLbl = new JLabel(unitStr);
        unitLbl.setFont(FONT_BODY);
        unitLbl.setForeground(TEXT_SECONDARY);

        // Status badge
        JLabel statusLbl = new JLabel(allergen ? "⚠️  Allergen" : "✓  Safe");
        statusLbl.setFont(FONT_BADGE);
        statusLbl.setForeground(allergen ? ALLERGEN_TEXT : EASY_COLOR);

        row.add(nameLbl);
        row.add(qtyLbl);
        row.add(unitLbl);
        row.add(statusLbl);
        return row;
    }

    /**
     * One numbered cooking step row.
     * The step number is displayed inside a filled cyan circle on the left.
     */
    private JPanel buildStepRow(int number, String stepText) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);

        // Circular step-number badge
        JLabel numLbl = new JLabel(String.valueOf(number), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        numLbl.setPreferredSize(new Dimension(30, 30));
        numLbl.setMinimumSize(new Dimension(30, 30));
        numLbl.setMaximumSize(new Dimension(30, 30));
        numLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        numLbl.setForeground(Color.WHITE);
        numLbl.setOpaque(false);

        // Wrap in a panel to vertically centre it
        JPanel numWrapper = new JPanel(new GridBagLayout());
        numWrapper.setOpaque(false);
        numWrapper.add(numLbl);

        // Step text (HTML wraps long lines)
        JLabel textLbl = new JLabel(
            "<html><div style='width:520px'>" + escapeHtml(stepText) + "</div></html>"
        );
        textLbl.setFont(FONT_BODY);
        textLbl.setForeground(TEXT_PRIMARY);

        row.add(numWrapper, BorderLayout.WEST);
        row.add(textLbl,    BorderLayout.CENTER);
        return row;
    }

    /**
     * Photo placeholder shown when no photo path is set (or loading fails).
     * Shows a dark panel with a centred plate emoji.
     */
    private JPanel buildPhotoPlaceholder(int w, int h) {
        JPanel ph = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12, 18, 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        ph.setPreferredSize(new Dimension(w, h));
        ph.setOpaque(false);

        JLabel icon = new JLabel("🍽", JLabel.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        icon.setForeground(new Color(55, 80, 112));
        ph.add(icon);
        return ph;
    }

    /**
     * Red-tinted secondary-style delete button.
     * Uses the same rounded-corner painting as buildSecondaryButton()
     * but with DELETE_BG / DELETE_HOVER fill and DELETE_FG text.
     */
    private JButton buildDeleteButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? DELETE_HOVER : DELETE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BUTTON);
        btn.setForeground(DELETE_FG);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        btn.setOpaque(false);
        return btn;
    }

    /**
     * Large green "Start Cooking" action button.
     * Uses COOK_GREEN / COOK_DARK fill with white text.
     */
    private JButton buildStartCookingButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? COOK_DARK : COOK_GREEN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(14, 48, 14, 48));
        btn.setOpaque(false);
        return btn;
    }



    //  UTILITY

    /** Formats the cook count as a readable string. */
    private String formatCookCount(int count) {
        if (count == 0) return "Never cooked yet";
        return "Cooked " + count + " time" + (count == 1 ? "" : "s");
    }

    /** Escapes special HTML characters to prevent broken <html> labels. */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
