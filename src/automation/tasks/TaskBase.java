package automation.tasks;

import automation.helpers.FileChooserHelper;
import automation.ui.ProgressUI;

public abstract class TaskBase implements AutomationTask {
    protected void handleError(ProgressUI progressUI, Exception e) {
        String errorMsg = "❌ " + getName() + " failed: " + e.getMessage();
        progressUI.updateStepProgress(100, errorMsg);
        FileChooserHelper.showErrorDialog(errorMsg);
    }

    protected String getFile(ProgressUI progressUI, String fileType) {
        return FileChooserHelper.showFileChopper(progressUI, fileType);
    }

    protected String getDirectory(ProgressUI progressUI, String dirType) {
        return FileChooserHelper.showDirectoryChooser(progressUI, dirType);
    }

    protected void initializeProgress(ProgressUI progressUI, int maxSteps) {
        progressUI.showProgress(getName(), "Initializing...");
        progressUI.setMainProgressMax(maxSteps);
        progressUI.setStepProgressMax(100);
    }
    
    protected void completeAndHide(ProgressUI progressUI, String message) {
        progressUI.completeAndHide(message);
    }

    protected void errorAndHide(ProgressUI progressUI, Exception e) {
        String errorMsg = "❌ " + getName() + " failed: " + e.getMessage();
        progressUI.updateStepProgress(100, errorMsg);
        progressUI.updateStatus(errorMsg);
        FileChooserHelper.showErrorDialog(errorMsg);
        
        new javax.swing.Timer(1500, evt -> {
            progressUI.setVisible(false);
            progressUI.resetProgress();
            ((javax.swing.Timer)evt.getSource()).stop();
        }).start();
    }

    protected void cancelAndHide(ProgressUI progressUI) {
        progressUI.showCancellation();
        
        new javax.swing.Timer(500, e -> {
            progressUI.setVisible(false);
            progressUI.resetProgress();
            ((javax.swing.Timer)e.getSource()).stop();
        }).start();
    }
}