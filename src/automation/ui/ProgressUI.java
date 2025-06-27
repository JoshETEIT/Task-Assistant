package automation.ui;

import java.awt.Component;
import javax.swing.*;

public class ProgressUI {
    private JDialog progressDialog;
    private JProgressBar mainProgressBar;
    private JProgressBar stepProgressBar;
    private JLabel statusLabel;

    public void showProgress(String title, String initialMessage) {
        progressDialog = AutomationUI.createStyledDialog(title, 400, 180);
        JPanel content = AutomationUI.createContentPanel();
        
        JLabel titleLabel = AutomationUI.createLabel(title);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(titleLabel);
        
        content.add(Box.createVerticalStrut(20));
        
        mainProgressBar = new JProgressBar(0, 100);
        mainProgressBar.setStringPainted(true);
        mainProgressBar.setOpaque(true);
        content.add(mainProgressBar);
        
        content.add(Box.createVerticalStrut(10));
        
        stepProgressBar = new JProgressBar(0, 100);
        stepProgressBar.setStringPainted(true);
        stepProgressBar.setOpaque(true);
        content.add(stepProgressBar);
        
        content.add(Box.createVerticalStrut(15));
        
        statusLabel = AutomationUI.createLabel(initialMessage);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(statusLabel);
        
        progressDialog.setContentPane(content);
        progressDialog.setVisible(true);
    }

    public void updateProgress(int mainValue, int stepValue, String message) {
        if (mainProgressBar != null) {
            mainProgressBar.setValue(mainValue);
            mainProgressBar.setString(mainValue + "%");
        }
        if (stepProgressBar != null) {
            stepProgressBar.setValue(stepValue);
            stepProgressBar.setString(stepValue + "%");
        }
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    public void close() {
        if (progressDialog != null) {
            progressDialog.dispose();
        }
    }
}