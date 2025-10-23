package automation.tasks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import automation.TestSuite;
import automation.helpers.FileChooserHelper;
import automation.ui.ProgressUI;

public abstract class TaskBase implements AutomationTask {
    protected void handleError(ProgressUI progressUI, Exception e) {
        String errorMsg = "❌ " + getName() + " failed: " + e.getMessage();
        progressUI.updateStepProgress(100, errorMsg);
        FileChooserHelper.showErrorDialog(errorMsg);
    }
    
    protected void checkCancellation() {
        if (TestSuite.isTaskCancelled()) {
            throw new RuntimeException("Task cancelled by user");
        }
    }

    protected String getFile(ProgressUI progressUI, String fileType) {
        return FileChooserHelper.showFileChopper(progressUI, fileType);
    }

    protected String getDirectory(ProgressUI progressUI, String dirType) {
        return FileChooserHelper.showDirectoryChooser(progressUI, dirType);
    }

    /*protected void initializeProgress(ProgressUI progressUI, int maxSteps) {
        progressUI.showProgress(getName(), "Initializing...");
        progressUI.setMainProgressMax(maxSteps);
        progressUI.setStepProgressMax(100);
    }*/
    
    protected void completeAndHide(ProgressUI progressUI, String message) {
        progressUI.completeAndHide(message);
    }

    protected void errorAndHide(ProgressUI progressUI, Exception e) {
        String errorMsg = "❌ " + getName() + " failed: " + e.getMessage();
        progressUI.updateStepProgress(100, errorMsg);
        progressUI.updateStatus(errorMsg);
        FileChooserHelper.showErrorDialog(errorMsg);
        
        new javax.swing.Timer(1500, evt -> {
            progressUI.close();
            ((javax.swing.Timer)evt.getSource()).stop();
        }).start();
    }

    protected void cancelAndHide(ProgressUI progressUI) {
        progressUI.showCancellation();
        
        new javax.swing.Timer(500, e -> {
        	progressUI.close();
            ((javax.swing.Timer)e.getSource()).stop();
        }).start();
    }
    
    public static Set<String> getAllPartNumbers(WebDriver driver) {
        Set<String> existingParts = new HashSet<>();
        try {
            // Simple unified approach: Find all <b> tags that contain part numbers
            // This works for both active parts and deleted parts
            List<WebElement> partNumberElements = driver.findElements(By.xpath("//td//b"));
            
            for (WebElement element : partNumberElements) {
                String partNo = element.getText().trim();
                if (partNo != null && !partNo.isEmpty()) {
                    existingParts.add(partNo);
                }
            }
            
            System.out.println("Found " + existingParts.size() + " existing parts: " + existingParts);
            
        } catch (Exception e) {
            System.out.println("Error reading part numbers - continuing with empty set: " + e.getMessage());
        }
        
        return existingParts;
    }
}