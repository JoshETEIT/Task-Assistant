package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import automation.ui.AutomationUI;
import automation.ui.ProgressUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class PageLoadTimeTask implements AutomationTask {
    
    private static JDialog dialog;
    private static JTextArea history;
    private static WebDriver activeDriver;
    private static boolean waitingForClick = false;
    private static long startTime;
    private static Robot robot;
    
    @Override
    public String getName() { return "Page Load Timer"; }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        activeDriver = driver;
        
        // Close the ProgressUI since we're opening our own dialog
        if (progressUI != null && progressUI.isShowing()) {
            progressUI.close();
        }
        
        try {
            robot = new Robot();
        } catch (AWTException e) {
            JOptionPane.showMessageDialog(null, "Could not create mouse listener: " + e.getMessage());
            return;
        }
        
        if (dialog == null) setupDialog();
        dialog.setVisible(true);
    }
    
    private void setupDialog() {
        dialog = AutomationUI.createStyledDialog("Click Timer", 450, 350);
        JPanel content = (JPanel)((JPanel)dialog.getContentPane()).getComponent(1);
        content.setLayout(new BorderLayout());
        
        JLabel titleLabel = AutomationUI.createLabel("Click-to-Start Timer");
        titleLabel.setFont(AutomationUI.TITLE_FONT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(titleLabel, BorderLayout.NORTH);
        
        // Instructions
        JLabel instructions = AutomationUI.createLabel(
            "<html><center>Click 'Arm Timer' then click ANYWHERE on screen to start timing<br>" +
            "Timer stops automatically when page loads</center></html>");
        instructions.setHorizontalAlignment(SwingConstants.CENTER);
        instructions.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(instructions, BorderLayout.NORTH);
        
        // Create a transparent text area
        history = new JTextArea() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isOpaque() {
                return false;
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(getForeground());
                g2d.setFont(getFont());
                super.paintComponent(g2d);
                g2d.dispose();
            }
        };
        
        history.setEditable(false);
        history.setFont(AutomationUI.BODY_FONT);
        history.setForeground(Color.WHITE);
        history.setOpaque(false);
        history.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.setBorder(BorderFactory.createEmptyBorder());
        textPanel.add(history, BorderLayout.CENTER);
        
        JScrollPane scrollPane = new JScrollPane(textPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        
        // Custom scroll bar styling (unchanged)
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = AutomationUI.PRIMARY_COLOR;
                this.trackColor = new Color(60, 80, 80);
                this.thumbDarkShadowColor = AutomationUI.PRIMARY_COLOR.darker();
                this.thumbHighlightColor = AutomationUI.PRIMARY_COLOR.brighter();
                this.thumbLightShadowColor = AutomationUI.PRIMARY_COLOR;
            }
            
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createInvisibleButton();
            }
            
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createInvisibleButton();
            }
            
            private JButton createInvisibleButton() {
                JButton button = new JButton();
                button.setOpaque(false);
                button.setFocusable(false);
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setBackground(new Color(0, 0, 0, 0));
                return button;
            }
            
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(trackColor);
                g2.fillRoundRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height, 5, 5);
            }
            
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y, thumbBounds.width - 4, thumbBounds.height, 5, 5);
            }
        });
        verticalScrollBar.setUnitIncrement(16);
        verticalScrollBar.setPreferredSize(new Dimension(10, Integer.MAX_VALUE));
        
        content.add(scrollPane, BorderLayout.CENTER);
        
        JButton armBtn = AutomationUI.createButton("Arm Timer");
        armBtn.addActionListener(e -> armTimer());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(armBtn);
        content.add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void armTimer() {
        if (waitingForClick) {
            history.append("[Timer already armed]\n");
            return;
        }
        
        waitingForClick = true;
        history.append("[Timer ARMED - Press F2 to start timing...]\n");
        history.setCaretPosition(history.getText().length());
        
        // Use a keyboard listener instead - much more reliable
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(e -> {
                if (waitingForClick && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_F2) {
                    startTime = System.currentTimeMillis();
                    waitingForClick = false;
                    history.append("[Timer STARTED - waiting for page load...]\n");
                    history.setCaretPosition(history.getText().length());
                    
                    // Start monitoring page load
                    monitorPageLoad();
                    return true; // consume the event
                }
                return false;
            });
        
        // Auto-cancel after 30 seconds
        new javax.swing.Timer(30000, e -> {
            if (waitingForClick) {
                waitingForClick = false;
                history.append("[Timer auto-disarmed after 30 seconds]\n");
                history.setCaretPosition(history.getText().length());
            }
        }).start();
    }
    
    private void monitorPageLoad() {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() {
                try {
                    // Wait for page to finish loading
                    new WebDriverWait(activeDriver, Duration.ofSeconds(30))
                        .until(d -> ((JavascriptExecutor)d)
                            .executeScript("return document.readyState").equals("complete"));
                    
                    long endTime = System.currentTimeMillis();
                    double loadTime = (endTime - startTime) / 1000.0;
                    
                    // Record the result
                    String timestamp = String.format("[%tT]", new Date());
                    String entry = String.format("%s %.2f seconds\n", timestamp, loadTime);
                    history.append(entry);
                    
                } catch (Exception e) {
                    history.append("[Page load timeout or error: " + e.getMessage() + "]\n");
                }
                return null;
            }
            
            protected void done() {
                history.setCaretPosition(history.getText().length());
            }
        }.execute();
    }
}