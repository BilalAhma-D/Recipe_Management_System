package com.recipe.gui;

import com.recipe.services.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * ============================================================
 *  FILE        : RegisterPanel.java
 *  PACKAGE     : com.recipe.gui
 *  AUTHOR      : Ahmad
 *  MODULE      : User & Authentication — GUI Layer
 * ============================================================
 *
 *  WHAT THIS SCREEN DOES:
 *  -----------------------
 *  RegisterPanel is the new-account screen. It shows:
 *    - Username field
 *    - Email field
 *    - Password field  +  live strength indicator (JProgressBar)
 *    - Confirm Password field
 *    - Register button
 *    - A link to go back to Login
 *
 *  KEY FEATURE — LIVE PASSWORD STRENGTH INDICATOR:
 *  ------------------------------------------------
 *  As the user types in the password field, a JProgressBar updates
 *  in real time to show how strong the password is.
 *  Scoring logic:
 *    +1 point  — at least 6 characters
 *    +1 point  — at least 10 characters
 *    +1 point  — contains a digit (0-9)
 *    +1 point  — contains a special character (!, @, #, etc.)
 *  Score 0-1 → Red "Weak"
 *  Score 2   → Orange "Fair"
 *  Score 3   → Yellow "Good"
 *  Score 4   → Green "Strong"
 *
 *  This uses a DocumentListener on the password field — it fires
 *  every time a character is inserted or removed.
 *
 *  VIVA TIP:
 *  ---------
 *  "I used a DocumentListener to react to every keystroke in the
 *   password field. This is better than an ActionListener (which
 *   only fires on Enter) because it gives real-time feedback.
 *   The strength score is computed locally without any DB call."
 */
public class RegisterPanel extends JPanel {

    // -------------------------------------------------------
    //  COLOURS  (same theme as LoginPanel for consistency)
    // -------------------------------------------------------

    private static final Color BG_DARK     = new Color(15, 15, 30);
    private static final Color CARD_BG     = new Color(25, 28, 50);
    private static final Color ACCENT_BLUE = new Color(79, 195, 247);
    private static final Color TEXT_WHITE  = new Color(230, 235, 245);
    private static final Color TEXT_MUTED  = new Color(140, 145, 165);
    private static final Color FIELD_BG    = new Color(35, 38, 65);
    private static final Color BORDER_DIM  = new Color(60, 65, 100);
    private static final Color ERROR_RED   = new Color(255, 90, 100);
    private static final Color SUCCESS_GRN = new Color(72, 199, 142);

    // Strength bar colours
    private static final Color STRENGTH_WEAK   = new Color(255, 90, 100);   // Red
    private static final Color STRENGTH_FAIR   = new Color(255, 160, 50);   // Orange
    private static final Color STRENGTH_GOOD   = new Color(255, 210, 50);   // Yellow
    private static final Color STRENGTH_STRONG = new Color(72, 199, 142);   // Green

    // -------------------------------------------------------
    //  COMPONENT FIELDS
    // -------------------------------------------------------

    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JProgressBar strengthBar;       // Live password strength bar
    private JLabel strengthLabel;           // "Weak" / "Fair" / "Good" / "Strong"
    private JLabel messageLabel;            // Success or error message

    // -------------------------------------------------------
    //  SERVICE & CALLBACKS
    // -------------------------------------------------------

    private AuthService authService;
    private Runnable onGoToLogin;    // Called when "Back to Login" is clicked


    // -------------------------------------------------------
    //  CONSTRUCTOR
    // -------------------------------------------------------

    /**
     * Creates the Register screen.
     *
     * @param authService  The AuthService that handles registration logic.
     * @param onGoToLogin  Runnable provided by MainWindow — switches to LoginPanel.
     */
    public RegisterPanel(AuthService authService, Runnable onGoToLogin) {
        this.authService = authService;
        this.onGoToLogin = onGoToLogin;
        buildLayout();
    }


    // -------------------------------------------------------
    //  buildLayout()
    // -------------------------------------------------------

    /**
     * Builds the full visual layout of the register screen.
     *
     * Layout:
     *   RegisterPanel (dark background, GridBagLayout centres card)
     *     └── card (vertical BoxLayout)
     *           ├── Title
     *           ├── Username field
     *           ├── Email field
     *           ├── Password field
     *           ├── Strength bar
     *           ├── Confirm Password field
     *           ├── Message label
     *           ├── Register button
     *           └── Back to Login link
     */
    private void buildLayout() {
        setLayout(new GridBagLayout());
        setBackground(BG_DARK);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(new EmptyBorder(40, 50, 40, 50));
        card.setPreferredSize(new Dimension(440, 640));

        // ---- Header ----
        JLabel iconLabel = new JLabel("✨", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        iconLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel titleLabel = makeLabel("Create Account", 22, Font.BOLD, TEXT_WHITE);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subLabel = makeLabel("Join Recipe Manager today", 12, Font.PLAIN, TEXT_MUTED);
        subLabel.setAlignmentX(CENTER_ALIGNMENT);
        subLabel.setBorder(new EmptyBorder(4, 0, 24, 0));

        // ---- Username ----
        JLabel userLabel = makeLabel("Username", 11, Font.BOLD, TEXT_MUTED);
        userLabel.setAlignmentX(LEFT_ALIGNMENT);
        usernameField = makeTextField("Choose a username");
        usernameField.setAlignmentX(LEFT_ALIGNMENT);

        // ---- Email ----
        JLabel emailLabel = makeLabel("Email", 11, Font.BOLD, TEXT_MUTED);
        emailLabel.setAlignmentX(LEFT_ALIGNMENT);
        emailLabel.setBorder(new EmptyBorder(12, 0, 0, 0));
        emailField = makeTextField("Enter your email");
        emailField.setAlignmentX(LEFT_ALIGNMENT);

        // ---- Password + Strength Bar ----
        JLabel passLabel = makeLabel("Password", 11, Font.BOLD, TEXT_MUTED);
        passLabel.setAlignmentX(LEFT_ALIGNMENT);
        passLabel.setBorder(new EmptyBorder(12, 0, 0, 0));

        passwordField = new JPasswordField();
        styleTextField(passwordField);
        passwordField.setAlignmentX(LEFT_ALIGNMENT);

        // Strength bar — updates live as user types
        strengthBar = new JProgressBar(0, 4);
        strengthBar.setValue(0);
        strengthBar.setStringPainted(false);
        strengthBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        strengthBar.setBackground(FIELD_BG);
        strengthBar.setForeground(STRENGTH_WEAK);
        strengthBar.setBorderPainted(false);
        strengthBar.setAlignmentX(LEFT_ALIGNMENT);

        // Label next to bar — "Weak", "Fair", "Good", "Strong"
        strengthLabel = makeLabel("", 10, Font.PLAIN, TEXT_MUTED);
        strengthLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Attach the listener that updates the bar on every keystroke
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { updateStrengthBar(); }
            @Override public void removeUpdate(DocumentEvent e)  { updateStrengthBar(); }
            @Override public void changedUpdate(DocumentEvent e) { updateStrengthBar(); }
        });

        // ---- Confirm Password ----
        JLabel confirmLabel = makeLabel("Confirm Password", 11, Font.BOLD, TEXT_MUTED);
        confirmLabel.setAlignmentX(LEFT_ALIGNMENT);
        confirmLabel.setBorder(new EmptyBorder(12, 0, 0, 0));
        confirmPasswordField = new JPasswordField();
        styleTextField(confirmPasswordField);
        confirmPasswordField.setAlignmentX(LEFT_ALIGNMENT);
        confirmPasswordField.addActionListener(e -> handleRegisterClicked());

        // ---- Message label (success / error) ----
        messageLabel = makeLabel("", 12, Font.PLAIN, ERROR_RED);
        messageLabel.setAlignmentX(LEFT_ALIGNMENT);
        messageLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        messageLabel.setVisible(false);

        // ---- Register button ----
        JButton registerButton = makeButton("Create Account");
        registerButton.setAlignmentX(CENTER_ALIGNMENT);
        registerButton.addActionListener(e -> handleRegisterClicked());

        // ---- Back to Login link ----
        JLabel loginLink = makeLabel("Already have an account?  Sign in", 12, Font.PLAIN, ACCENT_BLUE);
        loginLink.setAlignmentX(CENTER_ALIGNMENT);
        loginLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginLink.setBorder(new EmptyBorder(14, 0, 0, 0));
        loginLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onGoToLogin.run(); // Switch back to LoginPanel
            }
        });

        // ---- Assemble card ----
        card.add(iconLabel);
        card.add(titleLabel);
        card.add(subLabel);
        card.add(userLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(usernameField);
        card.add(emailLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(emailField);
        card.add(passLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(passwordField);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(strengthBar);
        card.add(strengthLabel);
        card.add(confirmLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(confirmPasswordField);
        card.add(messageLabel);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(registerButton);
        card.add(loginLink);

        add(card, new GridBagConstraints());
    }


    // -------------------------------------------------------
    //  handleRegisterClicked()
    // -------------------------------------------------------

    /**
     * Reads all fields and calls AuthService.register().
     * Shows success or error feedback.
     *
     * Called by the Register button and Enter key in confirmPasswordField.
     */
    private void handleRegisterClicked() {

        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm  = new String(confirmPasswordField.getPassword());

        // Call AuthService — it validates and saves to DB
        String result = authService.register(username, email, password, confirm);

        if ("SUCCESS".equals(result)) {
            // Show success message in green, then navigate to login
            showMessage("Account created! Please log in.", SUCCESS_GRN);
            clearFields();
            // Auto-navigate to login after 1.5 seconds
            Timer timer = new Timer(1500, e -> onGoToLogin.run());
            timer.setRepeats(false);
            timer.start();
        } else {
            // Show the error message from AuthService in red
            showMessage(result, ERROR_RED);
        }
    }


    // -------------------------------------------------------
    //  updateStrengthBar()  — live password strength checker
    // -------------------------------------------------------

    /**
     * Calculates a password strength score and updates the progress bar.
     *
     * Called by DocumentListener every time the password field changes.
     *
     * SCORING RULES:
     *   +1 if length >= 6
     *   +1 if length >= 10
     *   +1 if contains at least one digit
     *   +1 if contains at least one special character
     * Maximum score = 4 → "Strong"
     */
    private void updateStrengthBar() {
        String pass = new String(passwordField.getPassword());
        int score = 0;

        if (pass.length() >= 6)  score++;            // Basic length
        if (pass.length() >= 10) score++;            // Long password
        if (pass.matches(".*\\d.*")) score++;        // Has a number
        if (pass.matches(".*[!@#$%^&*()_+\\-=].*")) score++; // Has special char

        strengthBar.setValue(score);

        // Change bar colour and label based on score
        if (score <= 1) {
            strengthBar.setForeground(STRENGTH_WEAK);
            strengthLabel.setForeground(STRENGTH_WEAK);
            strengthLabel.setText(pass.isEmpty() ? "" : "Weak");
        } else if (score == 2) {
            strengthBar.setForeground(STRENGTH_FAIR);
            strengthLabel.setForeground(STRENGTH_FAIR);
            strengthLabel.setText("Fair");
        } else if (score == 3) {
            strengthBar.setForeground(STRENGTH_GOOD);
            strengthLabel.setForeground(STRENGTH_GOOD);
            strengthLabel.setText("Good");
        } else {
            strengthBar.setForeground(STRENGTH_STRONG);
            strengthLabel.setForeground(STRENGTH_STRONG);
            strengthLabel.setText("Strong");
        }
    }


    // -------------------------------------------------------
    //  HELPER METHODS
    // -------------------------------------------------------

    private void showMessage(String text, Color colour) {
        messageLabel.setText(text);
        messageLabel.setForeground(colour);
        messageLabel.setVisible(true);
        revalidate();
        repaint();
    }

    private void clearFields() {
        usernameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        strengthBar.setValue(0);
        strengthLabel.setText("");
    }

    private JLabel makeLabel(String text, int size, int style, Color colour) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(colour);
        return label;
    }

    private JTextField makeTextField(String placeholder) {
        JTextField field = new JTextField();
        styleTextField(field);
        field.setText(placeholder);
        field.setForeground(TEXT_MUTED);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_WHITE);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TEXT_MUTED);
                }
            }
        });
        return field;
    }

    private void styleTextField(JTextField field) {
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT_WHITE);
        field.setCaretColor(ACCENT_BLUE);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_DIM, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
    }

    private JButton makeButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(50, 160, 220) : ACCENT_BLUE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setPreferredSize(new Dimension(320, 46));
        return button;
    }
}
