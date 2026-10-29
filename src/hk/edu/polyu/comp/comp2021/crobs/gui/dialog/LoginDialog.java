package hk.edu.polyu.comp.comp2021.crobs.gui.dialog;

import hk.edu.polyu.comp.comp2021.crobs.gui.GuiController;
import hk.edu.polyu.comp.comp2021.crobs.gui.GuiTheme;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

/**
 * Clean login dialog matching the CROBS V3 Sign In specification.
 */
public class LoginDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final GuiController controller;
    private JTextField userIdField;
    private JLabel errorLabel;

    /**
     * Constructs the LoginDialog.
     *
     * @param parent     parent JFrame.
     * @param controller GUI controller facade.
     */
    public LoginDialog(JFrame parent, GuiController controller) {
        super(parent, "Sign in to CROBS", true);
        this.controller = controller;

        initComponents();
    }

    private void initComponents() {
        setResizable(false);
        getContentPane().setBackground(GuiTheme.COLOR_PANEL);
        setLayout(new BorderLayout());

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(GuiTheme.COLOR_PANEL);
        card.setBorder(new EmptyBorder(32, 36, 32, 36));

        JLabel titleLabel = new JLabel("Sign in to CROBS");
        titleLabel.setFont(GuiTheme.FONT_H1);
        titleLabel.setForeground(GuiTheme.COLOR_TEXT);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("<html>Enter a user ID to continue. The project does not use passwords.</html>");
        subtitleLabel.setFont(GuiTheme.FONT_BODY);
        subtitleLabel.setForeground(GuiTheme.COLOR_MUTED);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        subtitleLabel.setBorder(new EmptyBorder(6, 0, 24, 0));

        JLabel idLabel = new JLabel("User ID");
        idLabel.setFont(GuiTheme.FONT_SMALL_BOLD);
        idLabel.setForeground(GuiTheme.COLOR_MUTED);
        idLabel.setAlignmentX(LEFT_ALIGNMENT);

        userIdField = GuiTheme.createTextField(16);
        userIdField.setMaximumSize(new Dimension(320, 38));
        userIdField.setAlignmentX(LEFT_ALIGNMENT);
        userIdField.addActionListener(e -> attemptLogin());

        errorLabel = new JLabel(" ");
        errorLabel.setFont(GuiTheme.FONT_SMALL);
        errorLabel.setForeground(GuiTheme.COLOR_DANGER);
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);
        errorLabel.setBorder(new EmptyBorder(6, 0, 16, 0));

        JButton signInBtn = GuiTheme.createPrimaryButton("Sign in");
        signInBtn.setMaximumSize(new Dimension(320, 38));
        signInBtn.setAlignmentX(LEFT_ALIGNMENT);
        signInBtn.addActionListener(e -> attemptLogin());

        JLabel hintLabel = new JLabel("<html><small>Default SuperAdmin ID: <b>u1</b></small></html>");
        hintLabel.setFont(GuiTheme.FONT_SMALL);
        hintLabel.setForeground(GuiTheme.COLOR_SUBTLE);
        hintLabel.setAlignmentX(LEFT_ALIGNMENT);
        hintLabel.setBorder(new EmptyBorder(16, 0, 0, 0));

        card.add(titleLabel);
        card.add(subtitleLabel);
        card.add(idLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(userIdField);
        card.add(errorLabel);
        card.add(signInBtn);
        card.add(hintLabel);

        add(card, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getParent());
    }

    private void attemptLogin() {
        String uid = userIdField.getText().trim();
        if (uid.isEmpty()) {
            errorLabel.setText("Please enter your user ID.");
            return;
        }

        try {
            controller.login(uid);
            dispose();
        } catch (CrobsException e) {
            errorLabel.setText(e.getMessage());
        } catch (Exception e) {
            errorLabel.setText("Login error: " + e.getMessage());
        }
    }

    /**
     * Helper to show the login modal dialog.
     *
     * @param parent     parent window.
     * @param controller GUI controller.
     */
    public static void showLogin(JFrame parent, GuiController controller) {
        LoginDialog dialog = new LoginDialog(parent, controller);
        dialog.setVisible(true);
    }
}
