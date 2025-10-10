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
import org.openqa.selenium.chrome.ChromeOptions;
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
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--incognito",
                           "--disable-save-password-bubble",
                           "--disable-autofill",
                           "--disable-autofill-profile",
                           "--disable-autofill-keyboard-accessory-view");
        WebDriver driver = new ChromeDriver(options);
        
        WebDriverManager.chromedriver().setup();
        
        try {
            // Initialize progress UI
            progressUI.startTask(taskName); // This shows and resets progress
            progressUI.updateStatus("Launching browser");
            
            // Navigate to server and login
            driver.get(server.getUrl());
            driver.manage().window().maximize();
            
            // Login
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
                
                // Handle task completion based on task type
                if (!(task instanceof PageLoadTimeTask)) {
                    // For regular tasks: quit driver and return to main app
                    driver.quit();
                    
                    // ProgressUI will be hidden by the task's completeAndHide method
                    // Return to main application after a brief delay
                    new javax.swing.Timer(1000, e -> {
                        ((javax.swing.Timer)e.getSource()).stop();
                        SwingUtilities.invokeLater(TestSuite::startApplication);
                    }).start();
                }
                // PageLoadTimeTask manages its own UI and ProgressUI visibility
            } else {
                throw new IllegalArgumentException("Unknown task: " + taskName);
            }
        } catch (Exception e) {
            // Error handling - ensure ProgressUI is properly reset
            progressUI.updateStepProgress(100, "❌ Failed: " + e.getMessage());
            AutomationUI.showMessageDialog(
                null, 
                "Error during execution: " + e.getMessage(), 
                "Task Failed", 
                JOptionPane.ERROR_MESSAGE
            );
            
            // Clean up on error
            try {
                driver.quit();
            } catch (Exception ex) {
                // Ignore cleanup errors
            }
            
            // Hide ProgressUI and return to main app
            progressUI.setVisible(false);
            progressUI.resetProgress();
            
            new javax.swing.Timer(1500, evt -> {
                ((javax.swing.Timer)evt.getSource()).stop();
                SwingUtilities.invokeLater(TestSuite::startApplication);
            }).start();
        } 
    }
}