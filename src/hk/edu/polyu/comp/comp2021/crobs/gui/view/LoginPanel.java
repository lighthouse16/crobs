package hk.edu.polyu.comp.comp2021.crobs.gui.view;

import hk.edu.polyu.comp.comp2021.crobs.gui.GuiController;
import hk.edu.polyu.comp.comp2021.crobs.gui.GuiTheme;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.util.List;

/**
 * Screen: Sign in to CROBS.
 * Dedicated initial view matching official CROBS V3 specification.
 * Features clean centered card, quick user selector pills, and seamless login flow.
 */
public class LoginPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final GuiController controller;
    private final Runnable onLoginSuccess;

    private JTextField userIdField;
    private JLabel errorLabel;

    public LoginPanel(GuiController controller, Runnable onLoginSuccess) {
        this.controller = controller;
        this.onLoginSuccess = onLoginSuccess;

        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBackground(GuiTheme.COLOR_BG);

        // Center Login Card: 400px width, clean rounded border, white background
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(GuiTheme.COLOR_BORDER, 1),
                new EmptyBorder(36, 36, 36, 36)
        ));
        card.setPreferredSize(new Dimension(400, 390));
        card.setMaximumSize(new Dimension(400, 390));

        // Brand Icon (28x28)
        JPanel brandIcon = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GuiTheme.COLOR_BRAND);
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawRoundRect(1, 1, 26, 26, 7, 7);
                g2.fillRoundRect(7, 7, 5, 5, 1, 1);
                g2.fillRoundRect(16, 7, 5, 5, 1, 1);
                g2.fillRoundRect(7, 16, 5, 5, 1, 1);
                g2.fillRoundRect(16, 16, 5, 5, 1, 1);
                g2.dispose();
            }
        };
        brandIcon.setPreferredSize(new Dimension(28, 28));
        brandIcon.setMaximumSize(new Dimension(28, 28));
        brandIcon.setOpaque(false);
        brandIcon.setAlignmentX(LEFT_ALIGNMENT);
        card.add(brandIcon);
        card.add(Box.createVerticalStrut(14));

        // Heading: Sign in to CROBS
        JLabel title = new JLabel("Sign in to CROBS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 23));
        title.setForeground(GuiTheme.COLOR_TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));

        // Subtitle
        JLabel desc = new JLabel("Enter a user ID to continue. The project does not use passwords.");
        desc.setFont(GuiTheme.FONT_BODY);
        desc.setForeground(GuiTheme.COLOR_MUTED);
        desc.setAlignmentX(LEFT_ALIGNMENT);
        card.add(desc);
        card.add(Box.createVerticalStrut(22));

        // Field: User ID
        JLabel idLabel = new JLabel("User ID");
        idLabel.setFont(GuiTheme.FONT_SMALL_BOLD);
        idLabel.setForeground(new Color(80, 90, 102));
        idLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(idLabel);
        card.add(Box.createVerticalStrut(6));

        userIdField = GuiTheme.createTextField(20);
        userIdField.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
        userIdField.setPreferredSize(new Dimension(Short.MAX_VALUE, 40));
        userIdField.setAlignmentX(LEFT_ALIGNMENT);
        userIdField.addActionListener(e -> attemptLogin(userIdField.getText()));
        card.add(userIdField);
        card.add(Box.createVerticalStrut(6));

        // Error Label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(GuiTheme.FONT_SMALL);
        errorLabel.setForeground(GuiTheme.COLOR_DANGER);
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(10));

        // Sign In Button
        JButton signinBtn = GuiTheme.createPrimaryButton("Sign in");
        signinBtn.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
        signinBtn.setPreferredSize(new Dimension(Short.MAX_VALUE, 40));
        signinBtn.setAlignmentX(LEFT_ALIGNMENT);
        signinBtn.addActionListener(e -> attemptLogin(userIdField.getText()));
        card.add(signinBtn);
        card.add(Box.createVerticalStrut(20));

        // Quick sign-in pill buttons
        JLabel quickLabel = new JLabel("Quick sign-in for preview:");
        quickLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        quickLabel.setForeground(new Color(130, 140, 152));
        quickLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(quickLabel);
        card.add(Box.createVerticalStrut(8));

        JPanel quickGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quickGroup.setOpaque(false);
        quickGroup.setAlignmentX(LEFT_ALIGNMENT);

        quickGroup.add(createQuickPill("u1 (Superadmin)", "u1"));
        quickGroup.add(createQuickPill("u2 (Alex Chen)", "u2"));
        quickGroup.add(createQuickPill("u3 (Jamie Lee)", "u3"));

        card.add(quickGroup);

        add(card);
    }

    private JButton createQuickPill(String text, String id) {
        JButton pill = new JButton(text);
        pill.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pill.setForeground(new Color(60, 94, 143));
        pill.setBackground(new Color(240, 244, 250));
        pill.setBorder(new CompoundBorder(
                new LineBorder(new Color(210, 222, 238), 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        pill.setFocusPainted(false);
        pill.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pill.addActionListener(e -> {
            userIdField.setText(id);
            attemptLogin(id);
        });
        return pill;
    }

    private void attemptLogin(String id) {
        errorLabel.setText(" ");
        String cleanId = (id == null) ? "" : id.trim().toLowerCase();
        if (cleanId.isEmpty()) {
            errorLabel.setText("Please enter a User ID.");
            return;
        }

        try {
            controller.login(cleanId);
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            }
        } catch (CrobsException ex) {
            errorLabel.setText("User ID not found: " + cleanId);
        }
    }

    public void reset() {
        userIdField.setText("");
        errorLabel.setText(" ");
        javax.swing.SwingUtilities.invokeLater(() -> userIdField.requestFocusInWindow());
    }
}
