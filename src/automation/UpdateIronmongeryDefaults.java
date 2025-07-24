package automation;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import automation.ui.AutomationUI;
import javax.swing.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class UpdateIronmongeryDefaults {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public UpdateIronmongeryDefaults(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public boolean updateDefaults(String baseUrl) {
        try {
            // 1. Navigate to Home page if not already there
            if (!driver.getCurrentUrl().contains("/Home")) {
                driver.get(baseUrl + "/Home");
            }
            
            // 2. Wait for the page to fully load
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[contains(@href,'DrawingBoardConfig')]")));
            
            // 3. Click the DrawingBoard Config link
            WebElement drawingBoardLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@href,'/PricingAndConfig/DrawingBoardConfig')]")));
            drawingBoardLink.click();
            
            // 4. Wait for DrawingBoard Config page to load
            wait.until(ExpectedConditions.urlContains("DrawingBoardConfig"));
            
            // 5. Click the Drawing Template tab
            WebElement templateTab = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@class,'tab_inactive') and contains(@href,'DrawingTemplate')]")));
            templateTab.click();
            
            // 6. Final wait to confirm we're on the right page
            wait.until(ExpectedConditions.urlContains("DrawingTemplate"));

         // Collect all header elements
         List<WebElement> headers = driver.findElements(By.cssSelector("div.header2, div.header3"));

         // Show dialog with the WebElements
         int[] selectedIndices = AutomationUI.showMultiOptionDialog(
             null,
             "Select templates to update:",
             "Template Selection",
             headers
         );

         if (selectedIndices == null || selectedIndices.length == 0) {
             System.out.println("No templates selected. Operation cancelled.");
             return false;
         }

         // Create filtered list of headers (including group headers for reference)
         List<WebElement> filteredHeaders = new ArrayList<WebElement>();
         List<String> filteredTitles = new ArrayList<String>();
         List<Boolean> isGroup = new ArrayList<Boolean>();

         for (WebElement header : headers) {
             String text = header.getText().trim();
             if (text.isEmpty() || text.equals("Drawing Template") || text.contains("Main organisation")) {
                 continue;
             }
             filteredHeaders.add(header);
             filteredTitles.add(text);
             isGroup.add(text.endsWith("Templates") && "header2".equals(header.getAttribute("class")));
         }

         // Process selected templates
         for (int index : selectedIndices) {
             String headerTitle = filteredTitles.get(index);
             System.out.println("Processing template: " + headerTitle);
             
             WebElement currentHeader = filteredHeaders.get(index);
             List<WebElement> templateLinks = currentHeader.findElements(
                 By.xpath("./following-sibling::a[contains(@href,'/Template/')]"));
             
             for (WebElement link : templateLinks) {
                 processTemplate(link);
             }
         }
            
            System.out.println("Successfully updated selected templates");
            return true;

        } catch (Exception e) {
            System.out.println("Error updating templates: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void processTemplate(WebElement templateLink) {
        try {
            // Scroll to the template link
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", 
                templateLink);
            
            // Click the template link
            templateLink.click();
            
            // Wait for template page to load
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("div.template_settings")));
            
            // Update ironmongery defaults for this template
            updateTemplateDefaults();
            
            // Go back to templates list
            driver.navigate().back();
            wait.until(ExpectedConditions.urlContains("DrawingTemplate"));
            
        } catch (Exception e) {
            System.out.println("Error processing template: " + e.getMessage());
        }
    }
    
    private void updateTemplateDefaults() {
        try {
            // 1. Find and click the ironmongery defaults button
            WebElement ironmongeryButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(),'Ironmongery Defaults')]")));
            ironmongeryButton.click();
            
            // 2. Wait for the modal dialog to appear
            WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.ui-dialog")));
            
            // 3. Update all dropdowns in the modal
            List<WebElement> dropdowns = modal.findElements(By.tagName("select"));
            for (WebElement dropdown : dropdowns) {
                Select select = new Select(dropdown);
                if (select.getOptions().size() > 0) {
                    // Select the first non-empty option
                    select.selectByIndex(1); // Skip the first empty option if exists
                }
            }
            
            // 4. Click the save button
            WebElement saveButton = modal.findElement(
                By.xpath(".//button[contains(text(),'Save')]"));
            saveButton.click();
            
            // 5. Wait for the modal to close
            wait.until(ExpectedConditions.invisibilityOf(modal));
            
            System.out.println("Updated ironmongery defaults for template");
            
        } catch (Exception e) {
            System.out.println("Error updating template defaults: " + e.getMessage());
            // Try to close the modal if it's still open
            try {
                driver.findElement(By.cssSelector("div.ui-dialog-titlebar-close")).click();
            } catch (Exception ex) {
                // Ignore if we can't close it
            }
        }
    }
}