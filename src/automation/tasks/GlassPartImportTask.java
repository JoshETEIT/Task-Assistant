package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import automation.ui.ProgressUI;
import automation.ui.AutomationUI;
import static automation.helpers.ElementHelper.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.time.Duration;
import javax.swing.*;

public class GlassPartImportTask implements AutomationTask {
    
    @Override
    public String getName() {
        return "Import Glass Parts";
    }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        try {
            // Step 1: Get CSV file
            progressUI.close(); // Close progress UI for file selection
            String csvPath = AutomationUI.showFileChooser(
                null,
                "Automation Suite | Select Glass Parts CSV File"
            );

            if (csvPath == null) {
                progressUI.updateStatus("Import cancelled");
                return;
            }

            // Step 2: Read CSV
            progressUI.showProgress("Importing Glass Parts", "Starting...");
            ArrayList<GlassPartItem> items = readGlassPartCSV(csvPath);
            
            // Step 3: Perform import
            progressUI.setMainProgressMax(items.size());
            performImport(items, driver, baseUrl, progressUI);
            
            progressUI.updateStepProgress(100, "✅ Import completed");
        } catch (Exception e) {
            progressUI.updateStepProgress(100, "❌ Failed: " + e.getMessage());
            AutomationUI.showMessageDialog(
                null, 
                "Import failed: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static class GlassPartItem {
        String partNo, partName, pUnit, cost, obscure;

        public String getPartNo() { return partNo; }
        public void setPartNo(String partNo) { this.partNo = partNo; }
        public String getPartName() { return partName; }
        public void setPartName(String partName) { this.partName = partName; }
        public String getpUnit() { return pUnit; }
        public void setpUnit(String pUnit) { this.pUnit = pUnit; }
        public String getCost() { return cost; }
        public void setCost(String cost) { this.cost = cost; }
        public String getObscure() { return obscure; }
        public void setObscure(String obscure) { this.obscure = obscure; }
    }

    private ArrayList<GlassPartItem> readGlassPartCSV(String csvPath) throws Exception {
        ArrayList<GlassPartItem> itemList = new ArrayList<>();
        try (Scanner sc = new Scanner(new File(csvPath))) {
            if (sc.hasNextLine()) sc.nextLine(); // Skip header

            while (sc.hasNextLine()) {
                String[] fields = sc.nextLine().split(",");
                GlassPartItem item = new GlassPartItem();
                item.setPartNo(fields[0]);
                item.setPartName(fields[1]);
                item.setpUnit(fields[2]);
                item.setCost(fields[3]);
                item.setObscure(fields.length > 4 ? fields[4] : "No");
                itemList.add(item);
            }
        }
        return itemList;
    }

    private void performImport(ArrayList<GlassPartItem> items, WebDriver driver, 
                            String baseUrl, ProgressUI progressUI) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Actions actions = new Actions(driver);
        
        driver.get(baseUrl + "/PricingAndConfig/PartList/GL");
        wait.until(ExpectedConditions.urlContains("/PricingAndConfig/PartList/GL"));
        
        // Wait for page to be fully loaded
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("add_part_button")));
        
        for (int i = 0; i < items.size(); i++) {
            GlassPartItem item = items.get(i);
            progressUI.updateMainProgress(i);
            progressUI.updateStepProgress(0, "Processing " + item.getPartNo());
            
            try {
                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
                
                // Improved Add Part button click with JavaScript
                clickAddPartButtonWithRetry(driver, wait);
                
                // Fill basic fields
                enterTextById(wait, "part_no", item.getPartNo());
                enterTextById(wait, "part_name", item.getPartName());

                // Select dropdowns
                selectDropdownByVisibleText(driver, "part_unit_name", item.getpUnit());
                selectDropdownByVisibleText(driver, "part_allocated_unit_name", "each");

                // Set allocated amount
                enterTextById(wait, "part_allocated_amount_in_purchase_unit", "1");

                // Enter cost
                enterTextById(wait, "part_cost", item.getCost());

                // Handle obscure glass
                if ("yes".equalsIgnoreCase(item.getObscure())) {
                    selectCheckboxOrRadioButton(driver, "part_is_obscure_glass");
                }

                // Submit part using JavaScript click
                WebElement submitButton = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("part_dialog_submit_new")));
                ((JavascriptExecutor)driver).executeScript("arguments[0].click();", submitButton);
                
                // Wait for dialog to close
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.id("part_dialog_submit_new")));
                
                // Scroll to top using JavaScript
                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
                actions.sendKeys(Keys.HOME).perform();
                
                progressUI.updateStepProgress(100, "✅ Part added");
            } catch (Exception e) {
                progressUI.updateStepProgress(100, "❌ Failed: " + e.getMessage());
                System.out.println("Error adding part " + item.getPartNo() + ": " + e.getMessage());
                
                // Try to close any open dialogs
                try {
                    WebElement closeButton = driver.findElement(By.cssSelector(".ui-dialog-titlebar-close"));
                    ((JavascriptExecutor)driver).executeScript("arguments[0].click();", closeButton);
                } catch (Exception ex) {
                    // Ignore if we can't close it
                }
            }
        }
    }
    
    private void clickAddPartButtonWithRetry(WebDriver driver, WebDriverWait wait) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                // Find the button first
                WebElement addButton = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("add_part_button")));
                
                // Scroll into view centered
                ((JavascriptExecutor)driver).executeScript(
                    "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", 
                    addButton);
                
                // Click using JavaScript
                ((JavascriptExecutor)driver).executeScript("arguments[0].click();", addButton);
                
                // Verify the click worked by waiting for the dialog to appear
                wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("part_dialog_submit_new")));
                return;
            } catch (Exception e) {
                attempts++;
                if (attempts == 3) throw e;
                
                // If failed, try refreshing the page
                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
                driver.navigate().refresh();
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("add_part_button")));
            }
        }
    }
}