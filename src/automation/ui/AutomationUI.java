package automation.ui;

import javax.swing.*;

import org.openqa.selenium.WebElement;
import automation.TestSuite;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutomationUI {
    // Theme constants
    public static final Color TITLE_BAR_COLOR = new Color(50, 64, 65);
    public static final Color DIALOG_BG = new Color(50, 64, 65, 220);
    public static final Color PRIMARY_COLOR = new Color(0, 158, 153);
    public static final Color TEXT_COLOR = Color.WHITE;
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

    private static JPanel createTitleBar(String fullTitle, Window window) {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(TITLE_BAR_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        final Point[] offset = new Point[1];
        titlePanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                offset[0] = new Point(e.getPoint());
            }
        });
        titlePanel.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (offset[0] != null) {
                    Point newLoc = e.getLocationOnScreen();
                    newLoc.translate(-offset[0].x, -offset[0].y);
                    window.setLocation(newLoc);
                }
            }
        });
        
        String[] titleParts = fullTitle.split(" \\| ", 2);
        JPanel titleContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleContent.setOpaque(false);
        
        JLabel part1 = new JLabel(titleParts[0]);
        part1.setFont(TITLE_FONT);
        part1.setForeground(Color.WHITE);
        titleContent.add(part1);
        
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
        
        titlePanel.add(titleContent, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);
        
        MouseAdapter preventDrag = new MouseAdapter() {};
        buttonPanel.addMouseListener(preventDrag);
        buttonPanel.addMouseMotionListener(preventDrag);
        
        if (!fullTitle.contains("Select Action")) {
            JLabel homeLabel = new JLabel("⌂");
            homeLabel.setFont(new Font("Arial", Font.BOLD, 18));
            homeLabel.setForeground(TEXT_COLOR);
            homeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            homeLabel.setToolTipText("Return to main menu");
            homeLabel.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    homeLabel.setForeground(PRIMARY_COLOR);
                }
                public void mouseExited(MouseEvent e) {
                    homeLabel.setForeground(TEXT_COLOR);
                }
                public void mouseClicked(MouseEvent e) {
                    window.dispose();
                    TestSuite.startApplication();
                }
            });
            buttonPanel.add(homeLabel);
        }
        
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
        
        buttonPanel.add(closeLabel);
        titlePanel.add(buttonPanel, BorderLayout.EAST);
        return titlePanel;
    }

    @SuppressWarnings("serial")
    public static JDialog createStyledDialog(String title, int width, int height) {
        JDialog dialog = new JDialog();
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        
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
        bgPanel.add(createTitleBar(title, dialog), BorderLayout.NORTH);
        
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
        button.setForeground(TEXT_COLOR);
        button.setFont(BUTTON_FONT);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        return button;
    }
    
    public static void showMessageDialog(Component parent, String message, String title, int messageType) {
        JDialog dialog = createStyledDialog(title, 400, 200);
        JPanel content = (JPanel)((JPanel)dialog.getContentPane()).getComponent(1);
        
        JLabel messageLabel = createLabel(message);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(messageLabel);
        
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
        
        content.add(createLabel(message));
        content.add(Box.createVerticalStrut(10));
        
        JTextField textField = new JTextField(20);
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        textField.setPreferredSize(new Dimension(300, 30));
        textField.setFont(BODY_FONT);
        textField.setOpaque(true);
        textField.setBackground(new Color(70, 90, 90));
        textField.setForeground(TEXT_COLOR);
        content.add(textField);
        
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
    
    @SuppressWarnings("serial")
    public static JFrame createMainFrame(String title, int width, int height) {
        JFrame frame = new JFrame(title);
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0));
        
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
        bgPanel.add(createTitleBar(title, frame), BorderLayout.NORTH);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(bgPanel);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        
        return frame;
    }

    @SuppressWarnings("serial")
    public static String showDirectoryChooser(Component parent, String title) {
        if (lastDirectory == null || !lastDirectory.exists() || !lastDirectory.canRead()) {
            lastDirectory = new File(System.getProperty("user.dir"));
        }

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
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setControlButtonsAreShown(false);

        content.add(chooser, BorderLayout.CENTER);

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

    public static int showOptionDialog(Component parent, String message, String title, String[] options) {
        // Fixed width and MANUAL height setting
        final int DIALOG_WIDTH = 400;
        final int DIALOG_HEIGHT = 275; // <-- CHANGE THIS VALUE AS NEEDED
        
        JDialog dialog = createStyledDialog(title, DIALOG_WIDTH, DIALOG_HEIGHT);
        JPanel content = (JPanel)((JPanel)dialog.getContentPane()).getComponent(1);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        // Add message
        JLabel messageLabel = createLabel(message);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(messageLabel);
        content.add(Box.createVerticalStrut(10));
        
        // Create panel with original flow layout
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        optionsPanel.setOpaque(false);
        
        // Add all buttons
        for (int i = 0; i < options.length; i++) {
            JButton button = createButton(options[i]);
            final int index = i;
            button.addActionListener(e -> {
                dialog.dispose();
                dialog.getRootPane().putClientProperty("option", index);
            });
            optionsPanel.add(button);
        }
        
        content.add(optionsPanel);
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setVisible(true);
        
        Object result = dialog.getRootPane().getClientProperty("option");
        return result != null ? (int) result : JOptionPane.CLOSED_OPTION;
    }
    
    public static int[] showMultiOptionDialog(Component parent, String message, String title, List<WebElement> headers) {
        List<String> headerTitles = new ArrayList<String>();
        List<Boolean> isGroupHeader = new ArrayList<Boolean>();
        List<Integer> groupIds = new ArrayList<Integer>();
        int currentGroup = -1;
        
        for (WebElement header : headers) {
            String text = header.getText().trim();
            
            if (text.isEmpty() || text.equals("Drawing Template")) {
                continue;
            }
            
            if (text.contains("Main organisation")) {
                continue;
            }
            
            boolean isGroup = text.endsWith("Templates") && 
                             "header2".equals(header.getDomAttribute("class"));
            
            if (isGroup) {
                currentGroup++;
            }
            
            headerTitles.add(text);
            isGroupHeader.add(isGroup);
            groupIds.add(isGroup ? -1 : currentGroup);
        }

        JDialog dialog = createStyledDialog(title, 550, 450);
        JPanel content = (JPanel)((JPanel)dialog.getContentPane()).getComponent(1);
        content.setLayout(new BorderLayout());
        
        content.add(createLabel(message), BorderLayout.NORTH);

        JList<String> optionList = new JList<String>(headerTitles.toArray(new String[0])) {
            private static final long serialVersionUID = 1L;
            private int anchorIndex = -1;
            
            private void handleGroupHeaderClick(int groupIndex) {
                int start = groupIndex + 1;
                int end = start;
                while (end < getModel().getSize() && !isGroupHeader.get(end)) {
                    end++;
                }
                end--;
                
                if (start > end) return;
                
                boolean allSelected = true;
                for (int i = start; i <= end; i++) {
                    if (!isSelectedIndex(i)) {
                        allSelected = false;
                        break;
                    }
                }
                
                if (allSelected) {
                    removeSelectionInterval(start, end);
                } else {
                    addSelectionInterval(start, end);
                }
            }
            
            @Override
            protected void processMouseEvent(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    int index = locationToIndex(e.getPoint());
                    if (index >= 0) {
                        if (isGroupHeader.get(index)) {
                            handleGroupHeaderClick(index);
                            anchorIndex = index;
                        } 
                        else if (e.isShiftDown() && anchorIndex != -1) {
                            selectRangeInGroup(anchorIndex, index);
                        }
                        else {
                            if (isSelectedIndex(index)) {
                                removeSelectionInterval(index, index);
                            } else {
                                addSelectionInterval(index, index);
                            }
                            anchorIndex = index;
                        }
                    }
                    return;
                }
                super.processMouseEvent(e);
            }
            
            private void selectRangeInGroup(int from, int to) {
                int groupId = groupIds.get(from);
                int start = Math.min(from, to);
                int end = Math.max(from, to);
                
                for (int i = start; i <= end; i++) {
                    if (groupIds.get(i) != groupId) {
                        return;
                    }
                }
                
                for (int i = start; i <= end; i++) {
                    addSelectionInterval(i, i);
                }
            }
        };
        
        optionList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        optionList.setFont(BODY_FONT);
        optionList.setBackground(new Color(70, 90, 90));
        optionList.setForeground(TEXT_COLOR);
        optionList.setSelectionBackground(PRIMARY_COLOR);
        optionList.setSelectionForeground(Color.WHITE);
        optionList.setFixedCellHeight(30);
        
        optionList.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                
                if (isGroupHeader.get(index)) {
                    label.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    label.setForeground(new Color(180, 180, 180));
                    label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                } else {
                    label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    label.setBorder(BorderFactory.createEmptyBorder(2, 25, 2, 5));
                }
                
                if (isSelected) {
                    label.setBackground(PRIMARY_COLOR);
                    label.setForeground(Color.WHITE);
                    label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, PRIMARY_COLOR.brighter()),
                        isGroupHeader.get(index) ? 
                            BorderFactory.createEmptyBorder(5, 5, 5, 5) :
                            BorderFactory.createEmptyBorder(2, 25, 2, 5)
                    ));
                }
                return label;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(optionList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        content.add(scrollPane, BorderLayout.CENTER);
        
        JLabel instructions = createLabel(
            "<html>• Click items to toggle selection<br>" +
            "• Click group names to select all in group<br>" +
            "• Shift+Click for range selection within group</html>");
        instructions.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        instructions.setForeground(new Color(180, 180, 180));
        content.add(instructions, BorderLayout.SOUTH);
        
        final int[][] result = { null };
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        JButton okButton = createButton("OK");
        okButton.addActionListener(e -> {
            result[0] = optionList.getSelectedIndices();
            dialog.dispose();
        });
        
        JButton cancelButton = createButton("Cancel");
        cancelButton.setBackground(new Color(100, 100, 100));
        cancelButton.addActionListener(e -> {
            result[0] = new int[0];
            dialog.dispose();
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(cancelButton);
        content.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setModal(true);
        dialog.setVisible(true);
        
        return result[0] != null ? 
                Arrays.stream(result[0])
                    .filter(idx -> !isGroupHeader.get(idx))
                    .toArray() 
                : new int[0];
        }
    
    @SuppressWarnings("serial")
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