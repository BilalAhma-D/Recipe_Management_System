package com.recipe.database;

import com.recipe.models.User;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * ============================================================
 *  FILE        : UserDAO.java
 *  PACKAGE     : com.recipe.database
 *  AUTHOR      : Ahmad
 *  MODULE      : User & Authentication
 * ============================================================
 *
 *  WHAT IS A DAO?
 *  ---------------
 *  DAO = Data Access Object. It is a class whose only job is to
 *  talk to the database for one specific table.
 *
 *  UserDAO talks ONLY to the USERS table.
 *  It converts:
 *    Java  →  SQL  (when saving / updating data)
 *    SQL   →  Java (when reading / loading data)
 *
 *  AuthService and other services call UserDAO's methods.
 *  UserDAO calls DatabaseConnection to get a Connection.
 *  No GUI class should ever call UserDAO directly.
 *
 *  LAYER DIAGRAM:
 *    LoginPanel  →  AuthService  →  UserDAO  →  PostgreSQL
 *                                       ↑
 *                               DatabaseConnection
 *
 *  WHY PreparedStatement?
 *  -----------------------
 *  We always use PreparedStatement instead of plain Statement because:
 *    1. It prevents SQL Injection attacks (user can't break the query).
 *    2. It is faster (the DB compiles the query template once).
 *    3. It handles special characters like quotes automatically.
 *
 *  VIVA TIP:
 *  ---------
 *  "UserDAO is the Data Access Object for the users table. It uses
 *   PreparedStatements to prevent SQL injection. Every method gets a
 *   connection from Ahmad's DatabaseConnection pool and closes it in
 *   a finally block to prevent connection leaks."
 */
public class UserDAO {

    // -------------------------------------------------------
    //  insertUser()
    // -------------------------------------------------------

    /**
     * Saves a new user record into the USERS table in PostgreSQL.
     *
     * Called by: AuthService.register() — after validation and password hashing.
     *
     * @param user A User object with username, email, and passwordHash already set.
     *             The 'id' field is NOT set yet — the DB generates it automatically.
     * @return true  if the INSERT succeeded (the row was saved)
     *         false if something went wrong (duplicate username, DB offline, etc.)
     *
     * SQL EXECUTED:
     *   INSERT INTO users (username, email, password_hash, theme_preference)
     *   VALUES (?, ?, ?, ?)
     */
    public boolean insertUser(User user) {

        // The SQL query with ? placeholders (PreparedStatement fills these safely)
        String sql = "INSERT INTO users (username, email, password_hash, theme_preference) "
                   + "VALUES (?, ?, ?, ?)";

        // try-with-resources: automatically closes conn and ps when done
        // even if an exception happens — prevents connection leaks
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Fill in each ? placeholder in order (1-indexed)
            ps.setString(1, user.getUsername());     // 1st ? = username
            ps.setString(2, user.getEmail());        // 2nd ? = email
            ps.setString(3, user.getPasswordHash()); // 3rd ? = BCrypt hash
            ps.setString(4, "light");                // 4th ? = default theme

            // Execute the INSERT and check how many rows were affected
            int rowsAffected = ps.executeUpdate();

            // executeUpdate() returns the number of rows inserted (should be 1)
            return rowsAffected > 0;

        } catch (SQLException e) {
            // Print the error to console for debugging
            System.err.println("[UserDAO] insertUser() failed: " + e.getMessage());
            return false;
        }
    }


    // -------------------------------------------------------
    //  getUserByUsername()
    // -------------------------------------------------------

    /**
     * Looks up a user in the USERS table by their username.
     *
     * Called by: AuthService.login() — to find the user before checking the password.
     *
     * @param username The username to search for (e.g. "ahmad123")
     * @return A User object if found, or null if no user with that username exists.
     *
     * SQL EXECUTED:
     *   SELECT * FROM users WHERE username = ?
     */
    public User getUserByUsername(String username) {

        String sql = "SELECT user_id, username, email, password_hash, "
                   + "theme_preference, created_at "
                   + "FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Set the username we are searching for
            ps.setString(1, username);

            // executeQuery() returns a ResultSet — a table of rows matching the query
            ResultSet rs = ps.executeQuery();

            // rs.next() moves to the first (and only) matching row
            // If no row found, rs.next() returns false and we return null
            if (rs.next()) {
                // Build and return a User object from the row's columns
                return buildUserFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] getUserByUsername() failed: " + e.getMessage());
        }

        // No user found — return null (AuthService will handle this as "wrong username")
        return null;
    }


    // -------------------------------------------------------
    //  getUserById()
    // -------------------------------------------------------

    /**
     * Looks up a user by their numeric ID (Primary Key).
     *
     * Called by: SessionManager or other services that store just the user ID.
     *
     * @param id The integer user_id from the database
     * @return A User object if found, or null if the ID does not exist
     *
     * SQL EXECUTED:
     *   SELECT * FROM users WHERE user_id = ?
     */
    public User getUserById(int id) {

        String sql = "SELECT user_id, username, email, password_hash, "
                   + "theme_preference, created_at "
                   + "FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);  // Set the ID we are searching for

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return buildUserFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] getUserById() failed: " + e.getMessage());
        }

        return null;
    }


    // -------------------------------------------------------
    //  updateProfile()
    // -------------------------------------------------------

    /**
     * Updates the username and email of an existing user.
     *
     * Called by: ProfilePanel when the user saves changes to their profile.
     *
     * @param userId   The ID of the user to update (so we know WHICH row to change)
     * @param newUsername The new username they want
     * @param newEmail    The new email they want
     * @return true if the update worked, false if it failed
     *
     * SQL EXECUTED:
     *   UPDATE users SET username = ?, email = ? WHERE user_id = ?
     */
    public boolean updateProfile(int userId, String newUsername, String newEmail) {

        String sql = "UPDATE users SET username = ?, email = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newUsername);  // New username
            ps.setString(2, newEmail);     // New email
            ps.setInt(3, userId);          // Which user to update

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] updateProfile() failed: " + e.getMessage());
            return false;
        }
    }


    // -------------------------------------------------------
    //  updatePassword()
    // -------------------------------------------------------

    /**
     * Updates the password hash for a user (used when changing password).
     *
     * IMPORTANT: The newPasswordHash parameter must already be a BCrypt hash.
     * AuthService calls BCrypt.hashpw() first, then passes the result here.
     * This method never receives a plain-text password.
     *
     * Called by: AuthService.changePassword()
     *
     * @param userId          The ID of the user whose password to change
     * @param newPasswordHash The NEW BCrypt-hashed password (not plain text!)
     * @return true if update succeeded, false otherwise
     *
     * SQL EXECUTED:
     *   UPDATE users SET password_hash = ? WHERE user_id = ?
     */
    public boolean updatePassword(int userId, String newPasswordHash) {

        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPasswordHash);  // The new BCrypt hash
            ps.setInt(2, userId);              // Which user to update

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] updatePassword() failed: " + e.getMessage());
            return false;
        }
    }


    // -------------------------------------------------------
    //  doesUsernameExist()
    // -------------------------------------------------------

    /**
     * Checks if a username is already taken in the database.
     *
     * Called by: AuthService.register() to check for duplicates before inserting.
     *
     * @param username The username to check
     * @return true if the username already exists, false if it is available
     *
     * SQL EXECUTED:
     *   SELECT COUNT(*) FROM users WHERE username = ?
     */
    public boolean doesUsernameExist(String username) {

        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // COUNT(*) returns how many rows matched — 0 means username is free
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] doesUsernameExist() failed: " + e.getMessage());
        }

        return false;
    }


    // -------------------------------------------------------
    //  PRIVATE HELPER  — buildUserFromResultSet()
    // -------------------------------------------------------

    /**
     * Converts a single ResultSet row into a User object.
     *
     * This is a private helper used by getUserByUsername() and getUserById()
     * to avoid repeating the same column-reading code in every method.
     *
     * HOW IT WORKS:
     *   rs.getInt("user_id")     reads the "user_id" column as an integer
     *   rs.getString("username") reads the "username" column as a String
     *   rs.getTimestamp(...)     reads the timestamp and converts to LocalDateTime
     *
     * @param rs A ResultSet already positioned at the correct row (rs.next() was called)
     * @return A fully populated User object
     * @throws SQLException if a column name is wrong or the connection dropped
     */
    private User buildUserFromResultSet(ResultSet rs) throws SQLException {

        int id = rs.getInt("user_id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        String themePreference = rs.getString("theme_preference");

        // Timestamp from DB needs to be converted to Java's LocalDateTime
        // rs.getTimestamp() returns a java.sql.Timestamp; .toLocalDateTime() converts it
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;

        // Build and return the User object using the full constructor
        return new User(id, username, email, passwordHash, themePreference, createdAt);
    }
}
