package automation;

import automation.ui.AutomationUI;
import automation.ui.ProgressUI;
import automation.ui.ServerUI;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import javax.swing.*;

import java.io.File;
import java.time.Duration;

public class TestSuite {
    private static ServerManager serverManager = new ServerManager();
    private static ProgressUI progressUI = new ProgressUI();

    public static void main(String[] args) {
        startApplication();
    }

    public static void startApplication() {
        SwingUtilities.invokeLater(() -> {
            String[] options = {
            		"Add Lead", 
            	    "Import Ironmongery", 
            	    "Import Glass Parts",
            	    "Upload Part Images",
            	    "Update Ironmongery Defaults",
            	    "Exit"
            };
            
            int choice = AutomationUI.showOptionDialog(
                null,
                "What action would you like to perform?",
                "Automation Suite | Select Action",
                options
            );

            if (choice == options.length - 1 || choice == JOptionPane.CLOSED_OPTION) {
                System.exit(0);
            }
            
            boolean runAddLead = (choice == 0);
            boolean runIronmongeryImport = (choice == 1);
            boolean runGlassImport = (choice == 2);
            boolean runUploadImages = (choice == 3);
            boolean runUpdateDefaults = (choice == 4);
            
            new ServerUI(serverManager).showServerTable(runAddLead, runIronmongeryImport, runGlassImport, runUploadImages, runUpdateDefaults);
        });
    }

    public static void runSeleniumTest(
            ServerManager.Server s, 
            boolean runAddLead, 
            boolean runIronmongeryImport,
            boolean runGlassImport,
            boolean runUploadImages, 
            boolean runUpdateDefaults
    ) {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        progressUI.showProgress("Automation Progress", "Initializing...");
        progressUI.updateProgress(10, 0, "Launching browser");

        try {
            driver.get(s.getUrl());
            driver.manage().window().maximize();
            progressUI.updateProgress(20, 0, "Navigating to login page");

            wait.until(ExpectedConditions.elementToBeClickable(By.id("login_user_name"))).sendKeys(s.getUsername());
            wait.until(ExpectedConditions.elementToBeClickable(By.id("login_password"))).sendKeys(s.getPassword());
            wait.until(ExpectedConditions.elementToBeClickable(By.id("submit_button"))).click();
            progressUI.updateProgress(70, 0, "Logged in. Running test...");

            if (runAddLead) {
                for (int i = 0; i < 5; i++) {
                    boolean success = AddNewLead.testFormSubmission(driver);
                    System.out.println((success ? "✅" : "❌") + " Test result for " + s.getName());
                    driver.get(s.getUrl() + "/Home");
                }
            } 
            else if (runIronmongeryImport) {
                progressUI.close();
                String csvPath = AutomationUI.showFileChooser(
                    null,
                    "Automation Suite | Select Ironmongery CSV File"
                );

                if (csvPath != null) {
                    try {
                        AddingIronmongery.importIronmongery(
                            AddingIronmongery.CSVReader(csvPath),
                            driver,
                            s.getUrl()
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (runGlassImport) {
                progressUI.close();
                String csvPath = AutomationUI.showFileChooser(
                    null,
                    "Automation Suite | Select Glass Parts CSV File"
                );

                if (csvPath != null) {
                    try {
                        GlassPartImport.importGlassParts(
                            GlassPartImport.CSVReader(csvPath),
                            driver,
                            s.getUrl()
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (runUploadImages) {
                progressUI.close();
                String folderPath = AutomationUI.showDirectoryChooser(
                    null, 
                    "Automation Suite | Select Images Directory"
                );

                if (folderPath == null) {
                    progressUI.updateProgress(0, 0, "Operation cancelled");
                    progressUI.close();
                    return;
                }

                File dir = new File(folderPath);
                if (!dir.exists() || !dir.isDirectory()) {
                    AutomationUI.showMessageDialog(
                        null,
                        "The selected directory is not accessible",
                        "Directory Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                String[] partTypes = {"Glass", "Ironmongery"};
                int partTypeChoice = AutomationUI.showOptionDialog(
                    null,
                    "Upload images for which part type?",
                    "Automation Suite | Select Part Type",
                    partTypes
                );
                
                if (partTypeChoice == JOptionPane.CLOSED_OPTION) {
                    progressUI.close();
                    return;
                }

                progressUI.showProgress("Automation Progress", "Starting upload...");
                progressUI.updateProgress(10, 0, "Preparing upload...");
                
                try {
                    if (partTypeChoice == 0) {
                        new GlassPartImageUploader(driver).uploadImagesFromFolder(folderPath);
                    } else {
                        new IronmongeryPartImageUploader(driver).uploadImagesFromFolder(folderPath);
                    }
                } catch (Exception e) {
                    AutomationUI.showMessageDialog(
                        null, 
                        "Upload failed: " + e.getMessage(), 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ignored) {}
                        progressUI.close();
                    }).start();
                }
            }
            else if (runUpdateDefaults) {
                boolean success = new UpdateIronmongeryDefaults(driver).updateDefaults(s.getUrl());
                System.out.println((success ? "✅" : "❌") + " Update ironmongery defaults result for " + s.getName());
            }

        } catch (Exception e) {
            AutomationUI.showMessageDialog(
                null, 
                "Error: " + e.getMessage(), 
                "Test Failed", 
                JOptionPane.ERROR_MESSAGE
            );
        } finally {
            progressUI.close();
            SwingUtilities.invokeLater(() -> TestSuite.startApplication());
        }
    }
}