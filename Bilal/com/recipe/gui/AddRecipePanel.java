package com.recipe.gui;

import com.recipe.models.Ingredient;
import com.recipe.models.Recipe;
import com.recipe.models.RecipeIngredient;
import com.recipe.services.RecipeService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.recipe.gui.RecipeListPanel.*;


public class AddRecipePanel extends JPanel {

    private final RecipeService recipeService;
    private final AppNavigator  navigator;
    private       Recipe        editingRecipe;   // null = ADD mode, non-null = EDIT mode

    // Form fields
    private JTextField          titleField;
    private JTextArea           descriptionArea;
    private JSpinner            prepTimeSpinner;
    private JSpinner            cookTimeSpinner;
    private JSpinner            servingsSpinner;
    private JComboBox<String>   categoryCombo;
    private JLabel              photoPreview;
    private String              photoPath = null;
    private JTextArea           stepsArea;
    private JPanel              ingredientsContainer;
    private final List<IngredientRowPanel> ingredientRows = new ArrayList<>();

    // Buttons
    private JButton saveButton;
    private JButton cancelButton;
    private JButton addIngredientBtn;

    //  CONSTRUCTORS

    /** ADD mode — blank form */
    public AddRecipePanel(RecipeService recipeService, AppNavigator navigator) {
        this.recipeService  = recipeService;
        this.navigator      = navigator;
        this.editingRecipe  = null;
        setBackground(BG_DARK);
        setLayout(new BorderLayout());
        initComponents();
    }

    /** EDIT mode — pre-filled form */
    public AddRecipePanel(RecipeService recipeService, AppNavigator navigator, Recipe recipe) {
        this.recipeService  = recipeService;
        this.navigator      = navigator;
        this.editingRecipe  = recipe;
        setBackground(BG_DARK);
        setLayout(new BorderLayout());
        initComponents();
        populateForm(recipe);
    }
    //  UI SETUP


    private void initComponents() {

        //  HEADER  BAR
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(BG_DARK);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
            BorderFactory.createEmptyBorder(16, 22, 16, 22)
        ));

        JButton backBtn = buildSecondaryButton("← Back");
        backBtn.addActionListener(e -> navigator.navigateToRecipeList());

        String headerTitle = (editingRecipe == null) ? "Add New Recipe" : "Edit Recipe";
        JLabel titleLbl = new JLabel(headerTitle);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(TEXT_PRIMARY);

        saveButton = buildPrimaryButton(editingRecipe == null ? "Save Recipe" : "Update Recipe");
        saveButton.addActionListener(e -> handleSave());

        cancelButton = buildSecondaryButton("Cancel");
        cancelButton.addActionListener(e -> navigator.navigateToRecipeList());

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setBackground(BG_DARK);
        headerRight.add(cancelButton);
        headerRight.add(saveButton);

        header.add(backBtn,    BorderLayout.WEST);
        header.add(titleLbl,   BorderLayout.CENTER);
        header.add(headerRight,BorderLayout.EAST);

        //  SCROLLABLE  FORM  BODY

        JPanel formBody = new JPanel();
        formBody.setBackground(BG_DARK);
        formBody.setLayout(new BoxLayout(formBody, BoxLayout.Y_AXIS));
        formBody.setBorder(BorderFactory.createEmptyBorder(20, 28, 30, 28));

        // Section: Basic Info 
        formBody.add(buildSectionCard(buildBasicInfoSection()));
        formBody.add(Box.createVerticalStrut(16));

        // Section: Photo 
        formBody.add(buildSectionCard(buildPhotoSection()));
        formBody.add(Box.createVerticalStrut(16));

        // Section: Ingredients 
        formBody.add(buildSectionCard(buildIngredientsSection()));
        formBody.add(Box.createVerticalStrut(16));

        // Section: Steps 
        formBody.add(buildSectionCard(buildStepsSection()));
        formBody.add(Box.createVerticalStrut(20));

        // Bottom save bar 
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bottomBar.setBackground(BG_DARK);
        bottomBar.setAlignmentX(LEFT_ALIGNMENT);
        JButton cancelBtn2 = buildSecondaryButton("Cancel");
        cancelBtn2.addActionListener(e -> navigator.navigateToRecipeList());
        JButton saveBtn2 = buildPrimaryButton(editingRecipe == null ? "Save Recipe" : "Update Recipe");
        saveBtn2.addActionListener(e -> handleSave());
        bottomBar.add(cancelBtn2);
        bottomBar.add(saveBtn2);
        formBody.add(bottomBar);

        JScrollPane scroll = new JScrollPane(formBody);
        scroll.setBorder(null);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(header, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);
    }


    //  FORM  SECTION  BUILDERS


    /** "Title, Description, Time, Servings, Category" card */
    private JPanel buildBasicInfoSection() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_PANEL);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill   = GridBagConstraints.HORIZONTAL;

        // Section title
        c.gridx = 0; c.gridy = 0; c.gridwidth = 4; c.weightx = 1.0;
        p.add(buildSectionTitle("Recipe Details"), c);

        // Title field (full width)
        c.gridy = 1; c.gridwidth = 4;
        p.add(buildLabel("Recipe Title *"), c);

        c.gridy = 2; c.gridwidth = 4;
        titleField = buildTextField("e.g. Chicken Biryani", 500, 38);
        p.add(titleField, c);

        // Description (full width)
        c.gridy = 3; c.gridwidth = 4;
        p.add(buildLabel("Description"), c);

        c.gridy = 4; c.gridwidth = 4; c.ipady = 60;
        descriptionArea = buildTextArea("Write a short description...");
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(new LineBorder(BORDER, 1, true));
        descScroll.setBackground(BG_INPUT);
        descScroll.setPreferredSize(new Dimension(500, 88));
        p.add(descScroll, c);
        c.ipady = 0;

        // Row: Prep Time | Cook Time | Servings | Category ──
        c.gridy = 5; c.gridwidth = 1; c.weightx = 0.25;
        p.add(buildLabel("Prep Time (min)"), c);
        c.gridx = 1; p.add(buildLabel("Cook Time (min)"), c);
        c.gridx = 2; p.add(buildLabel("Servings"), c);
        c.gridx = 3; p.add(buildLabel("Category"), c);

        c.gridy = 6;
        SpinnerNumberModel prepModel = new SpinnerNumberModel(30, 0, 600, 5);
        prepTimeSpinner = buildSpinner(prepModel);
        c.gridx = 0; p.add(prepTimeSpinner, c);

        SpinnerNumberModel cookModel = new SpinnerNumberModel(45, 0, 600, 5);
        cookTimeSpinner = buildSpinner(cookModel);
        c.gridx = 1; p.add(cookTimeSpinner, c);

        SpinnerNumberModel servModel = new SpinnerNumberModel(4, 1, 100, 1);
        servingsSpinner = buildSpinner(servModel);
        c.gridx = 2; p.add(servingsSpinner, c);

        categoryCombo = buildCombo(
            new String[]{"Breakfast", "Lunch", "Dinner", "Dessert", "Snack", "Beverage", "Other"},
            160
        );
        c.gridx = 3; p.add(categoryCombo, c);

        return p;
    }

    /** Photo upload card */
    private JPanel buildPhotoSection() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_PANEL);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.weightx = 1.0;
        p.add(buildSectionTitle("Recipe Photo"), c);

        // Photo preview label
        photoPreview = new JLabel("No photo selected", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        photoPreview.setPreferredSize(new Dimension(200, 130));
        photoPreview.setFont(FONT_SMALL);
        photoPreview.setForeground(TEXT_SECONDARY);
        photoPreview.setOpaque(false);

        c.gridy = 1; c.gridwidth = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        p.add(photoPreview, c);

        JPanel photoButtons = new JPanel();
        photoButtons.setLayout(new BoxLayout(photoButtons, BoxLayout.Y_AXIS));
        photoButtons.setBackground(BG_PANEL);
        JButton uploadBtn = buildPrimaryButton("📷  Upload Photo");
        uploadBtn.setAlignmentX(LEFT_ALIGNMENT);
        uploadBtn.addActionListener(e -> handlePhotoUpload());
        JButton clearBtn = buildSecondaryButton("Clear Photo");
        clearBtn.setAlignmentX(LEFT_ALIGNMENT);
        clearBtn.addActionListener(e -> {
            photoPath = null;
            photoPreview.setIcon(null);
            photoPreview.setText("No photo selected");
        });
        photoButtons.add(uploadBtn);
        photoButtons.add(Box.createVerticalStrut(8));
        photoButtons.add(clearBtn);
        photoButtons.add(Box.createVerticalStrut(4));
        JLabel hint = new JLabel("Supported: JPG, PNG, GIF");
        hint.setFont(FONT_SMALL);
        hint.setForeground(TEXT_SECONDARY);
        photoButtons.add(hint);

        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
        c.insets = new Insets(6, 18, 6, 6);
        p.add(photoButtons, c);

        return p;
    }

    /** Ingredients section with dynamic rows */
    private JPanel buildIngredientsSection() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        // Header row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(BG_PANEL);
        headerRow.add(buildSectionTitle("Ingredients"), BorderLayout.WEST);
        addIngredientBtn = buildPrimaryButton("+ Add Ingredient");
        addIngredientBtn.addActionListener(e -> addIngredientRow());
        headerRow.add(addIngredientBtn, BorderLayout.EAST);

        // Column labels
        JPanel colLabels = new JPanel(new GridLayout(1, 5, 8, 0));
        colLabels.setBackground(BG_PANEL);
        colLabels.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        String[] cols = {"Ingredient Name", "Quantity", "Unit", "Price/Unit ($)", "Allergen"};
        for (String col : cols) {
            JLabel lbl = new JLabel(col);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setForeground(TEXT_SECONDARY);
            colLabels.add(lbl);
        }

        // Container for dynamic rows
        ingredientsContainer = new JPanel();
        ingredientsContainer.setLayout(new BoxLayout(ingredientsContainer, BoxLayout.Y_AXIS));
        ingredientsContainer.setBackground(BG_PANEL);

        addIngredientRow(); // Add one empty row by default

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(BG_PANEL);
        top.add(headerRow, BorderLayout.NORTH);
        top.add(colLabels, BorderLayout.SOUTH);

        p.add(top,                   BorderLayout.NORTH);
        p.add(ingredientsContainer,  BorderLayout.CENTER);
        return p;
    }

    /** Cooking steps section */
    private JPanel buildStepsSection() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        p.add(buildSectionTitle("Cooking Steps"), BorderLayout.NORTH);

        stepsArea = buildTextArea(
            "Enter each cooking step on a new line.\n" +
            "Step 1: Wash and chop all vegetables.\n" +
            "Step 2: Heat oil in a pan...\n"
        );
        JScrollPane stepsScroll = new JScrollPane(stepsArea);
        stepsScroll.setBorder(new LineBorder(BORDER, 1, true));
        stepsScroll.setBackground(BG_INPUT);
        stepsScroll.setPreferredSize(new Dimension(500, 160));
        p.add(stepsScroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("Tip: One step per line. RecipeDetailPanel will number them automatically.");
        hint.setFont(FONT_SMALL);
        hint.setForeground(TEXT_SECONDARY);
        p.add(hint, BorderLayout.SOUTH);
        return p;
    }

    //  ACTIONS

    /** Opens JFileChooser for photo selection, updates preview. */
    private void handlePhotoUpload() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Recipe Photo");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Image Files (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"
        ));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            photoPath = file.getAbsolutePath();
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    Image scaled = img.getScaledInstance(200, 130, Image.SCALE_SMOOTH);
                    photoPreview.setIcon(new ImageIcon(scaled));
                    photoPreview.setText("");
                }
            } catch (IOException ex) {
                photoPreview.setText(file.getName());
            }
        }
    }

    /** Adds a new empty IngredientRowPanel to the form */
    private void addIngredientRow() {
        IngredientRowPanel row = new IngredientRowPanel();
        ingredientRows.add(row);
        ingredientsContainer.add(row);
        ingredientsContainer.add(Box.createVerticalStrut(6));
        ingredientsContainer.revalidate();
        ingredientsContainer.repaint();
        // Scroll down to show the new row
        SwingUtilities.invokeLater(() -> {
            JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
            if (sp != null) {
                JScrollBar sb = sp.getVerticalScrollBar();
                sb.setValue(sb.getMaximum());
            }
        });
    }

    /** Removes a specific ingredient row from the form */
    private void removeIngredientRow(IngredientRowPanel row) {
        ingredientRows.remove(row);
        // Remove the row and the strut after it
        Component[] comps = ingredientsContainer.getComponents();
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] == row) {
                ingredientsContainer.remove(i);
                if (i < ingredientsContainer.getComponentCount()) {
                    ingredientsContainer.remove(i); // remove spacer
                }
                break;
            }
        }
        ingredientsContainer.revalidate();
        ingredientsContainer.repaint();
    }

    /**
     * Validates all form fields, builds Recipe + ingredient list,
     * then calls RecipeService.saveRecipe() or updateRecipe().
     * Called by both Save buttons.
     */
    private void handleSave() {
        // Validate title ──
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            showError("Please enter a recipe title.");
            titleField.requestFocus();
            return;
        }

        // Build Recipe object from form ──
        Recipe recipe = (editingRecipe != null) ? editingRecipe : new Recipe();
        recipe.setTitle(title);
        recipe.setDescription(descriptionArea.getText().trim());
        recipe.setPrepTime((Integer) prepTimeSpinner.getValue());
        recipe.setCookTime((Integer) cookTimeSpinner.getValue());
        recipe.setServings((Integer) servingsSpinner.getValue());
        recipe.setPhotoPath(photoPath);
        recipe.setSteps(stepsArea.getText().trim());

        // Map category name → id (0-based index from combo, +1 for 1-based id)
        recipe.setCategoryId(categoryCombo.getSelectedIndex() + 1);

        // Build ingredient list from rows ──
        List<RecipeIngredient> ingredientList = new ArrayList<>();
        for (IngredientRowPanel row : ingredientRows) {
            RecipeIngredient ri = row.buildRecipeIngredient();
            if (ri != null) {
                ingredientList.add(ri);
            }
        }

        // Disable buttons and show progress ──
        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        // Call service in background thread ──
        Recipe finalRecipe = recipe;
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                if (editingRecipe == null) {
                    return recipeService.saveRecipe(finalRecipe, ingredientList);
                } else {
                    return recipeService.updateRecipe(finalRecipe, ingredientList);
                }
            }
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(AddRecipePanel.this,
                            editingRecipe == null ? "Recipe saved successfully!" : "Recipe updated!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                        navigator.navigateToRecipeList();
                    } else {
                        showError("Could not save recipe. Please try again.");
                    }
                } catch (Exception ex) {
                    showError("Error saving recipe: " + ex.getMessage());
                } finally {
                    saveButton.setEnabled(true);
                    saveButton.setText(editingRecipe == null ? "Save Recipe" : "Update Recipe");
                }
            }
        };
        worker.execute();
    }

    /** Pre-fills all form fields from an existing Recipe object. Used in edit mode. */
    private void populateForm(Recipe recipe) {
        titleField.setText(recipe.getTitle());
        descriptionArea.setText(recipe.getDescription() != null ? recipe.getDescription() : "");
        prepTimeSpinner.setValue(recipe.getPrepTime());
        cookTimeSpinner.setValue(recipe.getCookTime());
        servingsSpinner.setValue(recipe.getServings());
        stepsArea.setText(recipe.getSteps() != null ? recipe.getSteps() : "");

        // Category (index = id - 1)
        if (recipe.getCategoryId() > 0 && recipe.getCategoryId() <= categoryCombo.getItemCount()) {
            categoryCombo.setSelectedIndex(recipe.getCategoryId() - 1);
        }

        // Photo
        if (recipe.getPhotoPath() != null && !recipe.getPhotoPath().isEmpty()) {
            photoPath = recipe.getPhotoPath();
            try {
                BufferedImage img = ImageIO.read(new File(photoPath));
                if (img != null) {
                    photoPreview.setIcon(new ImageIcon(img.getScaledInstance(200, 130, Image.SCALE_SMOOTH)));
                    photoPreview.setText("");
                }
            } catch (IOException ignored) { photoPreview.setText(new File(photoPath).getName()); }
        }

        // Load existing ingredients via service
        SwingWorker<List<RecipeIngredient>, Void> w = new SwingWorker<>() {
            @Override protected List<RecipeIngredient> doInBackground() {
                return recipeService.getIngredientsForRecipe(recipe.getId());
            }
            @Override protected void done() {
                try {
                    List<RecipeIngredient> rows = get();
                    // Clear default empty row
                    ingredientRows.clear();
                    ingredientsContainer.removeAll();
                    for (RecipeIngredient ri : rows) {
                        IngredientRowPanel row = new IngredientRowPanel(ri);
                        ingredientRows.add(row);
                        ingredientsContainer.add(row);
                        ingredientsContainer.add(Box.createVerticalStrut(6));
                    }
                    if (rows.isEmpty()) addIngredientRow();
                    ingredientsContainer.revalidate();
                    ingredientsContainer.repaint();
                } catch (Exception ignored) {}
            }
        };
        w.execute();
    }

    //  HELPER  BUILDERS

    /** Wraps a section content JPanel in a styled card with padding. */
    private JPanel buildSectionCard(JPanel content) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JLabel buildSectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lbl.setForeground(TEXT_PRIMARY);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return lbl;
    }

    private JLabel buildLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXT_SECONDARY);
        return lbl;
    }

    private JTextArea buildTextArea(String placeholder) {
        JTextArea ta = new JTextArea(placeholder);
        ta.setBackground(BG_INPUT);
        ta.setForeground(TEXT_SECONDARY);
        ta.setCaretColor(ACCENT);
        ta.setFont(FONT_BODY);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        ta.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (ta.getForeground().equals(TEXT_SECONDARY) && ta.getText().equals(placeholder)) {
                    ta.setText(""); ta.setForeground(TEXT_PRIMARY);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (ta.getText().trim().isEmpty()) {
                    ta.setText(placeholder); ta.setForeground(TEXT_SECONDARY);
                }
            }
        });
        return ta;
    }

    private JSpinner buildSpinner(SpinnerNumberModel model) {
        JSpinner sp = new JSpinner(model);
        sp.setBackground(BG_INPUT);
        sp.setForeground(TEXT_PRIMARY);
        sp.setFont(FONT_BODY);
        sp.setPreferredSize(new Dimension(110, 36));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(sp, "#");
        editor.getTextField().setBackground(BG_INPUT);
        editor.getTextField().setForeground(TEXT_PRIMARY);
        editor.getTextField().setCaretColor(ACCENT);
        editor.getTextField().setFont(FONT_BODY);
        editor.getTextField().setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        sp.setEditor(editor);
        sp.setBorder(new LineBorder(BORDER, 1, true));
        return sp;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }

    //  INNER CLASS : IngredientRowPanel
    //  One ingredient entry row (name, qty, unit, price, allergen, remove)

    /**
     * One row in the Ingredients section.
     * The user fills in ingredient name, quantity, unit, price per unit,
     * and checks the allergen box if the ingredient is a known allergen.
     *
     * buildRecipeIngredient() converts the row into a RecipeIngredient model object.
     */
    class IngredientRowPanel extends JPanel {

        private final JTextField nameField;
        private final JTextField quantityField;
        private final JTextField unitField;
        private final JTextField priceField;
        private final JCheckBox  allergenCheck;

        /** Empty row for new ingredient */
        IngredientRowPanel() {
            this(null);
        }

        /** Pre-filled row for editing an existing RecipeIngredient */
        IngredientRowPanel(RecipeIngredient ri) {
            setLayout(new GridLayout(1, 6, 8, 0));
            setBackground(BG_PANEL);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setAlignmentX(LEFT_ALIGNMENT);

            nameField     = buildSmallField(ri != null && ri.getIngredient() != null ? ri.getIngredient().getName() : "", "e.g. Onion");
            quantityField = buildSmallField(ri != null ? ri.getQuantity().toPlainString() : "", "e.g. 3");
            unitField     = buildSmallField(ri != null && ri.getIngredient() != null ? ri.getIngredient().getUnit() : "", "e.g. g");
            priceField    = buildSmallField(ri != null ? ri.getPricePerUnit().toPlainString() : "", "e.g. 0.15");

            allergenCheck = new JCheckBox("Allergen");
            allergenCheck.setBackground(BG_PANEL);
            allergenCheck.setForeground(TEXT_SECONDARY);
            allergenCheck.setFont(FONT_SMALL);
            allergenCheck.setFocusPainted(false);
            if (ri != null && ri.getIngredient() != null) {
                allergenCheck.setSelected(ri.getIngredient().isAllergen());
            }

            JButton removeBtn = new JButton("✕") {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(getModel().isRollover() ? new Color(200, 50, 50) : new Color(140, 40, 40));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            removeBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            removeBtn.setForeground(Color.WHITE);
            removeBtn.setContentAreaFilled(false);
            removeBtn.setBorderPainted(false);
            removeBtn.setFocusPainted(false);
            removeBtn.setPreferredSize(new Dimension(34, 34));
            removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            removeBtn.addActionListener(e -> removeIngredientRow(this));

            add(nameField);
            add(quantityField);
            add(unitField);
            add(priceField);
            add(allergenCheck);
            add(removeBtn);
        }

        private JTextField buildSmallField(String value, String placeholder) {
            JTextField f = new JTextField(value);
            f.putClientProperty("JTextField.placeholderText", placeholder);
            f.setBackground(BG_INPUT);
            f.setForeground(value.isEmpty() ? TEXT_SECONDARY : TEXT_PRIMARY);
            f.setCaretColor(ACCENT);
            f.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            f.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
            ));
            f.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) {
                    f.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(ACCENT, 1, true),
                        BorderFactory.createEmptyBorder(5, 8, 5, 8)));
                    if (f.getForeground().equals(TEXT_SECONDARY)) {
                        f.setText(""); f.setForeground(TEXT_PRIMARY);
                    }
                }
                @Override public void focusLost(FocusEvent e) {
                    f.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER, 1, true),
                        BorderFactory.createEmptyBorder(5, 8, 5, 8)));
                    if (f.getText().trim().isEmpty()) f.setForeground(TEXT_SECONDARY);
                }
            });
            return f;
        }

        /**
         * Reads all fields in this row and builds a RecipeIngredient model object.
         * Called by AddRecipePanel.handleSave() for each row.
         *
         * CONNECTS TO:
         *   RecipeService.findOrCreateIngredient() — looked up when saving,
         *   but here we build the model so RecipeService can do the lookup.
         *
         * @return RecipeIngredient with ingredient data, or null if row is blank.
         */
        public RecipeIngredient buildRecipeIngredient() {
            String name = nameField.getText().trim();
            if (name.isEmpty() || name.equals("e.g. Onion")) return null; // skip empty rows

            BigDecimal qty;
            BigDecimal price;
            try {
                qty   = new BigDecimal(quantityField.getText().trim().isEmpty() ? "0" : quantityField.getText().trim());
                price = new BigDecimal(priceField.getText().trim().isEmpty()    ? "0" : priceField.getText().trim());
            } catch (NumberFormatException e) {
                qty   = BigDecimal.ZERO;
                price = BigDecimal.ZERO;
            }

            String unit     = unitField.getText().trim();
            boolean allergen = allergenCheck.isSelected();

            // Build ingredient
            Ingredient ingredient = new Ingredient();
            ingredient.setName(name);
            ingredient.setUnit(unit.isEmpty() ? "unit" : unit);
            ingredient.setAllergenFlag(allergen);

            // Build RecipeIngredient (recipeId set later by RecipeService)
            RecipeIngredient ri = new RecipeIngredient();
            ri.setQuantity(qty);
            ri.setPricePerUnit(price);
            ri.setIngredient(ingredient);

            return ri;
        }

        /** Returns the ingredient name typed in this row. Used for duplicate detection. */
        public String getIngredientName() {
            return nameField.getText().trim();
        }
    }
}
