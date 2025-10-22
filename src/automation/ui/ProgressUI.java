package automation.ui;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProgressUI {
    private JDialog progressDialog;
    private JProgressBar mainProgressBar;
    private JProgressBar stepProgressBar;
    private JLabel statusLabel;
    private javax.swing.Timer closeTimer;
    private static int instanceCounter = 0;
    private final int instanceId;

    public ProgressUI() {
        this.instanceId = ++instanceCounter;
        System.out.println("ProgressUI CONSTRUCTOR - Instance #" + instanceId + " created");
    }

    public void showProgress(String title, String initialMessage) {
        // CANCEL any pending close timer
        if (closeTimer != null && closeTimer.isRunning()) {
            closeTimer.stop();
            closeTimer = null;
        }
        
        // CLOSE existing dialog first
        close();
        
        // Create debug title
        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        String debugTitle = title + " | Instance:" + instanceId + " | Created:" + timestamp;
        
        System.out.println("Creating ProgressUI: " + debugTitle);
        
        progressDialog = AutomationUI.createStyledDialog(debugTitle, 400, 200);
        progressDialog.setModal(false);
        progressDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        JPanel content = (JPanel)((JPanel)progressDialog.getContentPane()).getComponent(1);
        
        statusLabel = AutomationUI.createLabel(initialMessage);
        content.add(statusLabel);
        
        mainProgressBar = new JProgressBar(0, 100);
        mainProgressBar.setStringPainted(true);
        content.add(mainProgressBar);
        
        stepProgressBar = new JProgressBar(0, 100);
        stepProgressBar.setStringPainted(true);
        content.add(stepProgressBar);
        
        // Add window listener to track disposal
        progressDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                System.out.println("ProgressUI CLOSED - Instance: " + instanceId);
                cleanup();
            }
        });
        
        progressDialog.setVisible(true);
        System.out.println("ProgressUI VISIBLE - Instance: " + instanceId);
    }

    public void setMainProgressMax(int max) {
        if (mainProgressBar != null) {
            mainProgressBar.setMaximum(max);
        }
    }

    public void setStepProgressMax(int max) {
        if (stepProgressBar != null) {
            stepProgressBar.setMaximum(max);
        }
    }

    public void updateMainProgress(int value) {
        if (mainProgressBar != null) {
            mainProgressBar.setValue(value);
            mainProgressBar.setString(value + "/" + mainProgressBar.getMaximum());
        }
    }

    public void updateStepProgress(int value, String message) {
        if (stepProgressBar != null) {
            stepProgressBar.setValue(value);
            stepProgressBar.setString(message);
        }
    }

    public void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
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
        // Cancel any pending timer
        if (closeTimer != null && closeTimer.isRunning()) {
            closeTimer.stop();
        }
        
        updateStepProgress(100, "⏹ Task cancelled");
        updateStatus("Operation cancelled by user");
        
        closeTimer = new javax.swing.Timer(1000, e -> {
            System.out.println("Auto-closing CANCELLED ProgressUI - Instance: " + instanceId);
            close();
            ((javax.swing.Timer)e.getSource()).stop();
        });
        closeTimer.setRepeats(false);
        closeTimer.start();
    }
    
    public void completeAndHide(String message) {
        // Cancel any pending timer
        if (closeTimer != null && closeTimer.isRunning()) {
            closeTimer.stop();
        }
        
        updateStepProgress(100, "✅ " + message);
        updateStatus(message);
        
        closeTimer = new javax.swing.Timer(1000, e -> {
            System.out.println("Auto-closing COMPLETED ProgressUI - Instance: " + instanceId);
            close();
            ((javax.swing.Timer)e.getSource()).stop();
        });
        closeTimer.setRepeats(false);
        closeTimer.start();
    }

    public void close() {
        System.out.println("Manual close() called - Instance: " + instanceId);
        
        // Cancel any pending timer
        if (closeTimer != null && closeTimer.isRunning()) {
            closeTimer.stop();
            closeTimer = null;
        }
        
        if (progressDialog != null) {
            progressDialog.dispose();
            cleanup();
        }
    }
    
    private void cleanup() {
        System.out.println("Cleanup called - Instance: " + instanceId);
        progressDialog = null;
        mainProgressBar = null;
        stepProgressBar = null;
        statusLabel = null;
        closeTimer = null;
    }
    
    public boolean isShowing() {
        return progressDialog != null && progressDialog.isVisible();
    }
    
    // Debug method
    public void debugState() {
        System.out.println("=== ProgressUI State ===");
        System.out.println("Instance: " + instanceId);
        System.out.println("Dialog exists: " + (progressDialog != null));
        System.out.println("Dialog visible: " + (progressDialog != null && progressDialog.isVisible()));
        System.out.println("Timer running: " + (closeTimer != null && closeTimer.isRunning()));
        System.out.println("========================");
    }
}