package automation.ui;

import javax.swing.*;

public class ProgressUI extends AutomationUI {
    private JDialog progressDialog;
    private JProgressBar mainProgressBar;
    private JProgressBar stepProgressBar;
    private JLabel statusLabel;

    public void showProgress(String title, String initialMessage) {
        progressDialog = createBaseDialog(title, 400, 180);
        
        JPanel panel = createVerticalPanel();
        panel.add(new JLabel(title));
        
        mainProgressBar = new JProgressBar(0, 100);
        stepProgressBar = new JProgressBar(0, 100);
        
        panel.add(mainProgressBar);
        panel.add(stepProgressBar);
        
        statusLabel = new JLabel(initialMessage);
        panel.add(statusLabel);
        
        progressDialog.setContentPane(panel);
        progressDialog.setVisible(true);
    }

    public void updateProgress(int mainValue, int stepValue, String message) {
        if (mainProgressBar != null) mainProgressBar.setValue(mainValue);
        if (stepProgressBar != null) stepProgressBar.setValue(stepValue);
        if (statusLabel != null) statusLabel.setText(message);
    }

    public void close() {
        if (progressDialog != null) {
            progressDialog.dispose();
        }
    }
}