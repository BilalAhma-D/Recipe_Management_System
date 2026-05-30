package com.recipe.gui;

import com.recipe.models.User;
import com.recipe.services.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * ============================================================
 *  FILE        : LoginPanel.java
 *  PACKAGE     : com.recipe.gui
 *  AUTHOR      : Ahmad
 *  MODULE      : User & Authentication — GUI Layer
 * ============================================================
 *
 *  WHAT THIS CLASS DOES:
 *  ----------------------
 *  LoginPanel is the first screen the user sees when they open
 *  the application. It shows:
 *    - An app logo / title
 *    - A username input field
 *    - A password input field (characters are hidden with dots)
 *    - A Login button
 *    - An error label (shown only when login fails)
 *    - A link to open the RegisterPanel
 *
 *  COMMUNICATION FLOW:
 *    1. User fills in username + password and clicks "Login"
 *    2. LoginPanel calls AuthService.login(username, password)
 *    3. If successful → call onLoginSuccess callback → MainWindow shows HomePanel
 *    4. If failed    → show error message on screen
 *
 *  DESIGN CHOICES:
 *  ----------------
 *  - Dark charcoal background (#1a1a2e) with electric blue accent (#4fc3f7)
 *  - Rounded card panel in the centre (like a modern login widget)
 *  - FlatLaf look-and-feel gives flat, modern buttons automatically
 *  - Error message appears in red under the password field
 *
 *  HOW MAINWINDOW USES THIS:
 *  --------------------------
 *    LoginPanel login = new LoginPanel(authService, () -> {
 *        // This Runnable runs when login succeeds
 *        cardLayout.show(mainPanel, "HOME");
 *    }, () -> {
 *        // This Runnable runs when "Register" link is clicked
 *        cardLayout.show(mainPanel, "REGISTER");
 *    });
 *
 *  VIVA TIP:
 *  ---------
 *  "LoginPanel is purely a presentation class — it takes user input
 *   and delegates all logic to AuthService. The panel uses callbacks
 *   (Runnable) to tell MainWindow what to do next, which keeps GUI
 *   layers decoupled from each other."
 */
public class LoginPanel extends JPanel {

    // -------------------------------------------------------
    //  COLOUR CONSTANTS  (define the theme in one place)
    // -------------------------------------------------------

    private static final Color BG_DARK      = new Color(15, 15, 30);      // Deep navy background
    private static final Color CARD_BG      = new Color(25, 28, 50);      // Slightly lighter card
    private static final Color ACCENT_BLUE  = new Color(79, 195, 247);    // Electric blue accent
    private static final Color TEXT_WHITE   = new Color(230, 235, 245);   // Off-white text
    private static final Color TEXT_MUTED   = new Color(140, 145, 165);   // Muted grey labels
    private static final Color FIELD_BG     = new Color(35, 38, 65);      // Dark input field BG
    private static final Color BORDER_DIM   = new Color(60, 65, 100);     // Field border colour
    private static final Color ERROR_RED    = new Color(255, 90, 100);    // Error message colour
    private static final Color BTN_HOVER    = new Color(50, 160, 220);    // Button hover colour

    // -------------------------------------------------------
    //  GUI COMPONENT FIELDS
    // -------------------------------------------------------

    private JTextField usernameField;     // Where user types their username
    private JPasswordField passwordField; // Where user types password (shown as dots)
    private JLabel errorLabel;            // Shows error messages in red
    private JButton loginButton;          // The main "Login" button

    // -------------------------------------------------------
    //  SERVICE & CALLBACK FIELDS
    // -------------------------------------------------------

    private AuthService authService;      // Business logic — does the actual login
    private Runnable onLoginSuccess;      // Called when login works → MainWindow switches panel
    private Runnable onGoToRegister;      // Called when "Register" link is clicked


    // -------------------------------------------------------
    //  CONSTRUCTOR
    // -------------------------------------------------------

    /**
     * Creates the Login screen.
     *
     * @param authService     The AuthService that handles login logic.
     *                        Passed in from MainWindow (dependency injection).
     * @param onLoginSuccess  A Runnable that MainWindow gives us.
     *                        We run this after a successful login to switch screens.
     * @param onGoToRegister  A Runnable that MainWindow gives us.
     *                        We run this when the user clicks "Register" link.
     */
    public LoginPanel(AuthService authService, Runnable onLoginSuccess, Runnable onGoToRegister) {
        this.authService = authService;
        this.onLoginSuccess = onLoginSuccess;
        this.onGoToRegister = onGoToRegister;

        // Build the visual layout
        buildLayout();
    }


    // -------------------------------------------------------
    //  buildLayout()  — sets up all components on screen
    // -------------------------------------------------------

    /**
     * Builds and arranges all visual components on the panel.
     *
     * Layout structure:
     *   LoginPanel (BorderLayout, dark background)
     *     └── centrePanel (GridBagLayout — centres the card)
     *           └── card (custom RoundedPanel — the white-ish box)
     *                 ├── Logo + Title
     *                 ├── Username field
     *                 ├── Password field
     *                 ├── Error label (hidden until needed)
     *                 ├── Login button
     *                 └── Register link
     */
    private void buildLayout() {

        // ---- Outer panel: dark background, fills the whole window ----
        setLayout(new GridBagLayout()); // GridBagLayout centres its single child
        setBackground(BG_DARK);

        // ---- Card panel: the login box in the middle ----
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS)); // stack components vertically
        card.setBackground(CARD_BG);
        card.setBorder(new EmptyBorder(45, 50, 45, 50)); // inner padding
        card.setPreferredSize(new Dimension(420, 520));

        // ---- App icon / emoji label ----
        JLabel iconLabel = new JLabel("🍴", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLabel.setAlignmentX(CENTER_ALIGNMENT);
        iconLabel.setBorder(new EmptyBorder(0, 0, 8, 0));

        // ---- App title ----
        JLabel titleLabel = makeLabel("Recipe Manager", 24, Font.BOLD, TEXT_WHITE);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        // ---- Subtitle ----
        JLabel subLabel = makeLabel("Sign in to your account", 13, Font.PLAIN, TEXT_MUTED);
        subLabel.setAlignmentX(CENTER_ALIGNMENT);
        subLabel.setBorder(new EmptyBorder(4, 0, 28, 0));

        // ---- Username section ----
        JLabel userLabel = makeLabel("Username", 12, Font.BOLD, TEXT_MUTED);
        userLabel.setAlignmentX(LEFT_ALIGNMENT);

        usernameField = makeTextField("Enter your username");
        usernameField.setAlignmentX(LEFT_ALIGNMENT);

        // ---- Password section ----
        JLabel passLabel = makeLabel("Password", 12, Font.BOLD, TEXT_MUTED);
        passLabel.setAlignmentX(LEFT_ALIGNMENT);
        passLabel.setBorder(new EmptyBorder(14, 0, 0, 0));

        passwordField = new JPasswordField();
        styleTextField(passwordField);
        passwordField.setAlignmentX(LEFT_ALIGNMENT);

        // ---- Error message (hidden by default) ----
        errorLabel = makeLabel("", 12, Font.PLAIN, ERROR_RED);
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);
        errorLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        errorLabel.setVisible(false); // hidden until login fails

        // ---- Login button ----
        loginButton = makeButton("Login");
        loginButton.setAlignmentX(CENTER_ALIGNMENT);
        loginButton.setBorder(new EmptyBorder(0, 0, 0, 0));
        // Attach the login action
        loginButton.addActionListener(e -> handleLoginClicked());

        // Also allow pressing Enter in the password field to trigger login
        passwordField.addActionListener(e -> handleLoginClicked());

        // ---- Divider label ----
        JLabel divider = makeLabel("──────  or  ──────", 11, Font.PLAIN, TEXT_MUTED);
        divider.setAlignmentX(CENTER_ALIGNMENT);
        divider.setBorder(new EmptyBorder(18, 0, 10, 0));

        // ---- Register link ----
        JLabel registerLink = makeLabel("Don't have an account?  Register here", 12, Font.PLAIN, ACCENT_BLUE);
        registerLink.setAlignmentX(CENTER_ALIGNMENT);
        registerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // pointer cursor
        registerLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onGoToRegister.run(); // Tell MainWindow to show RegisterPanel
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                // Underline on hover (like a real hyperlink)
                registerLink.setText("<html><u>Don't have an account?  Register here</u></html>");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                registerLink.setText("Don't have an account?  Register here");
            }
        });

        // ---- Assemble the card ----
        card.add(iconLabel);
        card.add(titleLabel);
        card.add(subLabel);
        card.add(userLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6))); // small gap
        card.add(usernameField);
        card.add(passLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(passwordField);
        card.add(errorLabel);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(loginButton);
        card.add(divider);
        card.add(registerLink);

        // ---- Add the card to the centre of this panel ----
        // GridBagConstraints with default values centres the card
        add(card, new GridBagConstraints());
    }


    // -------------------------------------------------------
    //  handleLoginClicked()  — runs when Login button is pressed
    // -------------------------------------------------------

    /**
     * Reads the username and password from the fields,
     * calls AuthService.login(), and reacts to the result.
     *
     * Called by: the login button's ActionListener (and Enter key in password field).
     */
    private void handleLoginClicked() {

        // Read what the user typed
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()); // char[] → String

        // Show a brief "loading" state so the user knows something is happening
        loginButton.setText("Signing in...");
        loginButton.setEnabled(false);

        // Run login on a background thread so the UI does not freeze
        // (DB queries can take a moment — we don't want the button to "lock up")
        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() {
                // This runs on a background thread — safe to do DB work here
                return authService.login(username, password);
            }

            @Override
            protected void done() {
                // This runs back on the Event Dispatch Thread (UI thread) — safe to update UI
                try {
                    User loggedInUser = get(); // Get the result from doInBackground()

                    if (loggedInUser != null) {
                        // ---- LOGIN SUCCEEDED ----
                        hideError();
                        clearFields();
                        onLoginSuccess.run(); // Tell MainWindow to switch to HomePanel

                    } else {
                        // ---- LOGIN FAILED ----
                        showError("Invalid username or password. Please try again.");
                    }

                } catch (Exception ex) {
                    showError("Connection error. Please check your database settings.");
                    System.err.println("[LoginPanel] Login error: " + ex.getMessage());

                } finally {
                    // Re-enable the button regardless of result
                    loginButton.setText("Login");
                    loginButton.setEnabled(true);
                }
            }
        };

        worker.execute(); // Start the background task
    }


    // -------------------------------------------------------
    //  HELPER METHODS  — show/hide error, clear fields
    // -------------------------------------------------------

    /**
     * Shows an error message in red below the password field.
     * The label is made visible and its text is set.
     *
     * @param message The error text to display (e.g. "Invalid username or password")
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        revalidate(); // Recalculate layout to make room for the label
        repaint();
    }

    /**
     * Hides the error label (called on successful login).
     */
    private void hideError() {
        errorLabel.setVisible(false);
    }

    /**
     * Clears both input fields after a successful login.
     * This is good practice so if the user logs out and comes back,
     * the fields do not show the previous user's data.
     */
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
    }


    // -------------------------------------------------------
    //  FACTORY HELPERS  — make styled components cleanly
    // -------------------------------------------------------

    /**
     * Creates a pre-styled JLabel with our app's font and colour.
     *
     * @param text      The text to display
     * @param fontSize  Font size in points
     * @param style     Font.BOLD or Font.PLAIN
     * @param colour    Text colour (use our colour constants above)
     * @return A ready-to-add JLabel
     */
    private JLabel makeLabel(String text, int fontSize, int style, Color colour) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, fontSize));
        label.setForeground(colour);
        return label;
    }

    /**
     * Creates a styled text input field with placeholder text.
     *
     * @param placeholder  Grey hint text shown when the field is empty
     * @return A styled JTextField
     */
    private JTextField makeTextField(String placeholder) {
        JTextField field = new JTextField();
        styleTextField(field);
        // We use a simple approach — clear the hint on focus
        field.setText(placeholder);
        field.setForeground(TEXT_MUTED);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_WHITE);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TEXT_MUTED);
                }
            }
        });
        return field;
    }

    /**
     * Applies a consistent dark-theme style to any JTextField or JPasswordField.
     *
     * @param field The text component to style (works for both JTextField and JPasswordField)
     */
    private void styleTextField(JTextField field) {
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT_WHITE);
        field.setCaretColor(ACCENT_BLUE);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_DIM, 1, true),  // rounded border
                new EmptyBorder(10, 14, 10, 14)                         // inner padding
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); // full width, fixed height
    }

    /**
     * Creates a styled accent-coloured button with hover effect.
     *
     * @param text  The button label
     * @return A ready-to-add JButton
     */
    private JButton makeButton(String text) {
        JButton button = new JButton(text) {
            // Custom painting to get a rounded, filled button
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Use hover colour if mouse is over button, otherwise normal accent blue
                g2.setColor(getModel().isRollover() ? BTN_HOVER : ACCENT_BLUE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };

        button.setText(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(ACCENT_BLUE);
        button.setContentAreaFilled(false); // We paint our own background above
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setPreferredSize(new Dimension(300, 46));

        return button;
    }
}
