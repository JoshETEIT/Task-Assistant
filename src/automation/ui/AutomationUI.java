package automation.ui;

import javax.swing.*;
import java.awt.*;

public abstract class AutomationUI {
    protected JDialog createBaseDialog(String title, int width, int height) {
        JDialog dialog = new JDialog((Frame) null, title, false);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);
        return dialog;
    }

    protected JPanel createVerticalPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }
}