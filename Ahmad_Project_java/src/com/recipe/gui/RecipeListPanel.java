package com.recipe.gui;

import com.recipe.models.Recipe;
import com.recipe.services.RecipeService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * =========================================================
 *  CLASS   : RecipeListPanel
 *  PACKAGE : com.recipe.gui
 *  LAYER   : GUI
 *  AUTHOR  : Bilal
 *  EXTENDS : JPanel
 * =========================================================
 *
 *  PURPOSE:
 *  Main "Recipes" tab — a scrollable grid of recipe cards.
 *  Shown when user clicks "Recipes" in the left sidebar.
 *  Replaces the "Bilal's module — Coming soon..." placeholder.
 *
 *  CONNECTS TO:
 *   - RecipeService.getAllRecipes()           → loads all recipes
 *   - RecipeService.getMostCooked(5)          → sort option
 *   - AppNavigator.navigateToAddRecipe()      → Add Recipe button
 *   - AppNavigator.navigateToRecipeDetail()   → card click
 *
 *  HOW TO PLUG IN (Ahmad → MainWindow / TAB 4):
 *   RecipeService service = new RecipeService(new RecipeDAO());
 *   RecipeListPanel recipeListPanel = new RecipeListPanel(service, this);
 *   // Replace TAB 4 stub:
 *   tabs.setComponentAt(3, recipeListPanel);   // or addTab
 * =========================================================
 */
public class RecipeListPanel extends JPanel {

    // ─────────────────────────────────────────────────────────────
    //  THEME CONSTANTS  (matches the dark blue theme from screenshots)
    // ─────────────────────────────────────────────────────────────
    static final Color BG_DARK      = new Color(10, 14, 22);
    static final Color BG_PANEL     = new Color(19, 28, 45);
    static final Color BG_INPUT     = new Color(15, 22, 36);
    static final Color BG_HOVER     = new Color(26, 40, 62);
    static final Color BG_PHOTO     = new Color(14, 20, 34);
    static final Color ACCENT       = new Color(0, 188, 212);
    static final Color ACCENT_DARK  = new Color(0, 155, 178);
    static final Color TEXT_PRIMARY  = new Color(225, 232, 240);
    static final Color TEXT_SECONDARY= new Color(130, 148, 168);
    static final Color BORDER        = new Color(36, 52, 72);
    static final Color BORDER_CARD   = new Color(30, 45, 65);
    static final Color EASY_COLOR    = new Color(40, 167, 69);
    static final Color MEDIUM_COLOR  = new Color(255, 160, 0);
    static final Color HARD_COLOR    = new Color(220, 53, 69);

    static final Font FONT_PAGE_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    static final Font FONT_CARD_TITLE = new Font("Segoe UI", Font.BOLD, 13);
    static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 11);
    static final Font FONT_BADGE      = new Font("Segoe UI", Font.BOLD,  10);
    static final Font FONT_BUTTON     = new Font("Segoe UI", Font.BOLD,  13);

    // ─────────────────────────────────────────────────────────────
    //  FIELDS
    // ─────────────────────────────────────────────────────────────
    private final RecipeService recipeService;
    private final AppNavigator  navigator;

    private JTextField            searchField;
    private JComboBox<String>     difficultyFilter;
    private JComboBox<String>     sortCombo;
    private JPanel                cardsPanel;
    private JScrollPane           scrollPane;
    private JLabel                statusLabel;

    private List<Recipe> allRecipes = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────
    //  CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────

    /**
     * Create the recipe list panel.
     *
     * @param recipeService Bilal's service — provides all recipe operations
     * @param navigator     Ahmad's MainWindow (implements AppNavigator)
     */
    public RecipeListPanel(RecipeService recipeService, AppNavigator navigator) {
        this.recipeService = recipeService;
        this.navigator     = navigator;
        setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));
        initComponents();
        loadRecipes();
    }

    // ─────────────────────────────────────────────────────────────
    //  UI SETUP
    // ─────────────────────────────────────────────────────────────

    private void initComponents() {

        // ══════════════════════════════════════════════
        //  TOP  TOOLBAR
        // ══════════════════════════════════════════════
        JPanel topBar = new JPanel(new BorderLayout(0, 0));
        topBar.setBackground(BG_DARK);
        topBar.setBorder(BorderFactory.createEmptyBorder(22, 22, 0, 22));

        // Left: page title
        JLabel pageTitle = new JLabel("My Recipes");
        pageTitle.setFont(FONT_PAGE_TITLE);
        pageTitle.setForeground(TEXT_PRIMARY);

        // Right: search + filters + add button
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setBackground(BG_DARK);

        searchField = buildTextField("Search recipes...", 220, 36);
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { filterRecipes(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { filterRecipes(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterRecipes(); }
        });

        difficultyFilter = buildCombo(new String[]{"All Levels", "Easy", "Medium", "Hard"}, 130);
        difficultyFilter.addActionListener(e -> filterRecipes());

        sortCombo = buildCombo(new String[]{"Newest", "Most Cooked", "A – Z"}, 130);
        sortCombo.addActionListener(e -> filterRecipes());

        JButton addBtn = buildPrimaryButton("+ Add Recipe");
        addBtn.addActionListener(e -> navigator.navigateToAddRecipe());

        controls.add(searchField);
        controls.add(difficultyFilter);
        controls.add(sortCombo);
        controls.add(addBtn);

        topBar.add(pageTitle,  BorderLayout.WEST);
        topBar.add(controls,   BorderLayout.EAST);

        // ══════════════════════════════════════════════
        //  STATUS / COUNT BAR
        // ══════════════════════════════════════════════
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 22, 6));
        statusBar.setBackground(BG_DARK);
        statusLabel = new JLabel("Loading recipes...");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_SECONDARY);
        statusBar.add(statusLabel);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(BG_DARK);
        topSection.add(topBar,    BorderLayout.NORTH);
        topSection.add(statusBar, BorderLayout.SOUTH);

        // ══════════════════════════════════════════════
        //  CARDS  PANEL  (wrapping grid)
        // ══════════════════════════════════════════════
        cardsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 18, 18));
        cardsPanel.setBackground(BG_DARK);
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(8, 18, 18, 18));

        scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG_DARK);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setBackground(BG_DARK);

        add(topSection, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────
    //  DATA  METHODS
    // ─────────────────────────────────────────────────────────────

    /**
     * Loads all recipes from DB via RecipeService.
     * Runs in background thread (SwingWorker) so the UI never freezes.
     * Called on panel creation and after add/edit/delete.
     */
    public void loadRecipes() {
        statusLabel.setText("Loading recipes...");
        cardsPanel.removeAll();
        cardsPanel.add(buildLoadingPanel());
        cardsPanel.revalidate();
        cardsPanel.repaint();

        SwingWorker<List<Recipe>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Recipe> doInBackground() {
                return recipeService.getAllRecipes();
            }
            @Override
            protected void done() {
                try {
                    allRecipes = get();
                    filterRecipes();
                } catch (Exception ex) {
                    statusLabel.setText("Failed to load recipes.");
                    showErrorDialog("Could not load recipes: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /** Applies search query + difficulty filter + sort to allRecipes, then rebuilds cards. */
    private void filterRecipes() {
        String query = searchField.getText().trim().toLowerCase();
        String diff  = (String) difficultyFilter.getSelectedItem();
        String sort  = (String) sortCombo.getSelectedItem();

        List<Recipe> filtered = allRecipes.stream()
            .filter(r -> {
                boolean matchQ = query.isEmpty()
                    || r.getTitle().toLowerCase().contains(query)
                    || (r.getDescription() != null && r.getDescription().toLowerCase().contains(query));
                boolean matchD = "All Levels".equals(diff) || diff.equals(r.getDifficulty());
                return matchQ && matchD;
            })
            .collect(Collectors.toList());

        // Sort
        if ("Most Cooked".equals(sort)) {
            filtered.sort((a, b) -> Integer.compare(b.getCookCount(), a.getCookCount()));
        } else if ("A – Z".equals(sort)) {
            filtered.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
        }
        // "Newest" → default order from DB (id DESC)

        buildCards(filtered);
    }

    /** Clears cardsPanel and rebuilds recipe card components. */
    private void buildCards(List<Recipe> recipes) {
        cardsPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 18, 18));
        cardsPanel.removeAll();

        if (recipes.isEmpty()) {
            cardsPanel.setLayout(new GridBagLayout());
            cardsPanel.add(buildEmptyState(
                allRecipes.isEmpty() ? "No recipes yet!" : "No results found.",
                allRecipes.isEmpty() ? "Click '+ Add Recipe' to create your first recipe." : "Try a different search or filter."
            ));
            statusLabel.setText("0 recipes");
        } else {
            for (Recipe r : recipes) {
                cardsPanel.add(new RecipeCard(r));
            }
            statusLabel.setText(recipes.size() + " recipe" + (recipes.size() == 1 ? "" : "s"));
        }

        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPER  BUILDERS
    // ─────────────────────────────────────────────────────────────

    private JPanel buildLoadingPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_DARK);
        p.setPreferredSize(new Dimension(600, 300));
        JLabel lbl = new JLabel("Loading recipes...");
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_SECONDARY);
        p.add(lbl);
        return p;
    }

    private JPanel buildEmptyState(String headline, String sub) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_DARK);
        p.setBorder(BorderFactory.createEmptyBorder(60, 0, 0, 0));

        JLabel icon = new JLabel("🍽", JLabel.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        icon.setAlignmentX(CENTER_ALIGNMENT);

        JLabel h = new JLabel(headline);
        h.setFont(new Font("Segoe UI", Font.BOLD, 18));
        h.setForeground(TEXT_PRIMARY);
        h.setAlignmentX(CENTER_ALIGNMENT);

        JLabel s = new JLabel(sub);
        s.setFont(FONT_BODY);
        s.setForeground(TEXT_SECONDARY);
        s.setAlignmentX(CENTER_ALIGNMENT);

        p.add(icon);
        p.add(Box.createVerticalStrut(14));
        p.add(h);
        p.add(Box.createVerticalStrut(8));
        p.add(s);
        return p;
    }

    // ─────────────────────────────────────────────────────────────
    //  STYLED  COMPONENT  FACTORIES
    // ─────────────────────────────────────────────────────────────

    static JTextField buildTextField(String placeholder, int width, int height) {
        JTextField f = new JTextField();
        f.putClientProperty("JTextField.placeholderText", placeholder);
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(ACCENT);
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(7, 12, 7, 12)
        ));
        f.setPreferredSize(new Dimension(width, height));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(ACCENT, 1, true),
                    BorderFactory.createEmptyBorder(7, 12, 7, 12)));
            }
            @Override public void focusLost(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER, 1, true),
                    BorderFactory.createEmptyBorder(7, 12, 7, 12)));
            }
        });
        return f;
    }

    static JComboBox<String> buildCombo(String[] items, int width) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(FONT_BODY);
        cb.setPreferredSize(new Dimension(width, 36));
        cb.setBorder(new LineBorder(BORDER, 1, true));
        cb.setFocusable(false);
        // Renderer for dropdown items
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? ACCENT : BG_INPUT);
                setForeground(isSelected ? Color.WHITE : TEXT_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                setFont(FONT_BODY);
                return this;
            }
        });
        return cb;
    }

    static JButton buildPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ACCENT_DARK : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BUTTON);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(9, 22, 9, 22));
        btn.setOpaque(false);
        return btn;
    }

    static JButton buildSecondaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(30, 45, 68) : BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BUTTON);
        btn.setForeground(TEXT_PRIMARY);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        btn.setOpaque(false);
        return btn;
    }

    static JLabel buildDifficultyBadge(String difficulty) {
        String d   = (difficulty != null && !difficulty.isEmpty()) ? difficulty : "Easy";
        Color  bg  = d.equals("Easy") ? EASY_COLOR : d.equals("Hard") ? HARD_COLOR : MEDIUM_COLOR;
        JLabel lbl = new JLabel(d, JLabel.CENTER);
        lbl.setFont(FONT_BADGE);
        lbl.setForeground(Color.WHITE);
        lbl.setBackground(bg);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        return lbl;
    }

    private void showErrorDialog(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ═══════════════════════════════════════════════════════════════
    //  INNER CLASS : RecipeCard
    //  One recipe card shown in the grid
    // ═══════════════════════════════════════════════════════════════
    private class RecipeCard extends JPanel {

        private final Recipe  recipe;
        private boolean       hovered = false;

        RecipeCard(Recipe recipe) {
            this.recipe = recipe;
            setPreferredSize(new Dimension(205, 270));
            setLayout(new BorderLayout());
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            buildContent();
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) {
                    navigator.navigateToRecipeDetail(recipe);
                }
            });
        }

        private void buildContent() {
            // ── Photo area ──────────────────────────────────────
            JPanel photoPanel = new JPanel(new BorderLayout());
            photoPanel.setPreferredSize(new Dimension(205, 125));
            photoPanel.setOpaque(false);

            if (recipe.getPhotoPath() != null && !recipe.getPhotoPath().isEmpty()) {
                try {
                    BufferedImage img = ImageIO.read(new File(recipe.getPhotoPath()));
                    if (img != null) {
                        Image scaled = img.getScaledInstance(205, 125, Image.SCALE_SMOOTH);
                        JLabel imgLabel = new JLabel(new ImageIcon(scaled));
                        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        photoPanel.add(imgLabel, BorderLayout.CENTER);
                    } else {
                        photoPanel.add(buildPhotoPlaceholder(), BorderLayout.CENTER);
                    }
                } catch (IOException ex) {
                    photoPanel.add(buildPhotoPlaceholder(), BorderLayout.CENTER);
                }
            } else {
                photoPanel.add(buildPhotoPlaceholder(), BorderLayout.CENTER);
            }

            // ── Info area ────────────────────────────────────────
            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setOpaque(false);
            info.setBorder(BorderFactory.createEmptyBorder(11, 13, 13, 13));

            String title = recipe.getTitle().length() > 22
                ? recipe.getTitle().substring(0, 20) + "…"
                : recipe.getTitle();
            JLabel titleLbl = new JLabel(title);
            titleLbl.setFont(FONT_CARD_TITLE);
            titleLbl.setForeground(TEXT_PRIMARY);
            titleLbl.setAlignmentX(LEFT_ALIGNMENT);

            JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            badgeRow.setOpaque(false);
            badgeRow.setAlignmentX(LEFT_ALIGNMENT);
            badgeRow.add(buildDifficultyBadge(recipe.getDifficulty()));

            JLabel timeLbl = new JLabel("⏱  " + recipe.getTotalTime() + " min");
            timeLbl.setFont(FONT_SMALL);
            timeLbl.setForeground(TEXT_SECONDARY);
            timeLbl.setAlignmentX(LEFT_ALIGNMENT);

            String costStr = (recipe.getEstimatedCost() != null)
                ? "$" + recipe.getEstimatedCost().setScale(2, java.math.RoundingMode.HALF_UP)
                : "";
            JLabel costLbl = new JLabel(costStr);
            costLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            costLbl.setForeground(ACCENT);
            costLbl.setAlignmentX(LEFT_ALIGNMENT);

            info.add(titleLbl);
            info.add(Box.createVerticalStrut(6));
            info.add(badgeRow);
            info.add(Box.createVerticalStrut(5));
            info.add(timeLbl);
            if (!costStr.isEmpty()) {
                info.add(Box.createVerticalStrut(3));
                info.add(costLbl);
            }

            add(photoPanel, BorderLayout.NORTH);
            add(info,       BorderLayout.CENTER);
        }

        private JPanel buildPhotoPlaceholder() {
            JPanel ph = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(BG_PHOTO);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.dispose();
                }
            };
            ph.setOpaque(false);
            JLabel icon = new JLabel("🍽");
            icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
            icon.setForeground(new Color(55, 80, 112));
            ph.add(icon);
            return ph;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Background fill
            g2.setColor(hovered ? BG_HOVER : BG_PANEL);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
            // Border
            g2.setColor(hovered ? ACCENT : BORDER_CARD);
            g2.setStroke(new BasicStroke(hovered ? 1.6f : 1f));
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
            g2.dispose();
            super.paintComponent(g);
        }

        @Override public boolean isOpaque() { return false; }
    }

    // ═══════════════════════════════════════════════════════════════
    //  INNER CLASS : WrapLayout
    //  FlowLayout variant that correctly calculates preferred height
    //  inside a JScrollPane (standard FlowLayout breaks scroll height)
    // ═══════════════════════════════════════════════════════════════
    static class WrapLayout extends FlowLayout {

        WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension d = layoutSize(target, false);
            d.width -= (getHgap() + 1);
            return d;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                Insets insets    = target.getInsets();
                int    maxWidth  = targetWidth - insets.left - insets.right - getHgap() * 2;
                Dimension dim    = new Dimension(0, 0);
                int rowWidth     = 0;
                int rowHeight    = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth  = 0;
                        rowHeight = 0;
                    }
                    if (rowWidth != 0) rowWidth += getHgap();
                    rowWidth  += d.width;
                    rowHeight  = Math.max(rowHeight, d.height);
                }
                addRow(dim, rowWidth, rowHeight);
                dim.width  += insets.left + insets.right + getHgap() * 2;
                dim.height += insets.top + insets.bottom + getVgap() * 2;
                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);
            if (dim.height > 0) dim.height += getVgap();
            dim.height += rowHeight;
        }
    }
}
