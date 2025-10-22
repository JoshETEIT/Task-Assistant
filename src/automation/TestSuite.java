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
    private static ProgressUI progressUI;
    private static WebDriver currentDriver;
    private static Thread currentTaskThread;
    private static volatile boolean taskCancelled = false;

    public static void main(String[] args) {
        startApplication();
        //testProgressUIVisibility();
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
        // Create NEW ProgressUI instance for each task
        progressUI = new ProgressUI();
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--incognito",
                           "--disable-save-password-bubble",
                           "--disable-autofill",
                           "--disable-autofill-profile",
                           "--disable-autofill-keyboard-accessory-view");
        WebDriver driver = new ChromeDriver(options);
        
        WebDriverManager.chromedriver().setup();
        
        // Create a separate thread for the task
        Thread taskThread = new Thread(() -> {
            try {
                // Register this task with the task manager
                TestSuite.setCurrentTask(Thread.currentThread(), driver, progressUI);
                
                // Initialize progress UI
                progressUI.startTask(taskName);
                progressUI.updateStatus("Launching browser");
                
                // Navigate to server and login
                driver.get(server.getUrl());
                driver.manage().window().maximize();
                
                // Check for cancellation
                if (TestSuite.isTaskCancelled()) {
                    return;
                }
                
                // Login
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.elementToBeClickable(By.id("login_user_name")))
                    .sendKeys(server.getUsername());
                wait.until(ExpectedConditions.elementToBeClickable(By.id("login_password")))
                    .sendKeys(server.getPassword());
                wait.until(ExpectedConditions.elementToBeClickable(By.id("submit_button")))
                    .click();
                
                progressUI.updateStatus("Logged in. Running task...");
                
                // Check for cancellation
                if (TestSuite.isTaskCancelled()) {
                    return;
                }
                
                // Execute the selected task
                AutomationTask task = TaskRegistry.getTask(taskName);
                if (task != null) {
                    task.execute(driver, server.getUrl(), progressUI);
                    
                    // Handle task completion based on task type
                 // Only return to main menu for successfully completed regular tasks
                    if (!(task instanceof PageLoadTimeTask) && !TestSuite.isTaskCancelled()) {
                        SwingUtilities.invokeLater(() -> {
                            new javax.swing.Timer(1000, e -> {
                                ((javax.swing.Timer)e.getSource()).stop();
                                TestSuite.startApplication();
                            }).start();
                        });
                    }

                    TestSuite.clearCurrentTask();
                    // PageLoadTimeTask manages its own UI and ProgressUI visibility
                } else {
                    throw new IllegalArgumentException("Unknown task: " + taskName);
                }
            } catch (Exception e) {
                System.out.println("❌ Exception during task execution: " + e.getMessage());
                
                if (TestSuite.isTaskCancelled()) {
                    // USER CANCELLATION
                    System.out.println("✅ Task cancelled by user");
                    if (progressUI != null) {
                        progressUI.showCancellation();
                    }
                } else {
                    // REAL ERROR - but we don't close the browser
                    System.out.println("❌ Real error occurred - browser remains open for inspection");
                    if (progressUI != null) {
                        progressUI.updateStepProgress(100, "❌ Failed: " + e.getMessage());
                        AutomationUI.showMessageDialog(
                            null, 
                            "Error during execution: " + e.getMessage(), 
                            "Task Failed", 
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
                
                // NEVER call driver.quit() - browser stays open regardless
                TestSuite.clearCurrentTask();
                
                // Progress UI will handle its own closing via showCancellation() or the error above
            }
        });
        
        taskThread.start();
    }
    
    // Consider moving to a task manager helper
    public static void setCurrentTask(Thread taskThread, WebDriver driver, ProgressUI ui) {
        currentTaskThread = taskThread;
        currentDriver = driver;
        progressUI = ui;
        taskCancelled = false;
    }
    
    public static void cancelCurrentTask() {
        taskCancelled = true;
        System.out.println("🚫 Task cancellation requested by user");
        
        if (currentTaskThread != null && currentTaskThread.isAlive()) {
            System.out.println("🛑 Interrupting task thread: " + currentTaskThread.getName());
            currentTaskThread.interrupt();
        }
        
        if (progressUI != null) {
            progressUI.showCancellation();
        }
        
        System.out.println("💡 Cancellation flag set - browser will remain open");
    }
    
    public static boolean isTaskCancelled() {
        return taskCancelled;
    }
    
    public static void clearCurrentTask() {
        currentTaskThread = null;
        currentDriver = null;
        progressUI = null;
        taskCancelled = false;
    }
}