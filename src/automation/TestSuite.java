package automation;

import automation.tasks.AutomationTask;
import automation.tasks.PageLoadTimeTask;
import automation.tasks.TaskRegistry;
import automation.ui.AutomationUI;
import automation.ui.ProgressUI;
import automation.ui.ServerUI;
import io.github.bonigarcia.wdm.WebDriverManager;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import javax.swing.*;

public class TestSuite {
    private static final ServerManager serverManager = new ServerManager();
    private static final ProgressUI progressUI = new ProgressUI();

    public static void main(String[] args) {
        startApplication();
    }

    public static void startApplication() {
        SwingUtilities.invokeLater(() -> {
            String[] taskOptions = TaskRegistry.getTaskNames();
            
            int choice = AutomationUI.showOptionDialog(
                null,
                "What action would you like to perform?",
                "Automation Suite | Select Action",
                taskOptions
            );

            if (choice == JOptionPane.CLOSED_OPTION) {
                System.exit(0);
            }
            
            String selectedTask = taskOptions[choice];
            new ServerUI(serverManager, selectedTask).showServerTable();
        });
    }

    public static void runSeleniumTest(ServerManager.Server server, String taskName) {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        
        try {
            // Initialize progress UI
            progressUI.showProgress("Automation Progress", "Initializing...");
            progressUI.updateStatus("Launching browser");
            
            // Navigate to server and login
            driver.get(server.getUrl());
            driver.manage().window().maximize();
            
            // Login (existing code)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.elementToBeClickable(By.id("login_user_name")))
                .sendKeys(server.getUsername());
            wait.until(ExpectedConditions.elementToBeClickable(By.id("login_password")))
                .sendKeys(server.getPassword());
            wait.until(ExpectedConditions.elementToBeClickable(By.id("submit_button")))
                .click();
            
            progressUI.updateStatus("Logged in. Running task...");
            
            // Execute the selected task
            AutomationTask task = TaskRegistry.getTask(taskName);
            if (task != null) {
                task.execute(driver, server.getUrl(), progressUI);
                
                // Only auto-close for non-timer tasks
                if (!(task instanceof PageLoadTimeTask)) {
                    driver.quit();
                    progressUI.close();
                    SwingUtilities.invokeLater(TestSuite::startApplication);
                }
            } else {
                throw new IllegalArgumentException("Unknown task: " + taskName);
            }
        } catch (Exception e) {
            progressUI.updateStepProgress(100, "❌ Failed: " + e.getMessage());
            AutomationUI.showMessageDialog(
                null, 
                "Error during execution: " + e.getMessage(), 
                "Task Failed", 
                JOptionPane.ERROR_MESSAGE
            );
        } 
    }
}