package automation.helpers;

import automation.ui.ProgressUI;

public class ProgressTracker {
    private final ProgressUI progressUI;
    private final boolean progressEnabled;

    public ProgressTracker(ProgressUI progressUI, boolean progressEnabled) {
        this.progressUI = progressUI;
        this.progressEnabled = progressEnabled;
    }

    public void updateProgress(int step, String message) {
        if (progressEnabled) {
            progressUI.updateStepProgress(step, message);
        }
    }
}