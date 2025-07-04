package automation.ui;

import javax.swing.*;

public class ProgressUI {
    private JDialog progressDialog;
    private JProgressBar mainProgressBar;
    private JProgressBar stepProgressBar;
    private JLabel statusLabel;

    public void showProgress(String title, String initialMessage) {
        // This will now automatically get all styling from AutomationUI
        progressDialog = AutomationUI.createStyledDialog(title, 400, 200);
        JPanel content = (JPanel)((JPanel)progressDialog.getContentPane()).getComponent(1);
        
        // Add components - they'll inherit the correct styling
        statusLabel = AutomationUI.createLabel(initialMessage);
        content.add(statusLabel);
        
        mainProgressBar = new JProgressBar();
        content.add(mainProgressBar);
        
        stepProgressBar = new JProgressBar();
        content.add(stepProgressBar);
        
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