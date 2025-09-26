package automation.helpers;

import org.openqa.selenium.*;
import org.openqa.selenium.io.FileHandler;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotHandler {
    private final WebDriver driver;
    private final String screenshotDir;
    private final boolean enabled;
    
    public ScreenshotHandler(WebDriver driver, String baseDir, boolean enabled) {
        this.driver = driver;
        this.enabled = enabled;
        this.screenshotDir = createScreenshotDirectory(baseDir);
    }
    
    public ScreenshotHandler(WebDriver driver) {
        this(driver, System.getProperty("user.dir"), true);
    }
    
    private String createScreenshotDirectory(String baseDir) {
        File dir = new File(baseDir, "screenshots");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }
    
    /**
     * Take screenshot with label and timestamp
     * @param label Descriptive label for the screenshot
     * @param highlightElement Optional element to highlight (can be null)
     * @return File path of the saved screenshot, or null if failed/disabled
     */
    public String screenshot(String label, WebElement highlightElement) {
        if (!enabled) return null;
        
        try {
            // Highlight element if provided
            if (highlightElement != null) {
                highlightElement(highlightElement, "#FF0000"); // Red border
                Thread.sleep(300); // Brief pause to see highlight
            }
            
            // Take screenshot
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            
            // Generate filename with timestamp and minimal error indicator
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            String safeLabel = label.replaceAll("[^a-zA-Z0-9_-]", "_");
            String fileName = String.format("ERR_%s_%s.png", safeLabel, timestamp);
            
            File destination = new File(screenshotDir, fileName);
            FileHandler.copy(screenshot, destination);
            
            System.out.println("📸 Screenshot saved: " + destination.getAbsolutePath());
            
            // Remove highlight
            if (highlightElement != null) {
                removeHighlight(highlightElement);
            }
            
            return destination.getAbsolutePath();
            
        } catch (Exception e) {
            System.err.println("❌ Failed to take screenshot: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Simplified version without element highlighting
     */
    public String screenshot(String label) {
        return screenshot(label, null);
    }
    
    /**
     * Quick screenshot with auto-generated label based on calling method
     */
    public String screenshot() {
        String callerMethod = Thread.currentThread().getStackTrace()[2].getMethodName();
        return screenshot(callerMethod, null);
    }
    
    /**
     * Take screenshot on error with exception context
     */
    public String screenshotOnError(Exception error, String context) {
        String errorType = error.getClass().getSimpleName();
        String label = String.format("ERROR_%s_%s", errorType, context);
        return screenshot(label, null);
    }
    
    private void highlightElement(WebElement element, String color) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].style.border='3px solid " + color + "';" +
                "arguments[0].style.boxShadow='0 0 10px " + color + "';" +
                "arguments[0].style.zIndex='9999';",
                element
            );
        } catch (Exception e) {
            System.err.println("Could not highlight element: " + e.getMessage());
        }
    }
    
    private void removeHighlight(WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].style.border='';" +
                "arguments[0].style.boxShadow='';" +
                "arguments[0].style.zIndex='';",
                element
            );
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    public String getScreenshotDirectory() {
        return screenshotDir;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}