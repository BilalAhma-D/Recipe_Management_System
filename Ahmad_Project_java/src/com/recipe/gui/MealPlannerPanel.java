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
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ============================================================
//  FILE    : MealPlannerPanel.java
//  AUTHOR  : Uzair
//  PURPOSE : 7-day meal planner grid (Breakfast / Lunch / Dinner).
//            Each cell shows a recipe or "+ Add".
//            Clicking an empty cell lets you assign a recipe.
//            Clicking a filled cell lets you remove it.
//            Bottom card shows weekly nutrition totals.
// ============================================================
//
//  CONNECTS TO:
//    MealPlanService     → assign / remove / load meals
//    NutritionCalculator → weekly calorie + macro summary
//    SessionManager      → get the logged-in user (Ahmad's class)

public class MealPlannerPanel extends JPanel {

    // ---- Ahmad's color palette (kept consistent) ----
    private static final Color BG_DARK     = new Color(15, 15, 30);
    private static final Color CARD_BG     = new Color(25, 28, 50);
    private static final Color ACCENT_BLUE = new Color(79, 195, 247);
    private static final Color TEXT_WHITE  = new Color(230, 235, 245);
    private static final Color TEXT_MUTED  = new Color(140, 145, 165);
    private static final Color BORDER_DIM  = new Color(60, 65, 100);
    private static final Color SUCCESS_GRN = new Color(72, 199, 142);
    private static final Color ERROR_RED   = new Color(255, 90, 100);
    private static final Color CELL_FILLED = new Color(40, 70, 90);
    private static final Color CELL_EMPTY  = new Color(30, 33, 58);
    private static final Color HEADER_BG   = new Color(20, 22, 42);

    // ---- Services ----
    private final MealPlanService    mealPlanService;
    private final NutritionCalculator nutritionCalc;

    // ---- State ----
    private LocalDate currentWeekStart;

    // Key = "DayOfWeek|MealType"  e.g. "Monday|Lunch"
    // Value = the MealPlan entry for that slot (or absent if empty)
    private final Map<String, MealPlan> planMap = new HashMap<>();

    // ---- UI components that need updating after data loads ----
    private JLabel weekLabel;
    private JPanel gridPanel;
    private JLabel statusLabel;
    private JLabel calLabel, proteinLabel, carbsLabel, fatLabel;

    // ============================================================
    //  CONSTRUCTOR
    // ============================================================
    public MealPlannerPanel() {
        this.mealPlanService  = new MealPlanService();
        this.nutritionCalc    = new NutritionCalculator();
        this.currentWeekStart = mealPlanService.getCurrentWeekStart();
        buildLayout();
        loadWeek();   // load data from DB on first show
    }

    // ============================================================
    //  buildLayout()
    //  Sets up the full panel: top bar + grid + nutrition card.
    // ============================================================
    private void buildLayout() {
        setLayout(new BorderLayout());
        setBackground(BG_DARK);

        add(buildTopBar(), BorderLayout.NORTH);

        // Centre: status label + grid + nutrition card, all scrollable
        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.setBackground(BG_DARK);
        centre.setBorder(new EmptyBorder(14, 20, 20, 20));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(SUCCESS_GRN);
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        statusLabel.setBorder(new EmptyBorder(0, 0, 6, 0));
        centre.add(statusLabel);

        gridPanel = new JPanel();   // filled by loadWeek()
        gridPanel.setBackground(BG_DARK);
        gridPanel.setAlignmentX(LEFT_ALIGNMENT);
        centre.add(gridPanel);

        centre.add(Box.createRigidArea(new Dimension(0, 18)));
        centre.add(buildNutritionCard());

        JScrollPane scroll = new JScrollPane(centre);
        scroll.setBorder(null);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        add(scroll, BorderLayout.CENTER);
    }

    // ============================================================
    //  buildTopBar()
    //  Title + current week label + Prev/Next buttons
    // ============================================================
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CARD_BG);
        bar.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("\uD83D\uDDD3  Weekly Meal Planner");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_WHITE);

        weekLabel = new JLabel();
        weekLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        weekLabel.setForeground(ACCENT_BLUE);
        refreshWeekLabel();

        JButton prevBtn = makeNavButton("◀  Prev");
        JButton nextBtn = makeNavButton("Next  ▶");

        prevBtn.addActionListener(e -> {
            currentWeekStart = currentWeekStart.minusWeeks(1);
            refreshWeekLabel();
            loadWeek();
        });
        nextBtn.addActionListener(e -> {
            currentWeekStart = currentWeekStart.plusWeeks(1);
            refreshWeekLabel();
            loadWeek();
        });

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nav.setBackground(CARD_BG);
        nav.add(weekLabel);
        nav.add(prevBtn);
        nav.add(nextBtn);

        bar.add(title, BorderLayout.WEST);
        bar.add(nav,   BorderLayout.EAST);
        return bar;
    }

    // ============================================================
    //  buildNutritionCard()
    //  Weekly calorie + macro summary shown below the grid.
    //  Labels (calLabel etc.) are stored as fields so loadWeek()
    //  can update them after data loads.
    // ============================================================
    private JPanel buildNutritionCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_DIM, 1, true),
            new EmptyBorder(16, 22, 16, 22)
        ));

        JLabel title = new JLabel("\uD83D\uDCCA  Weekly Nutrition Summary");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(TEXT_WHITE);
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.setBorder(new EmptyBorder(0, 0, 12, 0));
        card.add(title);

        // Four stat labels side by side
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 36, 0));
        statsRow.setBackground(CARD_BG);
        statsRow.setAlignmentX(LEFT_ALIGNMENT);

        // Create labels and store references so updateNutritionSummary() can setText() them later
        calLabel     = makeStatLabel("Calories", "—");
        proteinLabel = makeStatLabel("Protein",  "—");
        carbsLabel   = makeStatLabel("Carbs",    "—");
        fatLabel     = makeStatLabel("Fat",      "—");

        statsRow.add(calLabel);
        statsRow.add(proteinLabel);
        statsRow.add(carbsLabel);
        statsRow.add(fatLabel);

        card.add(statsRow);
        return card;
    }

    // ============================================================
    //  loadWeek()   ← called by constructor AND by Prev/Next buttons
    //  Reads the current week plan from DB, rebuilds the grid,
    //  and refreshes the nutrition summary.
    // ============================================================
    public void loadWeek() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        // 1. Load from DB
        List<MealPlan> entries = mealPlanService.getWeekPlan(user, currentWeekStart);

        // 2. Build the fast-lookup map  key = "Monday|Lunch"
        planMap.clear();
        for (MealPlan mp : entries) {
            planMap.put(mp.getDayOfWeek() + "|" + mp.getMealType(), mp);
        }

        // 3. Rebuild the grid
        rebuildGrid();

        // 4. Update nutrition card
        updateNutritionSummary(user);
    }

    // ============================================================
    //  rebuildGrid()
    //  Clears gridPanel and redraws all 3 × 7 cells from planMap.
    // ============================================================
    private void rebuildGrid() {
        String[] days  = MealPlanService.DAYS;
        String[] meals = MealPlanService.MEALS;

        // GridLayout: 1 header row + 3 meal rows, 1 label col + 7 day cols
        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(meals.length + 1, days.length + 1, 4, 4));

        // Top-left corner (blank)
        gridPanel.add(makeHeaderCell(""));

        // Day headers
        for (String day : days) {
            gridPanel.add(makeHeaderCell(day));
        }

        // Meal rows
        for (String meal : meals) {
            gridPanel.add(makeRowLabel(meal));
            for (String day : days) {
                gridPanel.add(makeMealCell(day, meal));
            }
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    // ============================================================
    //  makeMealCell()
    //  One interactive button cell. Shows recipe name if filled,
    //  "+ Add" if empty.
    // ============================================================
    private JButton makeMealCell(String day, String meal) {
        String   key    = day + "|" + meal;
        MealPlan entry  = planMap.get(key);
        boolean  filled = (entry != null);

        String cellText = filled
            ? "<html><center>" + shorten(entry.getRecipeName(), 16) + "</center></html>"
            : "<html><center>+ Add</center></html>";

        JButton cell = new JButton(cellText) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = filled ? CELL_FILLED : CELL_EMPTY;
                g2.setColor(getModel().isRollover() ? base.brighter() : base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };

        cell.setFont(new Font("Segoe UI", filled ? Font.BOLD : Font.PLAIN, 11));
        cell.setForeground(filled ? ACCENT_BLUE : TEXT_MUTED);
        cell.setContentAreaFilled(false);
        cell.setBorderPainted(false);
        cell.setFocusPainted(false);
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cell.setPreferredSize(new Dimension(110, 70));

        if (filled) {
            // Clicking a filled cell → offer to remove it
            cell.addActionListener(e -> handleRemove(day, meal, entry));
        } else {
            // Clicking an empty cell → ask for a recipe ID
            cell.addActionListener(e -> handleAdd(day, meal));
        }

        return cell;
    }

    // ============================================================
    //  handleAdd()  — user clicked an empty cell
    // ============================================================
    private void handleAdd(String day, String meal) {
        String input = JOptionPane.showInputDialog(
            this,
            "Enter Recipe ID for " + day + " " + meal + ":",
            "Assign Recipe",
            JOptionPane.PLAIN_MESSAGE
        );

        if (input == null || input.trim().isEmpty()) return;

        int recipeId;
        try {
            recipeId = Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            setStatus("Please enter a valid numeric Recipe ID.", ERROR_RED);
            return;
        }

        User user = SessionManager.getInstance().getCurrentUser();
        String result = mealPlanService.assignRecipe(user, recipeId, day, meal, currentWeekStart);

        if ("SUCCESS".equals(result)) {
            setStatus("Recipe added to " + day + " " + meal + "!", SUCCESS_GRN);
            loadWeek();
        } else {
            setStatus(result, ERROR_RED);
        }
    }

    // ============================================================
    //  handleRemove()  — user clicked a filled cell
    // ============================================================
    private void handleRemove(String day, String meal, MealPlan entry) {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Remove \"" + entry.getRecipeName() + "\" from " + day + " " + meal + "?",
            "Remove Meal",
            JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) return;

        User user = SessionManager.getInstance().getCurrentUser();
        String result = mealPlanService.removeMeal(user, day, meal, currentWeekStart);

        if ("SUCCESS".equals(result)) {
            setStatus("Removed " + day + " " + meal + ".", TEXT_MUTED);
            loadWeek();
        } else {
            setStatus(result, ERROR_RED);
        }
    }

    // ============================================================
    //  updateNutritionSummary()
    //  Re-calculates totals and updates the four stat labels.
    // ============================================================
    private void updateNutritionSummary(User user) {
        Nutrition totals = nutritionCalc.calculateWeekTotals(user.getId(), currentWeekStart);

        calLabel    .setText(statHtml("Calories", String.format("%.0f kcal", totals.getCalories())));
        proteinLabel.setText(statHtml("Protein",  String.format("%.1f g",    totals.getProteinG())));
        carbsLabel  .setText(statHtml("Carbs",    String.format("%.1f g",    totals.getCarbsG())));
        fatLabel    .setText(statHtml("Fat",       String.format("%.1f g",    totals.getFatG())));
    }

    // ============================================================
    //  SMALL HELPER BUILDERS
    // ============================================================

    private JLabel makeHeaderCell(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(ACCENT_BLUE);
        lbl.setOpaque(true);
        lbl.setBackground(HEADER_BG);
        lbl.setBorder(new EmptyBorder(6, 4, 6, 4));
        return lbl;
    }

    private JLabel makeRowLabel(String meal) {
        JLabel lbl = new JLabel(meal, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(TEXT_WHITE);
        lbl.setOpaque(true);
        lbl.setBackground(HEADER_BG);
        lbl.setBorder(new EmptyBorder(4, 6, 4, 6));
        lbl.setPreferredSize(new Dimension(76, 70));
        return lbl;
    }

    private JButton makeNavButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(50, 160, 220) : new Color(40, 60, 90));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(TEXT_WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(90, 28));
        return btn;
    }

    // Creates a two-line stat label: muted label on top, bold value below.
    private JLabel makeStatLabel(String label, String value) {
        JLabel lbl = new JLabel(statHtml(label, value));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_WHITE);
        return lbl;
    }

    // HTML for a two-line stat: grey label + bold value
    private String statHtml(String label, String value) {
        return "<html><div style='text-align:center'>"
             + "<span style='color:#8c91a5;font-size:10px'>" + label + "</span><br>"
             + "<b>" + value + "</b></div></html>";
    }

    private void refreshWeekLabel() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        LocalDate end = currentWeekStart.plusDays(6);
        weekLabel.setText("Week: " + currentWeekStart.format(fmt)
                        + " – " + end.format(fmt));
    }

    private void setStatus(String msg, Color colour) {
        statusLabel.setText(msg);
        statusLabel.setForeground(colour);
    }

    // Truncates long recipe names to fit inside a grid cell
    private String shorten(String text, int max) {
        if (text == null)          return "—";
        if (text.length() <= max)  return text;
        return text.substring(0, max - 1) + "…";
    }
}
