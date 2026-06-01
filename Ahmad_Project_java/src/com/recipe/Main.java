package com.recipe;

import com.recipe.database.DatabaseConnection;
import com.recipe.database.RecipeDAO;
import com.recipe.gui.*;
import com.recipe.services.AuthService;
import com.recipe.services.RecipeService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// ============================================================
//  FILE   : Main.java
//  AUTHOR : Ahmad + Uzair + Bilal  (shared entry point)
//  PLACE  : src/main/java/com/recipe/Main.java
// ============================================================
//
//  HOW IT WORKS:
//  A JFrame holds one CardLayout panel. Cards = Login / Register / Home.
//  Home is a JTabbedPane with one tab per team member.
//
//  TABS:
//    Tab 1 — Profile      → Ahmad's ProfilePanel
//    Tab 2 — Meal Plan    → Uzair's MealPlannerPanel
//    Tab 3 — Grocery List → Uzair's GroceryListPanel
//    Tab 4 — Recipes      → Bilal's RecipeListPanel
//    Tab 5 — Search       → Raja's placeholder (swap when ready)
//
//  TO ADD RAJA'S PANEL:
//    Search for "TODO RAJA" in this file and follow the instruction.

public class Main {

    // Outer navigation (Login ↔ Register ↔ Home)
    private static CardLayout mainCards;
    private static JPanel     mainPanel;

    // Shared services (created once, reused everywhere)
    private static AuthService   authService;
    private static RecipeService recipeService;   // Bilal's service

    // Panels that need to refresh when their tab is selected
    private static MealPlannerPanel mealPlannerPanel;
    private static GroceryListPanel groceryListPanel;
    private static RecipeListPanel  recipeListPanel;
    private static ProfilePanel     profilePanel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::launch);
    }

    // ============================================================
    //  launch()
    // ============================================================
    private static void launch() {

        // Apply dark look-and-feel (FlatLaf)
        // pom.xml dependency needed:
        //   <groupId>com.formdev</groupId>
        //   <artifactId>flatlaf</artifactId>
        //   <version>3.2.5</version>
        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } catch (Exception e) {
            System.err.println("[Main] FlatLaf not found, using default L&F.");
        }

        // ---- Create shared services ----
        authService   = new AuthService();
        recipeService = new RecipeService(new RecipeDAO());   // Bilal's

        // ---- Main window ----
        JFrame frame = new JFrame("Recipe Manager");
        frame.setSize(1150, 740);
        frame.setMinimumSize(new Dimension(860, 600));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Shut down the DB connection pool cleanly when the window closes
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

        // ---- Ahmad: Login screen ----
        // Written by: Ahmad
        JPanel loginPanel = new LoginPanel(
            authService,
            () -> mainCards.show(mainPanel, "HOME"),      // on login success
            () -> mainCards.show(mainPanel, "REGISTER")   // go to register
        );

        // ---- Ahmad: Register screen ----
        // Written by: Ahmad
        JPanel registerPanel = new RegisterPanel(
            authService,
            () -> mainCards.show(mainPanel, "LOGIN")      // back to login
        );

        // ---- Home screen (tabbed) ----
        JPanel homePanel = buildHomePanel();

        mainPanel.add(loginPanel,    "LOGIN");
        mainPanel.add(registerPanel, "REGISTER");
        mainPanel.add(homePanel,     "HOME");

        mainCards.show(mainPanel, "LOGIN");
        frame.setContentPane(mainPanel);
        frame.setVisible(true);
    }

    // ============================================================
    //  buildHomePanel()
    //  Shown after login. One tab per team member.
    // ============================================================
    private static JPanel buildHomePanel() {

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(15, 15, 30));

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabs.setBackground(new Color(20, 22, 42));
        tabs.setForeground(new Color(230, 235, 245));

        // ---- TAB 1: Ahmad — Profile ----
        // Written by: Ahmad
        profilePanel = new ProfilePanel(
            authService,
            () -> mainCards.show(mainPanel, "LOGIN")   // on logout
        );
        tabs.addTab("\uD83D\uDC64  Profile",    profilePanel);

        // ---- TAB 2: Uzair — Meal Planner ----
        // Written by: Uzair
        mealPlannerPanel = new MealPlannerPanel();
        tabs.addTab("\uD83D\uDDD3  Meal Plan", mealPlannerPanel);

        // ---- TAB 3: Uzair — Grocery List ----
        // Written by: Uzair
        groceryListPanel = new GroceryListPanel();
        tabs.addTab("\uD83D\uDED2  Grocery",    groceryListPanel);

        // ---- TAB 4: Bilal — Recipes ----
        // Written by: Bilal
        // RecipeListPanel implements AppNavigator internally via the
        // navigator parameter. MainWindow (this class) passes a simple
        // AppNavigator implementation that swaps cards.
        recipeListPanel = new RecipeListPanel(recipeService, buildBilalNavigator(tabs));
        tabs.addTab("\uD83C\uDF7D  Recipes",    recipeListPanel);

        // ---- TAB 5: Raja — Search (placeholder) ----
        // TODO RAJA: Replace this stub with Raja's SearchPanel.
        //   1. Import com.recipe.gui.SearchPanel
        //   2. Replace the two lines below with:
        //        SearchPanel searchPanel = new SearchPanel(recipeService);
        //        tabs.addTab("🔍  Search", searchPanel);
        tabs.addTab("\uD83D\uDD0D  Search",    makePlaceholder("Raja's Search Module", "SearchPanel goes here"));

        // ---- Refresh panels when switching tabs ----
        tabs.addChangeListener(e -> {
            int i = tabs.getSelectedIndex();
            if (i == 0) profilePanel    .refreshUserData();
            if (i == 1) mealPlannerPanel.loadWeek();
            if (i == 2) groceryListPanel.loadIngredients();
            if (i == 3) recipeListPanel .loadRecipes();
        });

        wrapper.add(tabs, BorderLayout.CENTER);
        return wrapper;
    }

    // ============================================================
    //  buildBilalNavigator()
    //  Creates an AppNavigator that Bilal's panels use to switch
    //  between RecipeList / AddRecipe / RecipeDetail screens.
    //  These sub-panels are swapped inside a mini CardLayout that
    //  sits inside Tab 4.
    // ============================================================
    private static AppNavigator buildBilalNavigator(JTabbedPane tabs) {
        // Mini card panel that lives inside Tab 4
        CardLayout   bilalCards = new CardLayout();
        JPanel       bilalPanel = new JPanel(bilalCards);
        bilalPanel.setBackground(new Color(10, 14, 22));

        return new AppNavigator() {

            @Override
            public void navigateToRecipeList() {
                bilalCards.show(bilalPanel, "LIST");
                if (recipeListPanel != null) recipeListPanel.loadRecipes();
            }

            @Override
            public void navigateToAddRecipe() {
                AddRecipePanel addPanel = new AddRecipePanel(recipeService, this);
                bilalPanel.add(addPanel, "ADD");
                bilalCards.show(bilalPanel, "ADD");
            }

            @Override
            public void navigateToEditRecipe(com.recipe.models.Recipe recipe) {
                AddRecipePanel editPanel = new AddRecipePanel(recipeService, this, recipe);
                bilalPanel.add(editPanel, "EDIT");
                bilalCards.show(bilalPanel, "EDIT");
            }

            @Override
            public void navigateToRecipeDetail(com.recipe.models.Recipe recipe) {
                RecipeDetailPanel detailPanel = new RecipeDetailPanel(recipe, recipeService, this);
                bilalPanel.add(detailPanel, "DETAIL");
                bilalCards.show(bilalPanel, "DETAIL");
            }
        };
    }

    // ============================================================
    //  makePlaceholder()
    //  Shows a "Coming soon" card for modules not yet integrated.
    // ============================================================
    private static JPanel makePlaceholder(String owner, String hint) {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(new Color(15, 15, 30));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(25, 28, 50));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 65, 100), 1, true),
            new javax.swing.border.EmptyBorder(40, 60, 40, 60)
        ));

        JLabel icon = new JLabel("\uD83D\uDD0D", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name = new JLabel(owner, SwingConstants.CENTER);
        name.setFont(new Font("Segoe UI", Font.BOLD, 18));
        name.setForeground(new Color(230, 235, 245));
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        name.setBorder(new javax.swing.border.EmptyBorder(12, 0, 6, 0));

        JLabel sub = new JLabel("Coming soon…", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(new Color(79, 195, 247));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hintLbl = new JLabel(hint, SwingConstants.CENTER);
        hintLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintLbl.setForeground(new Color(100, 105, 130));
        hintLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        hintLbl.setBorder(new javax.swing.border.EmptyBorder(8, 0, 0, 0));

        card.add(icon);
        card.add(name);
        card.add(sub);
        card.add(hintLbl);

        outer.add(card);
        return outer;
    }
}