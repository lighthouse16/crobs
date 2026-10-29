package hk.edu.polyu.comp.comp2021.crobs.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Visual design system for CROBS V3 Desktop Workspace.
 * Faithfully matches the CROBS_GUI_V3 design specifications.
 */
public final class GuiTheme {

    private GuiTheme() {
        // Prevent instantiation
    }

    // --- Dimensions ---
    public static final int WINDOW_WIDTH = 1366;
    public static final int WINDOW_HEIGHT = 850;
    public static final int MIN_WINDOW_WIDTH = 1000;
    public static final int MIN_WINDOW_HEIGHT = 650;

    public static final int MARGIN_HORIZONTAL = 30;
    public static final int MARGIN_VERTICAL = 26;

    public static final int TABLE_ROW_HEIGHT = 64;
    public static final int TABLE_HEADER_HEIGHT = 38;

    // --- Color Palette from CROBS_GUI_V3 ---
    public static final Color COLOR_BG = new Color(248, 249, 250);
    public static final Color COLOR_PANEL = Color.WHITE;
    public static final Color COLOR_BORDER = new Color(222, 227, 232);
    public static final Color COLOR_DIVIDER = new Color(233, 237, 240);

    public static final Color COLOR_TEXT = new Color(32, 37, 44);
    public static final Color COLOR_MUTED = new Color(105, 115, 127);
    public static final Color COLOR_SUBTLE = new Color(148, 155, 165);

    public static final Color COLOR_BLUE = new Color(33, 79, 153);
    public static final Color COLOR_BLUE_HOVER = new Color(24, 62, 123);
    public static final Color COLOR_LIGHT = new Color(242, 245, 250);

    public static final Color COLOR_GREEN = new Color(40, 115, 78);
    public static final Color COLOR_GREEN_DOT = new Color(64, 129, 91);
    public static final Color COLOR_DANGER = new Color(171, 67, 61);
    public static final Color COLOR_DANGER_HOVER = new Color(145, 50, 45);
    public static final Color COLOR_DANGER_BG = new Color(252, 242, 241);

    public static final Color COLOR_BRAND = new Color(38, 49, 61);
    public static final Color COLOR_ACTIVE_TAB = new Color(32, 49, 67);

    // --- Typography (Segoe UI / Sans-serif) ---
    private static final String FONT_FAMILY = "Segoe UI";
    public static final Font FONT_H1 = new Font(FONT_FAMILY, Font.BOLD, 25);
    public static final Font FONT_H2 = new Font(FONT_FAMILY, Font.BOLD, 18);
    public static final Font FONT_SECTION = new Font(FONT_FAMILY, Font.BOLD, 15);
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font FONT_TINY = new Font(FONT_FAMILY, Font.PLAIN, 11);
    public static final Font FONT_CODE = new Font("Consolas", Font.PLAIN, 12);

    // --- Component Helpers ---

    /**
     * Creates a primary filled action button (#214f99).
     */
    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(190, 205, 228));
                } else if (getModel().isPressed()) {
                    g2.setColor(COLOR_BLUE_HOVER);
                } else if (getModel().isRollover()) {
                    g2.setColor(COLOR_BLUE_HOVER);
                } else {
                    g2.setColor(COLOR_BLUE);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BODY_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Creates a small primary button (height ~32px, padding 0 13px).
     */
    public static JButton createSmallPrimaryButton(String text) {
        JButton btn = createPrimaryButton(text);
        btn.setFont(FONT_SMALL_BOLD);
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 32));
        return btn;
    }

    /**
     * Creates a secondary white button with gray border.
     */
    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? COLOR_LIGHT : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BODY_BOLD);
        btn.setForeground(COLOR_TEXT);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Creates a small secondary button (height 30px).
     */
    public static JButton createSmallSecondaryButton(String text) {
        JButton btn = createSecondaryButton(text);
        btn.setFont(FONT_SMALL_BOLD);
        btn.setBorder(new EmptyBorder(5, 10, 5, 10));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 30));
        return btn;
    }

    /**
     * Creates a solid red danger button for destructive dialog confirmations.
     */
    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? COLOR_DANGER_BG : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(223, 183, 181));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BODY_BOLD);
        btn.setForeground(COLOR_DANGER);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Creates a clean flat text button (e.g. "View week", "Edit").
     */
    public static JButton createTextButton(String text) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isRollover()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(237, 242, 248));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BODY_BOLD);
        btn.setForeground(new Color(73, 97, 120));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(6, 10, 6, 10));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Creates a danger text button (e.g. "Cancel", "Remove", "Delete").
     */
    public static JButton createDangerTextButton(String text) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isRollover()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(COLOR_DANGER_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BODY_BOLD);
        btn.setForeground(COLOR_DANGER);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(6, 10, 6, 10));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Creates a styled text input field.
     */
    public static JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(FONT_BODY);
        field.setForeground(COLOR_TEXT);
        field.setBackground(Color.WHITE);
        field.setCaretColor(COLOR_TEXT);
        Border line = new LineBorder(COLOR_BORDER, 1);
        Border pad = new EmptyBorder(6, 10, 6, 10);
        field.setBorder(new CompoundBorder(line, pad));
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 39));
        return field;
    }

    /**
     * Styles a JTable to match the clean V3 tabular design.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setForeground(COLOR_TEXT);
        table.setBackground(Color.WHITE);
        table.setGridColor(COLOR_DIVIDER);
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(COLOR_LIGHT);
        table.setSelectionForeground(COLOR_TEXT);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_TINY);
        header.setBackground(new Color(242, 244, 246));
        header.setForeground(COLOR_MUTED);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, TABLE_HEADER_HEIGHT));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));

        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
        renderer.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
    }

    /**
     * Creates a card container panel with white background and subtle border.
     */
    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        return panel;
    }
}
