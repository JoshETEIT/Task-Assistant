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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.time.Duration;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class PageLoadTimeTask implements AutomationTask {
    
    private static JDialog dialog;
    private static JTextArea history;
    private static WebDriver activeDriver;
    
    @Override
    public String getName() { return "Page Load Timer"; }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        activeDriver = driver;
        if (dialog == null) setupDialog();
        dialog.setVisible(true);
        progressUI.setVisible(false);
    }
    
    private void setupDialog() {
        dialog = AutomationUI.createStyledDialog("Page Load Timer", 450, 350);
        JPanel content = (JPanel)((JPanel)dialog.getContentPane()).getComponent(1);
        content.setLayout(new BorderLayout());
        
        JLabel titleLabel = AutomationUI.createLabel("Page Load Measurements");
        titleLabel.setFont(AutomationUI.TITLE_FONT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(titleLabel, BorderLayout.NORTH);
        
        // Create a transparent text area
        history = new JTextArea() {
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isOpaque() {
                return false; // Make the component non-opaque
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                // Don't paint the background
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(getForeground());
                g2d.setFont(getFont());
                
                // Paint the text only
                super.paintComponent(g2d);
                g2d.dispose();
            }
        };
        
        history.setEditable(false);
        history.setFont(AutomationUI.BODY_FONT);
        history.setForeground(Color.WHITE); // Keep text white
        history.setOpaque(false); // Make transparent
        history.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Put the text area in a transparent panel to ensure proper background
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
        
        JButton measureBtn = AutomationUI.createButton("Measure Load Time");
        measureBtn.addActionListener(e -> measureLoadTime());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(measureBtn);
        content.add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void measureLoadTime() {
        new SwingWorker<Void, Void>() {
            long startTime;
            double loadTime;
            
            protected Void doInBackground() {
                startTime = System.currentTimeMillis();
                activeDriver.navigate().refresh();
                
                new WebDriverWait(activeDriver, Duration.ofSeconds(30))
                    .until(d -> ((JavascriptExecutor)d)
                        .executeScript("return document.readyState").equals("complete"));
                
                loadTime = (System.currentTimeMillis() - startTime) / 1000.0;
                return null;
            }
            
            protected void done() {
                String timestamp = String.format("[%tT]", new Date());
                String entry = String.format("%s %.2f seconds\n", timestamp, loadTime);
                history.append(entry);
                history.setCaretPosition(history.getText().length());
            }
        }.execute();
    }
}