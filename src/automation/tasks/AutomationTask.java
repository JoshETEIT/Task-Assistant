package automation.tasks;

import org.openqa.selenium.WebDriver;

import automation.ui.ProgressUI;

public interface AutomationTask {
    String getName();
    void execute(WebDriver driver, String baseUrl, ProgressUI progressUI);
}