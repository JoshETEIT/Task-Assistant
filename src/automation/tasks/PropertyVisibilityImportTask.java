package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import automation.TestSuite;
import automation.helpers.CsvReader;
import automation.helpers.ElementHelper.LocatorType;
import automation.helpers.ElementHelper.Screenshot;
import automation.ui.ProgressUI;

import java.time.Duration;
import java.util.List;

import static automation.helpers.ElementHelper.*;

public class PropertyVisibilityImportTask extends TaskBase {
    
    @Override
    public String getName() {
        return "Property Visibility Import";
    }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        try {
            progressUI.updateStatus("Starting property visibility import");
            progressUI.setMainProgressMax(1);
            progressUI.setStepProgressMax(100);
            
            // Get CSV file
            String csvPath = getCsvFile(progressUI, "Property Visibility");
            if (csvPath == null) return;
            
            // Read CSV data
            progressUI.updateStepProgress(10, "Reading CSV file...");
            List<PropertyVisibilityInfo> visibilityRules = CsvReader.read(csvPath, this::createVisibilityInfo);
            
            // Navigate to property visibility page
            progressUI.updateStepProgress(30, "Navigating to Property Visibility configuration");
            navigateToPropertyVisibility(driver, baseUrl);
            
            // Import the rules
            progressUI.updateStepProgress(50, "Importing property visibility rules");
            progressUI.setMainProgressMax(visibilityRules.size());
            importVisibilityRules(driver, visibilityRules, progressUI);
            
            completeAndHide(progressUI, "Successfully imported " + visibilityRules.size() + " property visibility rules");
            
        } catch (Exception e) {
            handleError(progressUI, e);
        }
    }
    
    private PropertyVisibilityInfo createVisibilityInfo(String[] fields) {
        PropertyVisibilityInfo info = new PropertyVisibilityInfo();
        if (fields.length >= 3) {
            info.part = fields[0];
            info.property = fields[1];
            info.value = fields[2];
        }
        return info;
    }
    
    private void navigateToPropertyVisibility(WebDriver driver, String baseUrl) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        
        if (!driver.getCurrentUrl().contains("/Home")) {
            driver.get(baseUrl + "/Home");
        }
        
        // Navigate through the UI to Property Visibility
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//a[contains(@href,'PricingAndConfig')]")));
        
        clickButton(driver, LocatorType.XPATH, 
            "//a[contains(@href,'DrawingBoardConfig')]", Screenshot.ON, 3);
        
        wait.until(ExpectedConditions.urlContains("DrawingBoardConfig"));
        
        // Click on Property Visibility tab
        clickButton(driver, LocatorType.XPATH, 
            "//a[contains(@class,'tab_inactive') and contains(@href,'PropertyVisibility')]", 
            Screenshot.ON, 3);
        
        wait.until(ExpectedConditions.urlContains("PropertyVisibility"));
        
        // Wait for the page to load completely
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("main_loading")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("th.propertyHeaderRowCell.firstCol")));
    }
    
    private void importVisibilityRules(WebDriver driver, List<PropertyVisibilityInfo> rules, ProgressUI progressUI) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        int processedCount = 0;
        
        for (int i = 0; i < rules.size() && !TestSuite.isTaskCancelled(); i++) {
            PropertyVisibilityInfo rule = rules.get(i);
            progressUI.updateMainProgress(i);
            progressUI.updateStepProgress(50 + (i * 50 / rules.size()), 
                "Processing rule " + (i + 1) + "/" + rules.size() + " (" + processedCount + " processed)");
            
            try {
                checkCancellation();
                processSingleRule(driver, wait, rule);
                processedCount++;
                progressUI.updateStatus("Processed: " + rule.part + " - " + rule.property);
                
            } catch (Exception e) {
                System.out.println("Error processing rule '" + rule.part + " - " + rule.property + "': " + e.getMessage());
                // Continue with next rule
            }
        }
        
        progressUI.updateStatus("Import complete: " + processedCount + " rules processed");
    }
    
    private void processSingleRule(WebDriver driver, WebDriverWait wait, PropertyVisibilityInfo rule) throws InterruptedException {
        checkCancellation();
        
        String cssSelector = "select.propertyRowCellSelect." + rule.part + "-" + rule.property + "-editor";
        
        try {
            // Find all dropdowns matching this part-property combination
            List<WebElement> dropdowns = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector(cssSelector)));
            
            if (dropdowns.isEmpty()) {
                System.out.println("No dropdowns found for: " + rule.part + "-" + rule.property);
                return;
            }
            
            // Process each dropdown
            for (WebElement dropdown : dropdowns) {
                checkCancellation();
                
                try {
                    // Scroll to the element
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", dropdown);
                    Thread.sleep(100);
                    
                    // Select the value
                    Select select = new Select(dropdown);
                    int valueIndex = Integer.parseInt(rule.value);
                    select.selectByIndex(valueIndex);
                    
                    Thread.sleep(50); // Brief pause between selections
                    
                } catch (Exception e) {
                    System.out.println("Failed to set dropdown for " + rule.part + "-" + rule.property + ": " + e.getMessage());
                    // Continue with next dropdown
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to process rule: " + rule.part + "-" + rule.property, e);
        }
    }
    
    // Data class for property visibility information
    private static class PropertyVisibilityInfo {
        String part;
        String property;
        String value;
    }
}