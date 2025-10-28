package automation.tasks;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import automation.TestSuite;
import automation.helpers.FileChooserHelper;
import automation.ui.ProgressUI;

public abstract class TaskBase implements AutomationTask {
	
	protected void handleError(ProgressUI progressUI, Exception e) {
	    String errorMsg = "❌ " + getName() + " failed: " + e.getMessage();
	    if (progressUI != null) {
	        progressUI.updateStepProgress(100, errorMsg);
	        progressUI.updateStatus(errorMsg);
	        
	        // Auto-close after showing error
	        new javax.swing.Timer(2000, evt -> {
	            progressUI.close();
	            ((javax.swing.Timer)evt.getSource()).stop();
	        }).start();
	    }
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
    
    protected String getCsvFile(ProgressUI progressUI, String fileType) {
        return getCsvFile(progressUI, fileType, true);
    }

    protected String getCsvFile(ProgressUI progressUI, String fileType, boolean cancelOnNull) {
        String csvPath = getFile(progressUI, fileType + " CSV");
        if (csvPath == null && cancelOnNull) {
            cancelAndHide(progressUI);
            return null;
        }
        return csvPath;
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
    
    private void waitForPageToLoad(WebDriver driver) {
        try {
            // Wait for document.readyState to be 'complete'
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> ((JavascriptExecutor) d)
                    .executeScript("return document.readyState").equals("complete"));
            //System.out.println("Page loaded - document.readyState is 'complete'");
            
            // Optional: Additional wait for jQuery (if the site uses it)
            try {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(d -> ((JavascriptExecutor) d)
                        .executeScript("return window.jQuery != undefined && jQuery.active == 0"));
                //System.out.println("jQuery activities completed");
            } catch (Exception e) {
                // jQuery not present or not needed, continue
            }
            
        } catch (Exception e) {
            //System.out.println("Page load timeout, but continuing anyway: " + e.getMessage());
        }
    }

    // Keep your existing waitForAutoScrollToComplete method
    private void waitForAutoScrollToComplete(WebDriver driver) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long lastScrollY = getCurrentScrollY(driver);
        int stableCount = 0;
        final int MAX_WAIT_TIME = 5000;
        final int STABLE_THRESHOLD = 4;
        
        waitForPageToLoad(driver);
        
        //System.out.println("Monitoring for auto-scroll...");
        
        while (System.currentTimeMillis() - startTime < MAX_WAIT_TIME) {
            Thread.sleep(200);
            
            long currentScrollY = getCurrentScrollY(driver);
            
            if (currentScrollY == lastScrollY) {
                stableCount++;
                if (stableCount >= STABLE_THRESHOLD) {
                    System.out.println("Auto-scroll completed after " + (System.currentTimeMillis() - startTime) + "ms");
                    return;
                }
            } else {
                stableCount = 0;
                lastScrollY = currentScrollY;
                //System.out.println("Auto-scroll detected - scroll changed to: " + currentScrollY);
            }
        }
        
        //System.out.println("Auto-scroll timeout after " + (System.currentTimeMillis() - startTime) + "ms, proceeding anyway");
    }

    private long getCurrentScrollY(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript("return window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0;");
            if (result instanceof Long) {
                return (Long) result;
            } else if (result instanceof Integer) {
                return ((Integer) result).longValue();
            }
            return 0L;
        } catch (Exception e) {
            System.out.println("Error getting scroll position: " + e.getMessage());
            return 0L;
        }
    }

    protected void scrollToTop(WebDriver driver) throws InterruptedException {
    	waitForAutoScrollToComplete(driver);
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
        Thread.sleep(200); // Brief pause after scrolling to ensure it completes
    }
}