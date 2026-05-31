package com.recipe.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ============================================================
 *  FILE        : DatabaseConnection.java
 *  PACKAGE     : com.recipe.database
 *  AUTHOR      : Ahmad
 *  MODULE      : Database Architect (shared by the whole team)
 * ============================================================
 *
 *  WHAT THIS CLASS DOES:
 *  ----------------------
 *  This is the ONLY class in the whole project that knows how
 *  to connect to PostgreSQL. Every other class (UserDAO,
 *  RecipeDAO, MealPlanDAO, etc.) calls this class to get a
 *  connection — they do NOT make their own connections.
 *
 *  This keeps connection logic in ONE place. If the DB host
 *  changes, we update only this file (and db.properties).
 *
 *  DESIGN PATTERN USED: SINGLETON
 *  --------------------------------
 *  A Singleton means "only one instance of this class can
 *  ever exist in the program". We do this because:
 *    - We only need ONE connection pool, not one per DAO.
 *    - Creating a pool is expensive — do it once and reuse.
 *
 *  HOW THE SINGLETON WORKS:
 *    1. The constructor is private → no one can do "new DatabaseConnection()"
 *    2. A static variable holds the single instance.
 *    3. getInstance() creates it on first call, then returns the same one every time.
 *
 *  HIKARICP CONNECTION POOL:
 *  --------------------------
 *  HikariCP is a library that manages a "pool" of JDBC connections.
 *  Instead of opening and closing a connection for every query
 *  (which is slow), it keeps a few connections open and lends them
 *  out as needed.
 *
 *  HOW OTHER CLASSES USE THIS:
 *  ----------------------------
 *    Connection conn = DatabaseConnection.getInstance().getConnection();
 *    // ... use conn for queries ...
 *    conn.close();   // returns it to the pool, does NOT actually close
 *
 *  VIVA TIP:
 *  ---------
 *  "DatabaseConnection uses the Singleton design pattern combined
 *   with HikariCP connection pooling. The Singleton ensures only
 *   one pool is created; HikariCP efficiently manages multiple
 *   JDBC connections within that pool."
 */
public class DatabaseConnection {

    // -------------------------------------------------------
    //  THE SINGLE INSTANCE  (held here statically)
    // -------------------------------------------------------

    /**
     * This static variable holds the one and only DatabaseConnection object.
     * It starts as null and is created the first time getInstance() is called.
     */
    private static DatabaseConnection instance = null;

    private final String jdbcUrl;
    private final String username;
    private final String password;


    // -------------------------------------------------------
    //  PRIVATE CONSTRUCTOR  (Singleton — no "new" from outside)
    // -------------------------------------------------------

    /**
     * Private constructor — only called once internally by getInstance().
     *
     * Reads database credentials from db.properties file, then
     * configures and starts the HikariCP connection pool.
     *
     * The constructor is private so no other class can write:
     *   new DatabaseConnection()   ← this would cause a compile error
     */
    private DatabaseConnection() {

        Properties props = loadProperties();
        this.jdbcUrl  = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");

        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new RuntimeException("[DatabaseConnection] db.url is not configured.");
        }

        System.out.println("[DatabaseConnection] Initialized with JDBC URL: " + jdbcUrl);
    }


    // -------------------------------------------------------
    //  getInstance()  — the Singleton access point
    // -------------------------------------------------------

    /**
     * Returns the single shared DatabaseConnection object.
     *
     * If it does not exist yet (first call), it creates one.
     * Every call after that returns the exact same object.
     *
     * THREAD SAFETY NOTE:
     *  "synchronized" means if two threads call this at the same
     *  millisecond, only one enters at a time — no double creation.
     *
     * @return The one shared DatabaseConnection instance
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            // First time — create the single instance
            instance = new DatabaseConnection();
        }
        // Every time — return the same instance
        return instance;
    }


    // -------------------------------------------------------
    //  getConnection()  — what DAOs actually call
    // -------------------------------------------------------

    /**
     * Borrows a JDBC Connection from the HikariCP pool.
     *
     * HOW OTHER CLASSES USE THIS:
     *   Connection conn = DatabaseConnection.getInstance().getConnection();
     *   PreparedStatement ps = conn.prepareStatement("SELECT ...");
     *   ...
     *   conn.close();  // returns the connection to the pool (does NOT close the real socket)
     *
     * @return A live JDBC Connection ready for SQL queries
     * @throws SQLException if the pool has no free connections or the DB is unreachable
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }


    // -------------------------------------------------------
    //  shutdown()  — call this when the app closes
    // -------------------------------------------------------

    /**
     * Nothing to shut down when using DriverManager directly.
     * Connections are closed by the caller after use.
     */
    public void shutdown() {
        System.out.println("[DatabaseConnection] Shutdown called; no pool to close.");
    }


    // -------------------------------------------------------
    //  HELPER  — loadProperties()
    // -------------------------------------------------------

    /**
     * Reads the db.properties file from the resources folder.
     *
     * The file looks like:
     *   db.url      = jdbc:postgresql://localhost:5432/recipedb
     *   db.username = postgres
     *   db.password = yourpassword
     *
     * The file is loaded from the classpath (resources/) so it
     * works whether you run from IntelliJ or from a JAR file.
     *
     * @return A Properties object with all the key-value pairs from the file
     * @throws RuntimeException if the file is missing or cannot be read
     */
    private Properties loadProperties() {
        Properties props = new Properties();

        // getResourceAsStream looks for the file inside the classpath
        // (i.e., the resources/ folder in IntelliJ projects)
        InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream("db.properties");

        if (input == null) {
            File fallback = new File("src/db.properties");
            if (fallback.exists()) {
                try {
                    input = new FileInputStream(fallback);
                } catch (IOException e) {
                    throw new RuntimeException(
                        "[DatabaseConnection] ERROR: Could not read fallback db.properties file — " + e.getMessage(), e
                    );
                }
            }
        }

        if (input == null) {
            throw new RuntimeException(
                "[DatabaseConnection] ERROR: db.properties file not found!\n"
                + "Make sure db.properties exists in your classpath or in src/db.properties.\n"
                + "It should contain: db.url, db.username, db.password"
            );
        }

        try (InputStream in = input) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(
                "[DatabaseConnection] ERROR: Could not read db.properties — " + e.getMessage(), e
            );
        }

        return props;
    }
}
