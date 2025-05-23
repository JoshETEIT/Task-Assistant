package automation.helpers;

import javax.swing.*;
import java.awt.*;

public class ProgressBarHelper {
    private static JProgressBar progressBar;
    private static JProgressBar stepProgressBar;

    public static void initializeProgress(JProgressBar overall, JProgressBar step) {
        progressBar = overall;
        stepProgressBar = step;

        if (progressBar != null) {
            progressBar.setPreferredSize(new Dimension(400, 20));
            progressBar.setOrientation(SwingConstants.HORIZONTAL);
        }
        if (stepProgressBar != null) {
            stepProgressBar.setPreferredSize(new Dimension(400, 20));
            stepProgressBar.setOrientation(SwingConstants.HORIZONTAL);
        }
    }

    public static void updateProgress(String message, int percent) {
        updateAny(progressBar, message, percent, 60);
    }

    public static void updateStepProgress(String message, int percent) {
        updateAny(stepProgressBar, message, percent, 60);
    }

    public static void close() {
        if (progressBar != null) {
            progressBar.setValue(0);
            progressBar.setString("");
        }
        if (stepProgressBar != null) {
            stepProgressBar.setValue(0);
            stepProgressBar.setString("");
        }
    }

    private static void updateAny(JProgressBar bar, String message, int percent, int delay) {
        if (bar == null) return;
        SwingUtilities.invokeLater(() -> {
            bar.setString(message);
            bar.setStringPainted(true);
        });

        new Thread(() -> {
            int current = bar.getValue();
            while (current < percent) {
                current++;
                final int progress = current;
                SwingUtilities.invokeLater(() -> bar.setValue(progress));
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }
}
