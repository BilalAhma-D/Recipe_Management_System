package com.recipe;

import com.recipe.database.DatabaseConnection;
import com.recipe.gui.*;
import com.recipe.services.AuthService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * ============================================================
 *  FILE        : Main.java
 *  PACKAGE     : com.recipe
 *  AUTHORS     : Ahmad + Uzair  (shared entry point)
 * ============================================================
 *
 *  HOW THIS WORKS:
 *  ---------------
 *  Main creates the JFrame (the app window) and uses a
 *  CardLayout to hold every panel on a "stack of cards".
 *  Only ONE card is visible at a time.
 *
 *  Card names:
 *   "LOGIN"    → Ahmad's LoginPanel
 *   "REGISTER" → Ahmad's RegisterPanel
 *   "HOME"     → Main hub with a tab bar:
 *                  • Profile    (Ahmad)
 *                  • Meal Plan  (Uzair)
 *                  • Grocery    (Uzair)
 *                  • [Recipes]  (Bilal  — placeholder tab)
 *                  • [Search]   (Raja   — placeholder tab)
 *
 *  HOW TO ADD BILAL / RAJA LATER:
 *  --------------------------------
 *  Search for the comment "TODO:" in this file.
 *  Each placeholder tab has a one-line swap — just replace
 *  the JPanel stub with their real panel class.
 *
 *  VIVA TIP:
 *  ---------
 *  "Main uses CardLayout so switching screens is a single
 *   mainCards.show(mainPanel, 'LOGIN') call. No panel is
 *   ever re-created — they all exist in memory from startup.
 *   This keeps navigation instant and state preserved."
 */
public class Recipe_Manager {

    // ---- The outer card layout (Login / Register / Home) ----
    private static CardLayout  mainCards;
    private static JPanel      mainPanel;

    // ---- The inner tab panel (visible after login) ----
    private static JTabbedPane homeTabs;

    // ---- Shared service (Ahmad owns this) ----
    private static AuthService authService;


    public static void main(String[] args) {
        // All Swing work must happen on the Event Dispatch Thread
        SwingUtilities.invokeLater(Recipe_Manager::buildAndShowApp);
    }


    // -------------------------------------------------------
    //  buildAndShowApp()
    // -------------------------------------------------------

    private static void buildAndShowApp() {

        // ---- Apply FlatLaf look-and-feel (dark theme) ----
        // Make sure FlatLaf is in your pom.xml:
        //   <dependency>
        //     <groupId>com.formdev</groupId>
        //     <artifactId>flatlaf</artifactId>
        //     <version>3.2.5</version>
        //   </dependency>
        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } catch (Exception e) {
            System.err.println("[Main] FlatLaf not found — using system L&F. " + e.getMessage());
        }

        // ---- Shared auth service (used by Login, Register, Profile) ----
        authService = new AuthService();

        // ---- Main window ----
        JFrame frame = new JFrame("Recipe Manager");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(1100, 720);
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(null); // Centre on screen

        // Graceful shutdown: close DB pool when window closes
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DatabaseConnection.getInstance().shutdown();
                System.exit(0);
            }
        });

        // ---- CardLayout container ----
        mainCards = new CardLayout();
        mainPanel = new JPanel(mainCards);
        mainPanel.setBackground(new Color(15, 15, 30));

        // ---- Build and register Ahmad's screens ----
        LoginPanel loginPanel = new LoginPanel(
                authService,
                () -> mainCards.show(mainPanel, "HOME"),      // on login success
                () -> mainCards.show(mainPanel, "REGISTER")   // go to register
        );

        RegisterPanel registerPanel = new RegisterPanel(
                authService,
                () -> mainCards.show(mainPanel, "LOGIN")      // back to login
        );

        // ---- Build the HOME screen (tab bar) ----
        JPanel homePanel = buildHomePanel();

        // ---- Register cards ----
        mainPanel.add(loginPanel,    "LOGIN");
        mainPanel.add(registerPanel, "REGISTER");
        mainPanel.add(homePanel,     "HOME");

        // ---- Start on the login screen ----
        mainCards.show(mainPanel, "LOGIN");

        frame.setContentPane(mainPanel);
        frame.setVisible(true);
    }


    // -------------------------------------------------------
    //  buildHomePanel()  — the tabbed screen shown after login
    // -------------------------------------------------------

    /**
     * Builds the main hub that appears after a successful login.
     * Contains a JTabbedPane with one tab per team member's module.
     *
     * Tabs currently wired:
     *   ✅  Profile    — Ahmad's ProfilePanel
     *   ✅  Meal Plan  — Uzair's MealPlannerPanel
     *   ✅  Grocery    — Uzair's GroceryListPanel
     *   🔲  Recipes    — Bilal's placeholder (swap TODO below)
     *   🔲  Search     — Raja's placeholder  (swap TODO below)
     */
    private static JPanel buildHomePanel() {

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(15, 15, 30));

        homeTabs = new JTabbedPane(JTabbedPane.LEFT);
        homeTabs.setTabPlacement(JTabbedPane.LEFT);
        homeTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        homeTabs.setPreferredSize(new Dimension(220, 0));
        homeTabs.setBackground(new Color(20, 22, 42));
        homeTabs.setForeground(new Color(230, 235, 245));
        homeTabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // ---- TAB 1: Ahmad — Profile ----
        ProfilePanel profilePanel = new ProfilePanel(
                authService,
                () -> {
                    mainCards.show(mainPanel, "LOGIN"); // on logout → back to login
                }
        );
        homeTabs.addTab("Profile",   profilePanel);

        // ---- TAB 2: Uzair — Meal Planner ----
        MealPlannerPanel mealPlannerPanel = new MealPlannerPanel();
        homeTabs.addTab("Meal Plan", mealPlannerPanel);

        // ---- TAB 3: Uzair — Grocery List ----
        GroceryListPanel groceryListPanel = new GroceryListPanel();
        homeTabs.addTab("Grocery",   groceryListPanel);

        // ---- TAB 4: Bilal — Recipes (PLACEHOLDER) ----
        // TODO: Replace this stub with Bilal's RecipePanel when ready.
        //   Current line → homeTabs.addTab("Recipes", new JPanel());
        //   Replace with → homeTabs.addTab("Recipes", new RecipePanel());
        JPanel recipesPlaceholder = makePlaceholderTab(
                "Recipes",
                "Bilal's module",
                "RecipePanel will go here.\nReplace the stub in Main.java → TAB 4."
        );
        homeTabs.addTab("Recipes", recipesPlaceholder);

        // ---- TAB 5: Raja — Search (PLACEHOLDER) ----
        // TODO: Replace this stub with Raja's SearchPanel when ready.
        //   Current line → homeTabs.addTab("🔍  Search", new JPanel());
        //   Replace with → homeTabs.addTab("🔍  Search", new SearchPanel());
        JPanel searchPlaceholder = makePlaceholderTab(
                "Search",
                "Raja's module",
                "SearchPanel will go here.\nReplace the stub in Main.java → TAB 5."
        );
        homeTabs.addTab("Search", searchPlaceholder);

        // Refresh Uzair's panels when their tab becomes active
        // (so they reload the DB data after a week changes, etc.)
        homeTabs.addChangeListener(e -> {
            int selected = homeTabs.getSelectedIndex();
            if (selected == 0) profilePanel.refreshUserData();
            if (selected == 1) mealPlannerPanel.loadWeek();
            if (selected == 2) groceryListPanel.loadIngredients();
        });

        wrapper.add(homeTabs, BorderLayout.CENTER);
        return wrapper;
    }


    // -------------------------------------------------------
    //  makePlaceholderTab()  — used for Bilal / Raja stubs
    // -------------------------------------------------------

    /**
     * Creates a dark placeholder panel shown when a team member's
     * module is not yet integrated.
     *
     * @param icon        Emoji icon for the tab
     * @param owner       Team member name shown on screen
     * @param instruction One-line instruction for how to swap it in
     * @return A styled JPanel that clearly signals "work in progress"
     */
    private static JPanel makePlaceholderTab(String icon, String owner, String instruction) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(15, 15, 30));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(25, 28, 50));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 65, 100), 1, true),
                new javax.swing.border.EmptyBorder(40, 60, 40, 60)
        ));

        JLabel iconLabel = new JLabel(icon, SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(owner, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(230, 235, 245));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(new javax.swing.border.EmptyBorder(14, 0, 8, 0));

        JLabel statusLabel = new JLabel("Coming soon…", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusLabel.setForeground(new Color(79, 195, 247));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Split instruction onto two lines for display
        String[] lines = instruction.split("\n");
        for (String line : lines) {
            JLabel hint = new JLabel(line, SwingConstants.CENTER);
            hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            hint.setForeground(new Color(100, 105, 130));
            hint.setAlignmentX(Component.CENTER_ALIGNMENT);
            hint.setBorder(new javax.swing.border.EmptyBorder(4, 0, 0, 0));
            card.add(hint);
        }

        card.add(iconLabel);
        card.add(titleLabel);
        card.add(statusLabel);
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        for (String line : lines) {
            JLabel hint = new JLabel(line, SwingConstants.CENTER);
            hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            hint.setForeground(new Color(100, 105, 130));
            hint.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(hint);
        }

        panel.add(card, new GridBagConstraints());
        return panel;
    }
}
