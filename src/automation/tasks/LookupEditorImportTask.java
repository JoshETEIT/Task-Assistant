package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import automation.TestSuite;
import automation.helpers.CsvReader;
import automation.ui.ProgressUI;

import java.time.Duration;
import java.util.List;

import static automation.helpers.ElementHelper.*;

public class LookupEditorImportTask extends TaskBase {
    
    @Override
    public String getName() {
        return "Lookup Editor Import";
    }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        try {
            progressUI.updateStatus("Starting Lookup Editor import");
            progressUI.setMainProgressMax(1);
            progressUI.setStepProgressMax(100);
            
            // Get CSV file
            String csvPath = getCsvFile(progressUI, "Lookup Editor");
            if (csvPath == null) return;
            
            // Read CSV data
            progressUI.updateStepProgress(10, "Reading CSV file...");
            List<LookupEditorItem> items = CsvReader.read(csvPath, this::createItem);
            
            // Navigate to Lookup Editor
            progressUI.updateStepProgress(30, "Navigating to Lookup Editor");
            navigateToLookupEditor(driver, baseUrl);
            
            // Import the items
            progressUI.updateStepProgress(50, "Importing lookup items");
            progressUI.setMainProgressMax(items.size());
            importLookupItems(driver, items, progressUI);
            
            completeAndHide(progressUI, "Successfully imported " + items.size() + " lookup items");
            
        } catch (Exception e) {
            handleError(progressUI, e);
        }
    }
    
    private LookupEditorItem createItem(String[] fields) {
        LookupEditorItem item = new LookupEditorItem();
        if (fields.length >= 1) {
            item.setName(fields[0]);
        }
        return item;
    }
    
    private void navigateToLookupEditor(WebDriver driver, String baseUrl) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        
        if (!driver.getCurrentUrl().contains("/Home")) {
            driver.get(baseUrl + "/Home");
        }
        
        // Navigate directly to Lookup Editor URL
        driver.get(baseUrl + "/Production/LookupEditor/IronmongeryLookup");
        wait.until(ExpectedConditions.urlContains("LookupEditor"));
        
        // Wait for the page to load completely
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("new_ironmongery_label")));
    }
    
    private void importLookupItems(WebDriver driver, List<LookupEditorItem> items, ProgressUI progressUI) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        Actions actions = new Actions(driver);
        int importedCount = 0;
        
        for (int i = 0; i < items.size() && !TestSuite.isTaskCancelled(); i++) {
            LookupEditorItem item = items.get(i);
            progressUI.updateMainProgress(i);
            progressUI.updateStepProgress(50 + (i * 40 / items.size()), 
                "Importing item " + (i + 1) + "/" + items.size() + " (" + importedCount + " imported)");
            
            try {
                checkCancellation();
                
                // Scroll to top before each operation
                actions.sendKeys(Keys.HOME).perform();
                Thread.sleep(200);
                
                // Enter item name
                enterText(wait, LocatorType.ID, "new_ironmongery_label", item.getName());
                
                // Add cancellation check before final submission
                checkCancellation();
                
                // Click add button
                clickButton(driver, LocatorType.ID, "add_ironmongery", Screenshot.ON, 3);
                
                // Wait for operation to complete
                Thread.sleep(500);
                
                importedCount++;
                progressUI.updateStatus("Imported: " + item.getName());
                
            } catch (Exception e) {
                System.out.println("Error importing item '" + item.getName() + "': " + e.getMessage());
                // Continue with next item
            }
        }
        
        progressUI.updateStatus("Import complete: " + importedCount + " items added");
    }
    
    // Data class for lookup editor information
    private static class LookupEditorItem {
        String name;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}