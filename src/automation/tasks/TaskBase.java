package automation.tasks;

import automation.helpers.FileChooserHelper;
import automation.ui.ProgressUI;

public abstract class TaskBase implements AutomationTask {
    protected void handleError(ProgressUI progressUI, Exception e) {
        String errorMsg = "❌ " + getName() + " failed: " + e.getMessage();
        progressUI.updateStepProgress(100, errorMsg);
        FileChooserHelper.showErrorDialog(errorMsg);
    }

    protected void complete(ProgressUI progressUI, String message) {
        progressUI.updateStepProgress(100, "✅ " + message);
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
}