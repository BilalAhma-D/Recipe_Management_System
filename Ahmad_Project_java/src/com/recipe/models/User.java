package com.recipe.models;

import java.time.LocalDateTime;

/**
 * ============================================================
 *  FILE        : User.java
 *  PACKAGE     : com.recipe.models
 *  AUTHOR      : Ahmad
 *  MODULE      : User & Authentication
 * ============================================================
 *
 *  WHAT THIS CLASS IS:
 *  -------------------
 *  This is a "Model" class — it is a plain Java object that
 *  mirrors exactly one row in the USERS table of our PostgreSQL
 *  database. It holds data only; it does NOT talk to the DB.
 *
 *  Think of it like a container / envelope that travels
 *  between layers:
 *    DB row  →  UserDAO fills a User object  →  AuthService
 *    uses it  →  SessionManager holds it  →  all panels read it.
 *
 *  FIELDS MAP TO DB COLUMNS:
 *  --------------------------
 *   id            ←→  users.user_id   (INT, Primary Key)
 *   username      ←→  users.username  (VARCHAR 50, Unique)
 *   email         ←→  users.email     (VARCHAR 100, Unique)
 *   passwordHash  ←→  users.password_hash (VARCHAR 255, BCrypt)
 *   themePreference ←→ users.theme_preference (VARCHAR 10)
 *   createdAt     ←→  users.created_at (TIMESTAMP)
 *
 *  VIVA TIP:
 *  ---------
 *  "This class demonstrates ENCAPSULATION — all fields are
 *   private and can only be accessed through public getters
 *   and setters. No other class can directly touch the data."
 */
public class User {

    // -------------------------------------------------------
    //  PRIVATE FIELDS  (encapsulation — no direct access)
    // -------------------------------------------------------

    private int id;                   // Primary key from DB
    private String username;          // Unique login name
    private String email;             // User's email address
    private String passwordHash;      // BCrypt-hashed password (never plain text)
    private String themePreference;   // "light" or "dark"
    private LocalDateTime createdAt;  // When the account was made


    // -------------------------------------------------------
    //  CONSTRUCTORS
    // -------------------------------------------------------

    /**
     * Default (no-argument) constructor.
     * Used when building a User object step by step:
     *   User u = new User();
     *   u.setUsername("Ahmad");
     */
    public User() {
        // nothing to do — Java sets everything to null/0 by default
    }

    /**
     * Full constructor — used by UserDAO when reading a row from the DB.
     *
     * @param id             The numeric user ID from the database
     * @param username       The unique username
     * @param email          The user's email
     * @param passwordHash   The BCrypt-hashed password string
     * @param themePreference "light" or "dark"
     * @param createdAt      Timestamp from the DB
     */
    public User(int id, String username, String email,
                String passwordHash, String themePreference,
                LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.themePreference = themePreference;
        this.createdAt = createdAt;
    }


    // -------------------------------------------------------
    //  GETTERS  (read the private fields from outside)
    // -------------------------------------------------------

    /** Returns the user's numeric ID (Primary Key in the DB). */
    public int getId() {
        return id;
    }

    /** Returns the username (e.g. "ahmad123"). */
    public String getUsername() {
        return username;
    }

    /** Returns the email address. */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the BCrypt password hash.
     * Note: This is the hashed string stored in the DB —
     * it is NEVER the real password in plain text.
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** Returns the theme preference ("light" or "dark"). */
    public String getThemePreference() {
        return themePreference;
    }

    /** Returns when this user account was created. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    // -------------------------------------------------------
    //  SETTERS  (write to the private fields from outside)
    // -------------------------------------------------------

    /** Sets the user ID. Typically called by UserDAO after an INSERT. */
    public void setId(int id) {
        this.id = id;
    }

    /** Sets the username. */
    public void setUsername(String username) {
        this.username = username;
    }

    /** Sets the email. */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Sets the password hash.
     * IMPORTANT: Always pass a BCrypt hash here, never a plain-text password.
     * Plain-text hashing is done in AuthService before calling this setter.
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** Sets the theme preference ("light" or "dark"). */
    public void setThemePreference(String themePreference) {
        this.themePreference = themePreference;
    }

    /** Sets the creation timestamp. */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    // -------------------------------------------------------
    //  toString  — useful for debugging / printing in console
    // -------------------------------------------------------

    /**
     * Returns a readable summary of this User object.
     * Called automatically when you do: System.out.println(user);
     * Does NOT print the password hash for security.
     */
    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", username='" + username + '\''
                + ", email='" + email + '\''
                + ", theme='" + themePreference + '\''
                + ", createdAt=" + createdAt
                + '}';
    }
}
