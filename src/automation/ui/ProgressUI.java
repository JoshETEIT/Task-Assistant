package automation.ui;

import javax.swing.*;

public class ProgressUI {
    private JDialog progressDialog;
    private JProgressBar mainProgressBar;
    private JProgressBar stepProgressBar;
    private JLabel statusLabel;

    public void showProgress(String title, String initialMessage) {
        progressDialog = AutomationUI.createStyledDialog(title, 400, 200);
        JPanel content = (JPanel)((JPanel)progressDialog.getContentPane()).getComponent(1);
        
        statusLabel = AutomationUI.createLabel(initialMessage);
        content.add(statusLabel);
        
        mainProgressBar = new JProgressBar(0, 100);
        mainProgressBar.setStringPainted(true);
        content.add(mainProgressBar);
        
        stepProgressBar = new JProgressBar(0, 100);
        stepProgressBar.setStringPainted(true);
        content.add(stepProgressBar);
        
        progressDialog.setVisible(true);
    }

    public void setMainProgressMax(int max) {
        mainProgressBar.setMaximum(max);
    }

    public void setStepProgressMax(int max) {
        stepProgressBar.setMaximum(max);
    }

    public void updateMainProgress(int value) {
        mainProgressBar.setValue(value);
        mainProgressBar.setString(value + "/" + mainProgressBar.getMaximum());
    }

    public void updateStepProgress(int value, String message) {
        stepProgressBar.setValue(value);
        stepProgressBar.setString(message);
    }

    public void updateStatus(String message) {
        statusLabel.setText(message);
    }

    public void updateDualProgress(int mainValue, int stepValue, String status) {
        updateMainProgress(mainValue);
        updateStepProgress(stepValue, status);
        updateStatus(status);
    }
    
    public void startTask(String taskName) {
        showProgress(taskName, "Starting...");
        resetProgress();
    }

    public void resetProgress() {
        setMainProgressMax(1);
        setStepProgressMax(100);
        updateMainProgress(0);
        updateStepProgress(0, "");
    }

    public void showCancellation() {
        updateStepProgress(100, "⏹ Task cancelled");
        updateStatus("Operation cancelled by user");
    }
    
    public void completeAndHide(String message) {
        updateStepProgress(100, "✅ " + message);
        updateStatus(message);
        
        new javax.swing.Timer(1000, e -> {
            setVisible(false);
            resetProgress();
            ((javax.swing.Timer)e.getSource()).stop();
        }).start();
    }
    
    public void setVisible(boolean visible) {
        if (progressDialog != null) {
            progressDialog.setVisible(visible);
        }
    }

    public void close() {
        if (progressDialog != null) {
            progressDialog.dispose();
        }
    }
}