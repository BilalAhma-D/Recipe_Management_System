package com.recipe.services;

import com.recipe.models.User;

/**
 * ============================================================
 *  FILE        : SessionManager.java
 *  PACKAGE     : com.recipe.services
 *  AUTHOR      : Ahmad
 *  MODULE      : User & Authentication (shared by whole team)
 * ============================================================
 *
 *  WHAT THIS CLASS DOES:
 *  ----------------------
 *  SessionManager is the "memory" of who is currently logged in.
 *
 *  After a successful login, AuthService puts the User object
 *  here. Then EVERY other module (Bilal's Recipe, Uzair's Meal
 *  Planner, Raja's Search) calls SessionManager.getCurrentUser()
 *  to find out who is logged in without passing the user around
 *  manually through every method and screen.
 *
 *  DESIGN PATTERN: SINGLETON
 *  --------------------------
 *  Same pattern as DatabaseConnection — only ONE SessionManager
 *  can ever exist. This guarantees that if AuthService sets a
 *  user, RecipePanel sees the exact same user object.
 *
 *  TYPICAL FLOW:
 *    1. User types credentials on LoginPanel
 *    2. LoginPanel calls AuthService.login(username, password)
 *    3. AuthService verifies password and calls SessionManager.setCurrentUser(user)
 *    4. Bilal's RecipeService calls SessionManager.getCurrentUser().getId()
 *       to tag a new recipe with the correct user ID
 *    5. When user clicks "Logout", we call SessionManager.clearSession()
 *
 *  VIVA TIP:
 *  ---------
 *  "SessionManager uses the Singleton pattern to hold one shared
 *   User object across all modules. It decouples modules from each
 *   other — no module needs to pass a User reference to another;
 *   they all read it from this central holder."
 */
public class SessionManager {

    // -------------------------------------------------------
    //  THE SINGLE INSTANCE  (Singleton — one for whole app)
    // -------------------------------------------------------

    /**
     * The one and only SessionManager object.
     * Private so only getInstance() can control it.
     */
    private static SessionManager instance = null;

    /**
     * The currently logged-in User.
     * null = no one is logged in.
     * Non-null = a user is logged in and their data is here.
     */
    private User currentUser = null;


    // -------------------------------------------------------
    //  PRIVATE CONSTRUCTOR  (Singleton — no "new" from outside)
    // -------------------------------------------------------

    /**
     * Private constructor — prevents anyone from writing:
     *   new SessionManager()  ← compile error
     *
     * This forces everyone to use getInstance().
     */
    private SessionManager() {
        // No setup needed — currentUser starts as null
    }


    // -------------------------------------------------------
    //  getInstance()  — Singleton access point
    // -------------------------------------------------------

    /**
     * Returns the single shared SessionManager.
     *
     * All classes call this to get the SessionManager.
     * "synchronized" makes it thread-safe (prevents double-creation).
     *
     * @return The one shared SessionManager instance
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }


    // -------------------------------------------------------
    //  setCurrentUser()  — called after login
    // -------------------------------------------------------

    /**
     * Stores the logged-in user in memory.
     *
     * Called by: AuthService.login() immediately after verifying the password.
     * After this call, all modules can read the user via getCurrentUser().
     *
     * @param user The User object returned by UserDAO after a successful login.
     *             Must NOT be null — use clearSession() to log out.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("[SessionManager] User logged in: " + user.getUsername());
    }


    // -------------------------------------------------------
    //  getCurrentUser()  — called by all modules
    // -------------------------------------------------------

    /**
     * Returns the currently logged-in User object.
     *
     * Called by: RecipeService, MealPlanService, SearchPanel, etc.
     * — basically every class that needs to know "who is using the app".
     *
     * HOW TO USE SAFELY:
     *   User user = SessionManager.getInstance().getCurrentUser();
     *   if (user != null) {
     *       int userId = user.getId();
     *       // ... use userId
     *   }
     *
     * Returns null if no one is logged in (app just started, or after logout).
     * Always do a null-check before calling .getId() or .getUsername() on the result.
     *
     * @return The currently logged-in User, or null if no session is active
     */
    public User getCurrentUser() {
        return currentUser;
    }


    // -------------------------------------------------------
    //  isLoggedIn()  — quick check
    // -------------------------------------------------------

    /**
     * A convenience method to check whether someone is logged in.
     *
     * More readable than writing: SessionManager.getInstance().getCurrentUser() != null
     *
     * Called by: MainWindow to decide whether to show LoginPanel or HomePanel.
     *
     * @return true if a user is currently logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }


    // -------------------------------------------------------
    //  clearSession()  — called on logout
    // -------------------------------------------------------

    /**
     * Removes the current user from memory (logs them out).
     *
     * Called by: AuthService.logout() when the user clicks the Logout button.
     * After this, isLoggedIn() returns false and getCurrentUser() returns null.
     *
     * MainWindow should switch back to the LoginPanel after this is called.
     */
    public void clearSession() {
        System.out.println("[SessionManager] User logged out: "
                + (currentUser != null ? currentUser.getUsername() : "nobody"));
        this.currentUser = null;
    }
}
