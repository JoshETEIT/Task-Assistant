package automation.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class AutomationUI {
    // Theme constants
    public static final Color TITLE_BAR_COLOR = new Color(50, 64, 65);
    public static final Color TRANSLUCENT_BG = new Color(200, 200, 200, 50);
    public static final Color PRIMARY_COLOR = new Color(0, 102, 204);
    public static final Color TEXT_COLOR = new Color(50, 50, 50);
    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);

    static {
        UIManager.put("Panel.background", new Color(0, 0, 0, 0));
        UIManager.put("Table.background", new Color(0, 0, 0, 0));
        UIManager.put("TableHeader.background", PRIMARY_COLOR);
        UIManager.put("Viewport.background", new Color(0, 0, 0, 0));
        UIManager.put("Panel.opaque", false);
        UIManager.put("Table.opaque", false);
    }

    // Frame and Dialog creation
    public static JFrame createMainFrame(String title, int width, int height) {
        JFrame frame = new JFrame(title);
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0));
        
        JPanel bgPanel = createBackgroundPanel();
        bgPanel.add(createTitleBar(title, frame), BorderLayout.NORTH);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.setContentPane(bgPanel);
        
        return frame;
    }

    public static JDialog createStyledDialog(String title, int width, int height) {
        JDialog dialog = new JDialog();
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        
        JPanel bgPanel = createBackgroundPanel();
        bgPanel.add(createTitleBar(title, dialog), BorderLayout.NORTH);
        
        dialog.setContentPane(bgPanel);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(null);
        
        return dialog;
    }

    private static JPanel createBackgroundPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(TRANSLUCENT_BG);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    private static JPanel createTitleBar(String title, Window window) {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(TITLE_BAR_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setOpaque(false);
        
        final Point[] offset = new Point[1];
        titlePanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                offset[0] = new Point(e.getPoint());
            }
        });
        titlePanel.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point newLoc = e.getLocationOnScreen();
                newLoc.translate(-offset[0].x, -offset[0].y);
                window.setLocation(newLoc);
            }
        });
        
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        JLabel closeLabel = new JLabel("×");
        closeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        closeLabel.setForeground(Color.WHITE);
        closeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        closeLabel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                closeLabel.setForeground(new Color(255, 100, 100));
            }
            public void mouseExited(MouseEvent e) {
                closeLabel.setForeground(Color.WHITE);
            }
            public void mouseClicked(MouseEvent e) {
                window.dispose();
            }
        });
        
        titlePanel.add(closeLabel, BorderLayout.EAST);
        return titlePanel;
    }

    // Component factories
    public static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_FONT);
        label.setForeground(TEXT_COLOR);
        label.setOpaque(false);
        return label;
    }

    public static JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setOpaque(true);
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFont(BUTTON_FONT);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        return button;
    }

    public static JPanel createContentPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        return panel;
    }

    // Standard dialogs
    public static int showOptionDialog(Component parent, String message, String title, String[] options) {
        JDialog dialog = createStyledDialog(title, 400, 200);
        JPanel content = createContentPanel();
        
        JLabel titleLabel = createLabel(title);
        titleLabel.setFont(TITLE_FONT);
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(10));
        
        JLabel messageLabel = createLabel(message);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(messageLabel);
        content.add(Box.createVerticalStrut(20));
        
        JPanel optionsPanel = new JPanel();
        optionsPanel.setOpaque(false);
        for (int i = 0; i < options.length; i++) {
            JButton button = createButton(options[i]);
            final int index = i;
            button.addActionListener(e -> {
                dialog.dispose();
                dialog.getRootPane().putClientProperty("option", index);
            });
            optionsPanel.add(button);
            if (i < options.length - 1) {
                optionsPanel.add(Box.createHorizontalStrut(10));
            }
        }
        
        content.add(optionsPanel);
        dialog.setContentPane(content);
        dialog.setModal(true);
        dialog.setVisible(true);
        
        Object result = dialog.getRootPane().getClientProperty("option");
        return result != null ? (int) result : JOptionPane.CLOSED_OPTION;
    }

    public static String showInputDialog(Component parent, String message, String title) {
        JDialog dialog = createStyledDialog(title, 400, 200);
        JPanel content = createContentPanel();
        
        content.add(createLabel(message));
        
        JTextField textField = new JTextField(20);
        textField.setFont(BODY_FONT);
        textField.setMaximumSize(new Dimension(300, 30));
        textField.setOpaque(true);
        content.add(Box.createVerticalStrut(10));
        content.add(textField);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        JButton okButton = createButton("OK");
        okButton.addActionListener(e -> dialog.dispose());
        
        JButton cancelButton = createButton("Cancel");
        cancelButton.setBackground(new Color(150, 150, 150));
        cancelButton.addActionListener(e -> {
            textField.setText(null);
            dialog.dispose();
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(cancelButton);
        
        content.add(buttonPanel);
        dialog.setContentPane(content);
        dialog.setModal(true);
        dialog.setVisible(true);
        
        return textField.getText();
    }

    // Updated to include messageType parameter
    public static void showMessageDialog(Component parent, String message, String title, int messageType) {
        JDialog dialog = createStyledDialog(title, 400, 200);
        JPanel content = createContentPanel();
        
        Icon icon = null;
        switch (messageType) {
            case JOptionPane.ERROR_MESSAGE:
                icon = UIManager.getIcon("OptionPane.errorIcon");
                break;
            case JOptionPane.WARNING_MESSAGE:
                icon = UIManager.getIcon("OptionPane.warningIcon");
                break;
            case JOptionPane.INFORMATION_MESSAGE:
                icon = UIManager.getIcon("OptionPane.informationIcon");
                break;
        }
        
        if (icon != null) {
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(iconLabel);
            content.add(Box.createVerticalStrut(10));
        }
        
        content.add(createLabel(message));
        
        JButton okButton = createButton("OK");
        okButton.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        buttonPanel.add(okButton);
        
        content.add(buttonPanel);
        dialog.setContentPane(content);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    // Overloaded method without messageType for backward compatibility
    public static void showMessageDialog(Component parent, String message, String title) {
        showMessageDialog(parent, message, title, JOptionPane.PLAIN_MESSAGE);
    }
}