package com.recipe.gui;

import com.recipe.models.User;
import com.recipe.services.AuthService;
import com.recipe.services.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * ============================================================
 *  FILE        : ProfilePanel.java
 *  PACKAGE     : com.recipe.gui
 *  AUTHOR      : Ahmad
 *  MODULE      : User & Authentication — GUI Layer
 * ============================================================
 *
 *  WHAT THIS SCREEN DOES:
 *  -----------------------
 *  ProfilePanel shows the currently logged-in user's account info:
 *    - A circular avatar (shows first letter of username)
 *    - Username and email
 *    - Total recipes they have created (placeholder — Bilal's RecipeDAO fills this)
 *    - "Edit Profile" section — change username and email
 *    - "Change Password" section — enter old + new password
 *    - "Logout" button
 *
 *  HOW IT GETS THE CURRENT USER:
 *  --------------------------------
 *  ProfilePanel calls SessionManager.getInstance().getCurrentUser()
 *  to get the logged-in User object. It does NOT accept a User
 *  parameter — SessionManager is the single source of truth.
 *
 *  COMMUNICATION:
 *    Edit Profile button  → calls AuthService.updateProfile()
 *    Change Password      → calls AuthService.changePassword()
 *    Logout button        → calls AuthService.logout() then onLogout.run()
 *
 *  VIVA TIP:
 *  ---------
 *  "ProfilePanel reads user data from SessionManager rather than
 *   accepting it as a constructor argument. This keeps the panel
 *   loosely coupled — if the user object changes in SessionManager,
 *   ProfilePanel can always call getCurrentUser() to get fresh data."
 */
public class ProfilePanel extends JPanel {

    // -------------------------------------------------------
    //  COLOURS
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
    private static final Color AVATAR_BG   = new Color(79, 195, 247);

    // -------------------------------------------------------
    //  COMPONENT FIELDS
    // -------------------------------------------------------

    // Profile display section
    private JLabel avatarLabel;         // Circle with first letter of username
    private JLabel displayNameLabel;    // Shows username
    private JLabel displayEmailLabel;   // Shows email

    // Edit profile section
    private JTextField newUsernameField;
    private JTextField newEmailField;
    private JLabel profileMessageLabel;

    // Change password section
    private JPasswordField oldPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmNewPasswordField;
    private JLabel passwordMessageLabel;

    // -------------------------------------------------------
    //  SERVICE & CALLBACKS
    // -------------------------------------------------------

    private AuthService authService;
    private Runnable onLogout;   // Called when user clicks logout → MainWindow shows LoginPanel


    // -------------------------------------------------------
    //  CONSTRUCTOR
    // -------------------------------------------------------

    /**
     * Creates the Profile screen.
     *
     * @param authService  The AuthService for updating profile and password.
     * @param onLogout     Runnable from MainWindow — switches to LoginPanel after logout.
     */
    public ProfilePanel(AuthService authService, Runnable onLogout) {
        this.authService = authService;
        this.onLogout = onLogout;
        buildLayout();
    }

    /**
     * Refreshes the displayed user data from SessionManager.
     *
     * Call this every time the ProfilePanel is shown (e.g., in CardLayout's show callback)
     * so the latest username/email from SessionManager is displayed.
     */
    public void refreshUserData() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            displayNameLabel.setText(user.getUsername());
            displayEmailLabel.setText(user.getEmail());
            // Update avatar letter
            avatarLabel.setText(String.valueOf(user.getUsername().charAt(0)).toUpperCase());
        }
    }


    // -------------------------------------------------------
    //  buildLayout()
    // -------------------------------------------------------

    private void buildLayout() {
        setLayout(new BorderLayout());
        setBackground(BG_DARK);

        // Main scroll area so profile fits on small screens
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_DARK);
        contentPanel.setBorder(new EmptyBorder(30, 60, 30, 60));

        // ---- Build each section ----
        contentPanel.add(buildAvatarCard());
        contentPanel.add(Box.createRigidArea(new Dimension(0, 24)));
        contentPanel.add(buildEditProfileCard());
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        contentPanel.add(buildChangePasswordCard());
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        contentPanel.add(buildLogoutCard());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG_DARK);
        scrollPane.getViewport().setBackground(BG_DARK);
        add(scrollPane, BorderLayout.CENTER);
    }


    // -------------------------------------------------------
    //  buildAvatarCard()  — top section with avatar and name
    // -------------------------------------------------------

    /**
     * Builds the top "hero" card that shows the user's avatar, name, and email.
     *
     * The avatar is a coloured circle with the first letter of the username painted inside.
     * We draw it using a custom JPanel with paintComponent.
     *
     * @return A JPanel card with avatar info
     */
    private JPanel buildAvatarCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // ---- Circular avatar panel ----
        JPanel avatarCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Draw the circle
                g2.setColor(AVATAR_BG);
                g2.fill(new Ellipse2D.Float(0, 0, 80, 80));
                g2.dispose();
            }
        };
        avatarCircle.setPreferredSize(new Dimension(80, 80));
        avatarCircle.setMaximumSize(new Dimension(80, 80));
        avatarCircle.setBackground(CARD_BG);
        avatarCircle.setOpaque(false);
        avatarCircle.setLayout(new GridBagLayout());

        // Letter inside the circle
        User user = SessionManager.getInstance().getCurrentUser();
        String firstLetter = (user != null && !user.getUsername().isEmpty())
                ? String.valueOf(user.getUsername().charAt(0)).toUpperCase()
                : "?";

        avatarLabel = new JLabel(firstLetter);
        avatarLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        avatarLabel.setForeground(Color.WHITE);
        avatarCircle.add(avatarLabel);

        // Wrapper to centre the circle horizontally
        JPanel avatarWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        avatarWrapper.setBackground(CARD_BG);
        avatarWrapper.add(avatarCircle);

        // Name and email labels
        displayNameLabel = makeLabel(user != null ? user.getUsername() : "—", 20, Font.BOLD, TEXT_WHITE);
        displayNameLabel.setAlignmentX(CENTER_ALIGNMENT);

        displayEmailLabel = makeLabel(user != null ? user.getEmail() : "—", 13, Font.PLAIN, TEXT_MUTED);
        displayEmailLabel.setAlignmentX(CENTER_ALIGNMENT);
        displayEmailLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel memberLabel = makeLabel("Recipe Manager Member", 11, Font.PLAIN, ACCENT_BLUE);
        memberLabel.setAlignmentX(CENTER_ALIGNMENT);
        memberLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        card.add(avatarWrapper);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(displayNameLabel);
        card.add(displayEmailLabel);
        card.add(memberLabel);

        return card;
    }


    // -------------------------------------------------------
    //  buildEditProfileCard()
    // -------------------------------------------------------

    /**
     * Builds the "Edit Profile" section where the user can change username and email.
     *
     * "Save Changes" calls AuthService.updateProfile().
     *
     * @return A card JPanel with the edit profile form
     */
    private JPanel buildEditProfileCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel sectionTitle = makeLabel("✏  Edit Profile", 15, Font.BOLD, TEXT_WHITE);
        sectionTitle.setAlignmentX(LEFT_ALIGNMENT);
        sectionTitle.setBorder(new EmptyBorder(0, 0, 16, 0));

        User user = SessionManager.getInstance().getCurrentUser();

        JLabel userLabel = makeLabel("New Username", 11, Font.BOLD, TEXT_MUTED);
        userLabel.setAlignmentX(LEFT_ALIGNMENT);
        newUsernameField = new JTextField(user != null ? user.getUsername() : "");
        styleTextField(newUsernameField);
        newUsernameField.setAlignmentX(LEFT_ALIGNMENT);

        JLabel emailLabel = makeLabel("New Email", 11, Font.BOLD, TEXT_MUTED);
        emailLabel.setAlignmentX(LEFT_ALIGNMENT);
        emailLabel.setBorder(new EmptyBorder(12, 0, 0, 0));
        newEmailField = new JTextField(user != null ? user.getEmail() : "");
        styleTextField(newEmailField);
        newEmailField.setAlignmentX(LEFT_ALIGNMENT);

        profileMessageLabel = makeLabel("", 12, Font.PLAIN, ERROR_RED);
        profileMessageLabel.setAlignmentX(LEFT_ALIGNMENT);
        profileMessageLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        profileMessageLabel.setVisible(false);

        JButton saveButton = makeButton("Save Changes", ACCENT_BLUE);
        saveButton.setAlignmentX(LEFT_ALIGNMENT);
        saveButton.addActionListener(e -> {
            String result = authService.updateProfile(
                    newUsernameField.getText(),
                    newEmailField.getText()
            );
            if ("SUCCESS".equals(result)) {
                showMsg(profileMessageLabel, "Profile updated successfully!", SUCCESS_GRN);
                refreshUserData(); // Update the avatar card above
            } else {
                showMsg(profileMessageLabel, result, ERROR_RED);
            }
        });

        card.add(sectionTitle);
        card.add(userLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(newUsernameField);
        card.add(emailLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(newEmailField);
        card.add(profileMessageLabel);
        card.add(Box.createRigidArea(new Dimension(0, 16)));
        card.add(saveButton);

        return card;
    }


    // -------------------------------------------------------
    //  buildChangePasswordCard()
    // -------------------------------------------------------

    /**
     * Builds the "Change Password" section.
     *
     * Requires old password for verification, then new + confirm.
     * Calls AuthService.changePassword() which validates and hashes.
     *
     * @return A card JPanel with the password change form
     */
    private JPanel buildChangePasswordCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel sectionTitle = makeLabel("🔒  Change Password", 15, Font.BOLD, TEXT_WHITE);
        sectionTitle.setAlignmentX(LEFT_ALIGNMENT);
        sectionTitle.setBorder(new EmptyBorder(0, 0, 16, 0));

        // Current password
        JLabel oldLabel = makeLabel("Current Password", 11, Font.BOLD, TEXT_MUTED);
        oldLabel.setAlignmentX(LEFT_ALIGNMENT);
        oldPasswordField = new JPasswordField();
        styleTextField(oldPasswordField);
        oldPasswordField.setAlignmentX(LEFT_ALIGNMENT);

        // New password
        JLabel newLabel = makeLabel("New Password", 11, Font.BOLD, TEXT_MUTED);
        newLabel.setAlignmentX(LEFT_ALIGNMENT);
        newLabel.setBorder(new EmptyBorder(12, 0, 0, 0));
        newPasswordField = new JPasswordField();
        styleTextField(newPasswordField);
        newPasswordField.setAlignmentX(LEFT_ALIGNMENT);

        // Confirm new
        JLabel confirmLabel = makeLabel("Confirm New Password", 11, Font.BOLD, TEXT_MUTED);
        confirmLabel.setAlignmentX(LEFT_ALIGNMENT);
        confirmLabel.setBorder(new EmptyBorder(12, 0, 0, 0));
        confirmNewPasswordField = new JPasswordField();
        styleTextField(confirmNewPasswordField);
        confirmNewPasswordField.setAlignmentX(LEFT_ALIGNMENT);

        passwordMessageLabel = makeLabel("", 12, Font.PLAIN, ERROR_RED);
        passwordMessageLabel.setAlignmentX(LEFT_ALIGNMENT);
        passwordMessageLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        passwordMessageLabel.setVisible(false);

        JButton changeBtn = makeButton("Update Password", new Color(100, 100, 200));
        changeBtn.setAlignmentX(LEFT_ALIGNMENT);
        changeBtn.addActionListener(e -> {
            String result = authService.changePassword(
                    new String(oldPasswordField.getPassword()),
                    new String(newPasswordField.getPassword()),
                    new String(confirmNewPasswordField.getPassword())
            );
            if ("SUCCESS".equals(result)) {
                showMsg(passwordMessageLabel, "Password changed successfully!", SUCCESS_GRN);
                oldPasswordField.setText("");
                newPasswordField.setText("");
                confirmNewPasswordField.setText("");
            } else {
                showMsg(passwordMessageLabel, result, ERROR_RED);
            }
        });

        card.add(sectionTitle);
        card.add(oldLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(oldPasswordField);
        card.add(newLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(newPasswordField);
        card.add(confirmLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(confirmNewPasswordField);
        card.add(passwordMessageLabel);
        card.add(Box.createRigidArea(new Dimension(0, 16)));
        card.add(changeBtn);

        return card;
    }


    // -------------------------------------------------------
    //  buildLogoutCard()
    // -------------------------------------------------------

    /**
     * Builds a simple logout card with a red "Sign Out" button.
     * Calls AuthService.logout() then triggers the onLogout callback
     * so MainWindow switches back to the LoginPanel.
     *
     * @return A card JPanel with a logout button
     */
    private JPanel buildLogoutCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel desc = makeLabel("Done for now? Sign out of your account.", 13, Font.PLAIN, TEXT_MUTED);
        desc.setAlignmentX(LEFT_ALIGNMENT);
        desc.setBorder(new EmptyBorder(0, 0, 14, 0));

        JButton logoutButton = makeButton("Sign Out", new Color(200, 70, 80));
        logoutButton.setAlignmentX(LEFT_ALIGNMENT);
        logoutButton.addActionListener(e -> {
            authService.logout();  // Clears SessionManager
            onLogout.run();        // MainWindow switches to LoginPanel
        });

        card.add(desc);
        card.add(logoutButton);

        return card;
    }


    // -------------------------------------------------------
    //  HELPER METHODS
    // -------------------------------------------------------

    /**
     * Shows a message on a label with the given colour.
     * Automatically makes the label visible.
     */
    private void showMsg(JLabel label, String text, Color colour) {
        label.setText(text);
        label.setForeground(colour);
        label.setVisible(true);
        revalidate();
        repaint();
    }

    /**
     * Creates a card panel (rounded dark box with padding).
     * Used for each section to give a "card" visual layout.
     */
    private JPanel makeCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_DIM, 1, true),
                new EmptyBorder(24, 28, 24, 28)
        ));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private JLabel makeLabel(String text, int size, int style, Color colour) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(colour);
        return label;
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

    private JButton makeButton(String text, Color colour) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? colour.brighter() : colour);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(200, 42));
        btn.setPreferredSize(new Dimension(180, 42));
        return btn;
    }
}
