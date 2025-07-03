package automation.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class AutomationUI {
    // Theme constants - Updated with more comprehensive defaults
    public static final Color TITLE_BAR_COLOR = new Color(50, 64, 65); // Dark teal
    public static final Color DIALOG_BG = new Color(50, 64, 65, 220); // Translucent version
    public static final Color PRIMARY_COLOR = new Color(0, 158, 153);
    public static final Color TEXT_COLOR = Color.WHITE; // Default to white
    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);
    
    private static File lastDirectory = new File(System.getProperty("user.dir"));

    static {
        // Set global UI defaults
        UIManager.put("Panel.background", DIALOG_BG);
        UIManager.put("Label.foreground", TEXT_COLOR);
        UIManager.put("Button.background", PRIMARY_COLOR);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", BUTTON_FONT);
        UIManager.put("ProgressBar.foreground", PRIMARY_COLOR);
    }

    // Enhanced dialog creation with automatic styling
    public static JDialog createStyledDialog(String title, int width, int height) {
        JDialog dialog = new JDialog();
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0)); // Transparent background
        
        // Main container with translucent background
        JPanel bgPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(DIALOG_BG);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bgPanel.setOpaque(false);
        
        // Add title bar (now always included)
        bgPanel.add(createTitleBar(title, dialog), BorderLayout.NORTH);
        
        // Content panel with automatic styling
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        bgPanel.add(contentPanel, BorderLayout.CENTER);
        
        dialog.setContentPane(bgPanel);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(null);
        
        return dialog;
    }

    // Title bar factory - now always consistent
    private static JPanel createTitleBar(String fullTitle, Window window) {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(TITLE_BAR_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Split title into parts if it contains " - "
        String[] titleParts = fullTitle.split(" \\| ", 2);
        
        // Create a panel for the title components
        JPanel titleContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleContent.setOpaque(false);
        
        // First part (white)
        JLabel part1 = new JLabel(titleParts[0]);
        part1.setFont(TITLE_FONT);
        part1.setForeground(Color.WHITE);
        
        // Add first part
        titleContent.add(part1);
        
        // If there's a second part, add it with primary color
        if (titleParts.length > 1) {
            JLabel separator = new JLabel(" | ");
            separator.setFont(TITLE_FONT);
            separator.setForeground(Color.WHITE);
            titleContent.add(separator);
            
            JLabel part2 = new JLabel(titleParts[1]);
            part2.setFont(TITLE_FONT);
            part2.setForeground(PRIMARY_COLOR);
            titleContent.add(part2);
        }
        
        // Drag functionality
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
        
        titlePanel.add(titleContent, BorderLayout.CENTER);
        
        // Close button
        JLabel closeLabel = new JLabel("×");
        closeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        closeLabel.setForeground(TEXT_COLOR);
        closeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        closeLabel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                closeLabel.setForeground(new Color(255, 100, 100));
            }
            public void mouseExited(MouseEvent e) {
                closeLabel.setForeground(TEXT_COLOR);
            }
            public void mouseClicked(MouseEvent e) {
                window.dispose();
            }
        });
        
        titlePanel.add(closeLabel, BorderLayout.EAST);
        return titlePanel;
    }

    // Updated component factories with automatic styling
    public static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_FONT);
        label.setForeground(TEXT_COLOR); // Now automatically white
        label.setOpaque(false);
        return label;
    }

    public static JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setOpaque(true);
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFont(BUTTON_FONT);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        return button;
    }
    
    public static void showMessageDialog(Component parent, String message, String title, int messageType) {
        JDialog dialog = createStyledDialog(title, 400, 200);
        JPanel content = (JPanel)((JPanel)dialog.getContentPane()).getComponent(1);
        
        // Message label (automatically gets white text from createLabel)
        JLabel messageLabel = createLabel(message);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(messageLabel);
        
        // OK button
        JButton okButton = createButton("OK");
        okButton.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        buttonPanel.add(okButton);
        
        content.add(buttonPanel);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    public static String showInputDialog(Component parent, String message, String title) {
        JDialog dialog = createStyledDialog(title, 400, 200);
        JPanel content = (JPanel)((JPanel)dialog.getContentPane()).getComponent(1);
        
        final String[] result = { null };
        
        // Message label
        content.add(createLabel(message));
        content.add(Box.createVerticalStrut(10));
        
        // Input field
        JTextField textField = new JTextField(20);
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); // Fixed height
        textField.setPreferredSize(new Dimension(300, 30));
        textField.setFont(BODY_FONT);
        textField.setOpaque(true);
        textField.setBackground(new Color(70, 90, 90));
        textField.setForeground(TEXT_COLOR);
        content.add(textField);
        
        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        JButton okButton = createButton("OK");
        okButton.addActionListener(e -> {
            result[0] = textField.getText();
            dialog.dispose();
        });
        
        JButton cancelButton = createButton("Cancel");
        cancelButton.setBackground(new Color(100, 100, 100));
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(cancelButton);
        content.add(buttonPanel);
        
        dialog.setModal(true);
        dialog.setVisible(true);
        
        return result[0];
    }
    
    public static JFrame createMainFrame(String title, int width, int height) {
        JFrame frame = new JFrame(title);
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0)); // Transparent background
        
        // Create background panel with same translucent effect as dialogs
        JPanel bgPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(DIALOG_BG); // Using same translucent background as dialogs
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bgPanel.setOpaque(false);
        
        // Add title bar (same as dialogs)
        bgPanel.add(createTitleBar(title, frame), BorderLayout.NORTH);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(bgPanel);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        
        return frame;
    }

    public static String showDirectoryChooser(Component parent, String title) {
        // Initialize lastDirectory
        if (lastDirectory == null) {
            lastDirectory = new File(System.getProperty("user.dir"));
        }
        if (!lastDirectory.exists() || !lastDirectory.canRead()) {
            lastDirectory = new File(System.getProperty("user.dir"));
        }

        // Create dialog with custom UI
        JDialog dialog = createStyledDialog(title, 600, 400);
        JPanel content = (JPanel)((JPanel)dialog.getContentPane()).getComponent(1);
        content.setLayout(new BorderLayout());

        // Create file chooser without custom approveSelection
        JFileChooser chooser = new JFileChooser(lastDirectory) {
            protected JDialog createDialog(Component parent) throws HeadlessException {
                JDialog d = super.createDialog(parent);
                d.setUndecorated(false); // Keep native decorations for better behavior
                return d;
            }
        };
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setControlButtonsAreShown(false); // Hide default approve/cancel buttons

        // Add chooser to dialog
        content.add(chooser, BorderLayout.CENTER);

        // Create custom button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JButton submitButton = createButton("Select Folder");
        JButton cancelButton = createButton("Cancel");
        cancelButton.setBackground(new Color(100, 100, 100));

        final String[] result = { null };

        submitButton.addActionListener(e -> {
            File selected = chooser.getSelectedFile();
            if (selected != null && selected.isDirectory()) {
                lastDirectory = selected;
                result[0] = selected.getAbsolutePath();
                dialog.dispose();
            } else {
                showMessageDialog(dialog, 
                    "Please select a valid directory", 
                    "Invalid Selection", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(submitButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(cancelButton);
        content.add(buttonPanel, BorderLayout.SOUTH);

        // Apply UI styling
        try {
            UIManager.put("FileChooser.background", DIALOG_BG);
            UIManager.put("FileChooser.foreground", TEXT_COLOR);
            UIManager.put("FileChooser.font", BODY_FONT);
            SwingUtilities.updateComponentTreeUI(chooser);
        } catch (Exception e) {
            System.err.println("Error styling file chooser: " + e.getMessage());
        }

        dialog.setModal(true);
        dialog.setVisible(true);

        return result[0];
    }

    // Standard dialogs will now automatically inherit all styling
    public static int showOptionDialog(Component parent, String message, String title, String[] options) {
        JDialog dialog = createStyledDialog(title, 400, 200);
        JPanel content = (JPanel)((JPanel)dialog.getContentPane()).getComponent(1);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        JLabel messageLabel = createLabel(message);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        //content.add(Box.createVerticalGlue()); // Add flexible space
        content.add(messageLabel);
        //content.add(Box.createVerticalGlue());
        
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
        dialog.setModal(true);
        dialog.setVisible(true);
        
        Object result = dialog.getRootPane().getClientProperty("option");
        return result != null ? (int) result : JOptionPane.CLOSED_OPTION;
    }
    
    public static String showFileChooser(Component parent, String title) {
        JDialog dialog = createStyledDialog(title, 600, 400);
        JPanel content = (JPanel)((JPanel)dialog.getContentPane()).getComponent(1);
        content.setLayout(new BorderLayout());

        JFileChooser chooser = new JFileChooser(lastDirectory) {
            protected JDialog createDialog(Component parent) throws HeadlessException {
                JDialog d = super.createDialog(parent);
                d.setUndecorated(false);
                return d;
            }
        };
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setControlButtonsAreShown(false);

        content.add(chooser, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JButton submitButton = createButton("Select File");
        JButton cancelButton = createButton("Cancel");
        cancelButton.setBackground(new Color(100, 100, 100));

        final String[] result = { null };

        submitButton.addActionListener(e -> {
            File selected = chooser.getSelectedFile();
            if (selected != null && selected.isFile()) {
                lastDirectory = selected.getParentFile();
                result[0] = selected.getAbsolutePath();
                dialog.dispose();
            } else {
                showMessageDialog(dialog, 
                    "Please select a valid file", 
                    "Invalid Selection", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(submitButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(cancelButton);
        content.add(buttonPanel, BorderLayout.SOUTH);

        try {
            UIManager.put("FileChooser.background", DIALOG_BG);
            UIManager.put("FileChooser.foreground", TEXT_COLOR);
            UIManager.put("FileChooser.font", BODY_FONT);
            SwingUtilities.updateComponentTreeUI(chooser);
        } catch (Exception e) {
            System.err.println("Error styling file chooser: " + e.getMessage());
        }

        dialog.setModal(true);
        dialog.setVisible(true);

        return result[0];
    }
}