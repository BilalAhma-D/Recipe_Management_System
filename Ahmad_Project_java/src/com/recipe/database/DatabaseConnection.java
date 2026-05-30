package com.recipe.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
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

    /**
     * HikariDataSource is the connection pool itself.
     * It holds multiple real JDBC connections internally and lends them out.
     */
    private HikariDataSource dataSource;


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

        // Step 1: Load settings from db.properties file
        // (db.properties is in the resources/ folder and contains
        //  the host, port, database name, username, and password)
        Properties props = loadProperties();

        // Step 2: Configure HikariCP using those settings
        HikariConfig config = new HikariConfig();

        // The JDBC URL tells Java which database server to connect to.
        // Format: jdbc:postgresql://HOST:PORT/DATABASE_NAME
        config.setJdbcUrl(props.getProperty("db.url"));

        // Username and password from db.properties
        config.setUsername(props.getProperty("db.username"));
        config.setPassword(props.getProperty("db.password"));

        // Maximum number of real DB connections kept open in the pool.
        // 10 is enough for a desktop app with a few simultaneous DAOs.
        config.setMaximumPoolSize(10);

        // How long (ms) to wait for a free connection before throwing an error.
        // 30 seconds is generous for a student project.
        config.setConnectionTimeout(30000);

        // A name for the pool — shows in logs so we know which pool it is.
        config.setPoolName("RecipeAppPool");

        // Step 3: Create the actual pool using our config
        this.dataSource = new HikariDataSource(config);

        System.out.println("[DatabaseConnection] Connection pool started successfully.");
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
        return dataSource.getConnection();
    }


    // -------------------------------------------------------
    //  shutdown()  — call this when the app closes
    // -------------------------------------------------------

    /**
     * Closes the entire connection pool.
     *
     * Call this once when the application window closes (in MainWindow's
     * windowClosing listener). This releases all open sockets to PostgreSQL.
     *
     * After this is called, getConnection() will fail — so only call
     * this at the very end of the program.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[DatabaseConnection] Connection pool shut down.");
        }
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
        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (input == null) {
                // File not found — give a helpful error message
                throw new RuntimeException(
                    "[DatabaseConnection] ERROR: db.properties file not found!\n"
                    + "Make sure db.properties exists in your src/main/resources/ folder.\n"
                    + "It should contain: db.url, db.username, db.password"
                );
            }

            // Load all key=value lines from the file into props
            props.load(input);

        } catch (IOException e) {
            throw new RuntimeException(
                "[DatabaseConnection] ERROR: Could not read db.properties — " + e.getMessage(), e
            );
        }

        return props;
    }
}
