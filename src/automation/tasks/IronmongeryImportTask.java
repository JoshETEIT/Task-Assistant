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

public class IronmongeryImportTask implements AutomationTask {
    
    @Override
    public String getName() {
        return "Import Ironmongery";
    }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        try {
            progressUI.updateStatus("Selecting CSV file...");
            String csvPath = showFileChooserWithProgress(progressUI);

            if (csvPath == null) {
                progressUI.updateStatus("Import cancelled");
                return;
            }

            progressUI.updateStatus("Reading CSV...");
            ArrayList<IronmongeryItem> items = readIronmongeryCSV(csvPath);
            
            progressUI.updateStatus("Importing items...");
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

    private String showFileChooserWithProgress(ProgressUI progressUI) {
        try {
            progressUI.setVisible(false);
            return AutomationUI.showFileChooser(
                null,
                "Automation Suite | Select Ironmongery CSV File"
            );
        } finally {
            progressUI.setVisible(true);
        }
    }

    // CSV Item definition
    private static class IronmongeryItem {
        String partNo, name, cost, unit, type, notes;

        public String getPartNo() { return partNo; }
        public void setPartNo(String partNo) { this.partNo = partNo; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCost() { return cost; }
        public void setCost(String cost) { this.cost = cost; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        @SuppressWarnings("unused")
		public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        @SuppressWarnings("unused")
		public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    // CSV Reading
    private ArrayList<IronmongeryItem> readIronmongeryCSV(String csvPath) throws Exception {
        ArrayList<IronmongeryItem> itemList = new ArrayList<>();
        try (Scanner sc = new Scanner(new File(csvPath))) {
            if (sc.hasNextLine()) sc.nextLine(); // Skip header

            while (sc.hasNextLine()) {
                String[] fields = sc.nextLine().split(",");
                IronmongeryItem item = new IronmongeryItem();
                item.setPartNo(fields[0]);
                item.setName(fields[1]);
                item.setCost(fields[2]);
                item.setUnit(fields[3]);
                item.setType(fields[4]);
                item.setNotes(fields.length > 5 ? fields[5] : "");
                itemList.add(item);
            }
        }
        return itemList;
    }

    // Import Logic
    private void performImport(ArrayList<IronmongeryItem> items, WebDriver driver, 
                             String baseUrl, ProgressUI progressUI) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Actions actions = new Actions(driver);
        
        driver.get(baseUrl + "/PricingAndConfig/PartList/IM");
        
        for (int i = 0; i < items.size(); i++) {
            IronmongeryItem item = items.get(i);
            progressUI.updateMainProgress(i);
            progressUI.updateStepProgress(0, "Processing " + item.getPartNo());
            
            try {
                // Scroll to top before each item
                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
                
                // Click Add Part button
                WebElement addButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("add_part_button")));
                addButton.click();
                
                // Fill basic fields
                enterTextById(wait, "part_no", item.getPartNo());
                enterTextById(wait, "part_name", item.getName());
                
                // Select dropdowns
                selectDropdownByVisibleText(driver, "part_unit_name", item.getUnit());
                selectDropdownByVisibleText(driver, "part_allocated_unit_name", "each");
                
                // Handle special units
                if (item.getUnit().equalsIgnoreCase("pair") || 
                    item.getUnit().equalsIgnoreCase("set") ||
                    item.getUnit().equalsIgnoreCase("roll")) {
                    enterTextById(wait, "part_allocated_amount_in_purchase_unit", "1");
                }
                
                // Enter cost
                enterTextById(wait, "part_cost", item.getCost());
                
                // Submit part
                clickButtonById(driver, "part_dialog_submit_new");
                
                // Wait for completion
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.id("part_dialog_submit_new")));
                
                // Scroll to top using multiple methods
                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
                actions.sendKeys(Keys.HOME).perform();
                Thread.sleep(500);
                
                progressUI.updateStepProgress(100, "✅ Part added");
            } catch (Exception e) {
                progressUI.updateStepProgress(100, "❌ Failed: " + e.getMessage());
                System.out.println("Error adding part " + item.getPartNo() + ": " + e.getMessage());
                
                // Try to close any open dialogs
                try {
                    driver.findElement(By.cssSelector(".ui-dialog-titlebar-close")).click();
                } catch (Exception ex) {
                    // Ignore if we can't close it
                }
            }
        }
    }
}