package com.recipe.services;

import com.recipe.database.UserDAO;
import com.recipe.models.User;
import org.mindrot.jbcrypt.BCrypt;

/**
 * ============================================================
 *  FILE        : AuthService.java
 *  PACKAGE     : com.recipe.services
 *  AUTHOR      : Ahmad
 *  MODULE      : User & Authentication
 * ============================================================
 *
 *  WHAT THIS CLASS DOES:
 *  ----------------------
 *  AuthService contains all the BUSINESS LOGIC for user accounts.
 *
 *  "Business logic" means the rules and decisions of the app:
 *    - "Is this password strong enough?" → validation rule
 *    - "Hash the password before saving" → security rule
 *    - "If username already exists, reject the registration" → business rule
 *
 *  AuthService sits between the GUI layer and the DAO layer:
 *    GUI (LoginPanel)  →  AuthService  →  UserDAO  →  PostgreSQL
 *
 *  It uses:
 *    - UserDAO    → to actually save/load from the database
 *    - BCrypt     → to hash passwords and verify them
 *    - SessionManager → to remember who is logged in
 *
 *  BCRYPT EXPLAINED:
 *  ------------------
 *  BCrypt is a password hashing function. When the user registers:
 *    plain password: "myPassword123"
 *    BCrypt hash:    "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
 *
 *  We store the hash in the DB, NEVER the real password.
 *  When the user logs in, BCrypt.checkpw(plainPassword, storedHash) compares them.
 *  Even if the DB is leaked, passwords stay safe because the hash cannot be reversed.
 *
 *  VIVA TIP:
 *  ---------
 *  "AuthService applies business logic before touching the database.
 *   It validates input, hashes passwords with BCrypt, and manages
 *   the session via SessionManager. The GUI (LoginPanel) never
 *   talks to UserDAO directly — that is AuthService's job."
 */
public class AuthService {

    // -------------------------------------------------------
    //  DEPENDENCY  — UserDAO (handles DB queries for us)
    // -------------------------------------------------------

    /**
     * UserDAO is used to INSERT, SELECT, and UPDATE user rows in the DB.
     * AuthService never writes SQL directly — it delegates to UserDAO.
     */
    private UserDAO userDAO;


    // -------------------------------------------------------
    //  CONSTRUCTOR
    // -------------------------------------------------------

    /**
     * Creates a new AuthService and initialises UserDAO.
     *
     * LoginPanel and RegisterPanel create one AuthService
     * and reuse it for all login/register actions.
     */
    public AuthService() {
        this.userDAO = new UserDAO();
    }


    // -------------------------------------------------------
    //  register()
    // -------------------------------------------------------

    /**
     * Registers a new user account.
     *
     * STEP-BY-STEP WHAT HAPPENS:
     *   1. Validate that all fields are filled in
     *   2. Check that username is not already taken
     *   3. Check that email is not already registered
     *   4. Hash the plain-text password using BCrypt
     *   5. Build a User object with the hashed password
     *   6. Call UserDAO to INSERT the row into the DB
     *   7. Return a result message to the GUI
     *
     * Called by: RegisterPanel's "Register" button click handler.
     *
     * @param username  The chosen username (from the username text field)
     * @param email     The user's email (from the email text field)
     * @param password  The plain-text password (from the password field)
     *                  NOTE: We hash it here before doing anything with it.
     * @param confirmPassword  The repeated password (from the confirm field)
     * @return A String message describing the result:
     *         "SUCCESS"              → registration worked, redirect to login
     *         "Passwords do not match" → show error on RegisterPanel
     *         "Username already taken" → show error on RegisterPanel
     *         "Please fill all fields" → show error on RegisterPanel
     */
    public String register(String username, String email,
                           String password, String confirmPassword) {

        // ---- STEP 1: Check that no field is empty ----
        // trim() removes leading/trailing spaces so "   " is treated as empty
        if (username == null || username.trim().isEmpty()
                || email == null || email.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            return "Please fill in all fields.";
        }

        // ---- STEP 2: Passwords must match ----
        if (!password.equals(confirmPassword)) {
            return "Passwords do not match.";
        }

        // ---- STEP 3: Password must be strong enough ----
        // We require at least 6 characters — simple but effective
        if (password.length() < 6) {
            return "Password must be at least 6 characters.";
        }

        // ---- STEP 4: Email must look like an email ----
        // Simple check — must contain @ and a dot after it
        if (!email.contains("@") || !email.contains(".")) {
            return "Please enter a valid email address.";
        }

        // ---- STEP 5: Username must not already be taken ----
        if (userDAO.doesUsernameExist(username.trim())) {
            return "Username is already taken. Please choose another.";
        }

        // ---- STEP 6: Hash the password with BCrypt ----
        // BCrypt.gensalt() creates a random "salt" (makes each hash unique)
        // BCrypt.hashpw() combines the salt and password into a secure hash
        // The result looks like: "$2a$10$randomSaltAndHashHere..."
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // ---- STEP 7: Build the User object ----
        // We do NOT set the ID — the database assigns it automatically (SERIAL / auto-increment)
        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setEmail(email.trim());
        newUser.setPasswordHash(hashedPassword);  // Store the hash, NOT the plain password

        // ---- STEP 8: Save to database via UserDAO ----
        boolean saved = userDAO.insertUser(newUser);

        if (saved) {
            return "SUCCESS";  // GUI reads this and navigates to LoginPanel
        } else {
            return "Registration failed. Please try again.";
        }
    }


    // -------------------------------------------------------
    //  login()
    // -------------------------------------------------------

    /**
     * Logs in an existing user.
     *
     * STEP-BY-STEP WHAT HAPPENS:
     *   1. Validate that username and password are not empty
     *   2. Look up the user in the DB by username (UserDAO)
     *   3. If not found → return null (wrong username)
     *   4. Use BCrypt.checkpw() to verify the password against the stored hash
     *   5. If password matches → store the user in SessionManager → return the User
     *   6. If password wrong → return null
     *
     * Called by: LoginPanel's "Login" button click handler.
     *
     * @param username  The username typed by the user
     * @param password  The plain-text password typed by the user
     * @return The logged-in User object if login succeeded,
     *         or null if username/password was wrong (GUI shows error label)
     */
    public User login(String username, String password) {

        // ---- STEP 1: Do not attempt login if fields are empty ----
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            return null;  // GUI will show "Please fill in all fields"
        }

        // ---- STEP 2: Load the user row from the database ----
        // UserDAO returns a User object if found, or null if username does not exist
        User user = userDAO.getUserByUsername(username.trim());

        // ---- STEP 3: If username was not found, login fails ----
        if (user == null) {
            return null;  // No user with this username — GUI shows "Invalid credentials"
        }

        // ---- STEP 4: Compare the typed password against the stored BCrypt hash ----
        // BCrypt.checkpw(plain, hash) returns true if they match, false otherwise
        // This works even though the hash looks completely different from the password
        boolean passwordMatches = BCrypt.checkpw(password, user.getPasswordHash());

        // ---- STEP 5: Wrong password → login fails ----
        if (!passwordMatches) {
            return null;  // Password was incorrect — GUI shows "Invalid credentials"
        }

        // ---- STEP 6: Login succeeded — store user in SessionManager ----
        // Now any module can call SessionManager.getInstance().getCurrentUser()
        // to get this user without us passing it manually everywhere
        SessionManager.getInstance().setCurrentUser(user);

        System.out.println("[AuthService] Login successful for: " + user.getUsername());
        return user;
    }


    // -------------------------------------------------------
    //  logout()
    // -------------------------------------------------------

    /**
     * Logs out the current user.
     *
     * Clears the user from SessionManager's memory.
     * After this, isLoggedIn() returns false.
     *
     * Called by: Any panel with a "Logout" button.
     * After calling this, MainWindow should switch back to LoginPanel.
     */
    public void logout() {
        SessionManager.getInstance().clearSession();
        System.out.println("[AuthService] User logged out.");
    }


    // -------------------------------------------------------
    //  changePassword()
    // -------------------------------------------------------

    /**
     * Changes the password of the currently logged-in user.
     *
     * STEP-BY-STEP WHAT HAPPENS:
     *   1. Get the current user from SessionManager
     *   2. Verify the old password using BCrypt
     *   3. Validate the new password
     *   4. Hash the new password with BCrypt
     *   5. Save the new hash to the DB via UserDAO
     *
     * Called by: ProfilePanel's "Change Password" button.
     *
     * @param oldPassword     The user's current password (to verify identity)
     * @param newPassword     The new password they want to set
     * @param confirmNew      Confirmation of the new password
     * @return A result message:
     *         "SUCCESS"                  → password changed
     *         "Old password is incorrect" → BCrypt check failed
     *         "New passwords do not match" → newPassword != confirmNew
     *         "Not logged in"             → SessionManager has no user
     */
    public String changePassword(String oldPassword,
                                  String newPassword,
                                  String confirmNew) {

        // ---- STEP 1: Must be logged in to change password ----
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            return "Not logged in.";
        }

        // ---- STEP 2: Verify old password is correct ----
        boolean oldPasswordOk = BCrypt.checkpw(oldPassword, currentUser.getPasswordHash());
        if (!oldPasswordOk) {
            return "Old password is incorrect.";
        }

        // ---- STEP 3: New passwords must match ----
        if (!newPassword.equals(confirmNew)) {
            return "New passwords do not match.";
        }

        // ---- STEP 4: New password must meet minimum length ----
        if (newPassword.length() < 6) {
            return "New password must be at least 6 characters.";
        }

        // ---- STEP 5: Hash the new password ----
        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());

        // ---- STEP 6: Update in the database ----
        boolean updated = userDAO.updatePassword(currentUser.getId(), newHash);

        if (updated) {
            // Also update the User object in SessionManager so it stays current
            currentUser.setPasswordHash(newHash);
            return "SUCCESS";
        } else {
            return "Failed to update password. Please try again.";
        }
    }


    // -------------------------------------------------------
    //  updateProfile()
    // -------------------------------------------------------

    /**
     * Updates the username and email of the currently logged-in user.
     *
     * Called by: ProfilePanel's "Save Profile" button.
     *
     * @param newUsername  The new username they want
     * @param newEmail     The new email they want
     * @return "SUCCESS" or an error message string
     */
    public String updateProfile(String newUsername, String newEmail) {

        // Must be logged in
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            return "Not logged in.";
        }

        // Validate inputs
        if (newUsername == null || newUsername.trim().isEmpty()
                || newEmail == null || newEmail.trim().isEmpty()) {
            return "Please fill in all fields.";
        }

        if (!newEmail.contains("@") || !newEmail.contains(".")) {
            return "Please enter a valid email address.";
        }

        // Save to database
        boolean updated = userDAO.updateProfile(
                currentUser.getId(),
                newUsername.trim(),
                newEmail.trim()
        );

        if (updated) {
            // Refresh the SessionManager's User object so the change shows everywhere
            currentUser.setUsername(newUsername.trim());
            currentUser.setEmail(newEmail.trim());
            return "SUCCESS";
        } else {
            return "Profile update failed. Please try again.";
        }
    }
}
