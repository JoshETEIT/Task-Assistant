package automation.helpers;

import automation.ui.AutomationUI;
import automation.ui.ProgressUI;
import javax.swing.*;

public class FileChooserHelper {
    private static final String DEFAULT_TITLE_PREFIX = "Automation Suite | Select ";

    public static String showFileChopper(ProgressUI progressUI, String fileType) {
        return showChopper(progressUI, fileType, true);
    }

    public static String showDirectoryChooser(ProgressUI progressUI, String fileType) {
        return showChopper(progressUI, fileType, false);
    }

    private static String showChopper(ProgressUI progressUI, String fileType, boolean isFile) {
        try {
            progressUI.setVisible(false);
            String title = DEFAULT_TITLE_PREFIX + fileType + (isFile ? " File" : " Directory");
            
            return isFile 
                ? AutomationUI.showFileChooser(null, title)
                : AutomationUI.showDirectoryChooser(null, title);
        } finally {
            progressUI.setVisible(true);
        }
    }

    public static void showErrorDialog(String message) {
        AutomationUI.showMessageDialog(
            null,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}