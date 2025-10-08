// Config.java
package automation.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {
    private UiConfig ui;
    private AutomationConfig automation;
    private SeleniumConfig selenium;
    private Map<String, Object> custom;
    
    // Getters and setters
    public UiConfig getUi() { return ui; }
    public void setUi(UiConfig ui) { this.ui = ui; }
    
    public AutomationConfig getAutomation() { return automation; }
    public void setAutomation(AutomationConfig automation) { this.automation = automation; }
    
    public SeleniumConfig getSelenium() { return selenium; }
    public void setSelenium(SeleniumConfig selenium) { this.selenium = selenium; }
    
    public Map<String, Object> getCustom() { return custom; }
    public void setCustom(Map<String, Object> custom) { this.custom = custom; }
    
    // Nested config classes
    public static class UiConfig {
        private String theme = "dark";
        private boolean enableAnimations = true;
        private int dialogWidth = 400;
        private int dialogHeight = 300;
        private FontConfig font;
        
        // Getters and setters
        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }
        
        public boolean isEnableAnimations() { return enableAnimations; }
        public void setEnableAnimations(boolean enableAnimations) { this.enableAnimations = enableAnimations; }
        
        public int getDialogWidth() { return dialogWidth; }
        public void setDialogWidth(int dialogWidth) { this.dialogWidth = dialogWidth; }
        
        public int getDialogHeight() { return dialogHeight; }
        public void setDialogHeight(int dialogHeight) { this.dialogHeight = dialogHeight; }
        
        public FontConfig getFont() { return font; }
        public void setFont(FontConfig font) { this.font = font; }
    }
    
    public static class FontConfig {
        private String family = "Segoe UI";
        private int size = 14;
        private String style = "plain"; // plain, bold, italic
        
        // Getters and setters
        public String getFamily() { return family; }
        public void setFamily(String family) { this.family = family; }
        
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        
        public String getStyle() { return style; }
        public void setStyle(String style) { this.style = style; }
    }
    
    public static class AutomationConfig {
        private int defaultRetryAttempts = 3;
        private int timeoutSeconds = 30;
        private boolean enableScreenshots = true;
        private String screenshotDirectory = "screenshots";
        private boolean visualDebug = true;
        
        // Getters and setters
        public int getDefaultRetryAttempts() { return defaultRetryAttempts; }
        public void setDefaultRetryAttempts(int defaultRetryAttempts) { this.defaultRetryAttempts = defaultRetryAttempts; }
        
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        
        public boolean isEnableScreenshots() { return enableScreenshots; }
        public void setEnableScreenshots(boolean enableScreenshots) { this.enableScreenshots = enableScreenshots; }
        
        public String getScreenshotDirectory() { return screenshotDirectory; }
        public void setScreenshotDirectory(String screenshotDirectory) { this.screenshotDirectory = screenshotDirectory; }
        
        public boolean isVisualDebug() { return visualDebug; }
        public void setVisualDebug(boolean visualDebug) { this.visualDebug = visualDebug; }
    }
    
    public static class SeleniumConfig {
        private boolean headless = false;
        private int implicitWaitSeconds = 10;
        private int pageLoadTimeoutSeconds = 30;
        private String browser = "chrome";
        private WindowConfig window;
        
        // Getters and setters
        public boolean isHeadless() { return headless; }
        public void setHeadless(boolean headless) { this.headless = headless; }
        
        public int getImplicitWaitSeconds() { return implicitWaitSeconds; }
        public void setImplicitWaitSeconds(int implicitWaitSeconds) { this.implicitWaitSeconds = implicitWaitSeconds; }
        
        public int getPageLoadTimeoutSeconds() { return pageLoadTimeoutSeconds; }
        public void setPageLoadTimeoutSeconds(int pageLoadTimeoutSeconds) { this.pageLoadTimeoutSeconds = pageLoadTimeoutSeconds; }
        
        public String getBrowser() { return browser; }
        public void setBrowser(String browser) { this.browser = browser; }
        
        public WindowConfig getWindow() { return window; }
        public void setWindow(WindowConfig window) { this.window = window; }
    }
    
    public static class WindowConfig {
        private int width = 1920;
        private int height = 1080;
        private boolean maximize = true;
        
        // Getters and setters
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        
        public boolean isMaximize() { return maximize; }
        public void setMaximize(boolean maximize) { this.maximize = maximize; }
    }
}