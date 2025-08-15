package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import automation.ui.ProgressUI;
import automation.ui.AutomationUI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class UpdateIronmongeryDefaultsTask implements AutomationTask {
    
    @Override
    public String getName() {
        return "Update Ironmongery Defaults";
    }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        try {
            progressUI.showProgress("Updating Ironmongery Defaults", "Initializing...");
            progressUI.setMainProgressMax(1); // Will be updated when we know template count
            progressUI.setStepProgressMax(100);
            
            if (!navigateToTemplates(driver, baseUrl, progressUI)) {
                return;
            }
            
            List<WebElement> headers = getTemplateHeaders(driver);
            int[] selectedIndices = selectTemplatesToUpdate(headers);
            
            if (selectedIndices == null || selectedIndices.length == 0) {
                progressUI.updateStepProgress(100, "⏭️ No templates selected");
                return;
            }
            
            processSelectedTemplates(driver, headers, selectedIndices, progressUI);
            
            progressUI.updateStepProgress(100, "✅ Defaults updated");
        } catch (Exception e) {
            progressUI.updateStepProgress(100, "❌ Failed: " + e.getMessage());
            AutomationUI.showMessageDialog(
                null, 
                "Error updating defaults: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    private boolean navigateToTemplates(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        try {
            progressUI.updateStepProgress(5, "Navigating to templates");
            
            if (!driver.getCurrentUrl().contains("/Home")) {
                driver.get(baseUrl + "/Home");
            }
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[contains(@href,'DrawingBoardConfig')]")));
            
            progressUI.updateStepProgress(10, "Opening drawing board config");
            WebElement drawingBoardLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@href,'/PricingAndConfig/DrawingBoardConfig')]")));
            drawingBoardLink.click();
            
            wait.until(ExpectedConditions.urlContains("DrawingBoardConfig"));
            
            progressUI.updateStepProgress(15, "Opening template tab");
            WebElement templateTab = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@class,'tab_inactive') and contains(@href,'DrawingTemplate')]")));
            templateTab.click();
            
            wait.until(ExpectedConditions.urlContains("DrawingTemplate"));
            return true;
        } catch (Exception e) {
            progressUI.updateStepProgress(100, "❌ Navigation failed");
            return false;
        }
    }
    
    private List<WebElement> getTemplateHeaders(WebDriver driver) {
        return driver.findElements(By.cssSelector("div.header2, div.header3"));
    }
    
    private int[] selectTemplatesToUpdate(List<WebElement> headers) {
        List<WebElement> filteredHeaders = new ArrayList<>();
        for (WebElement header : headers) {
            String text = header.getText().trim();
            if (!text.isEmpty() && !text.equals("Drawing Template") && !text.contains("Main organisation")) {
                filteredHeaders.add(header);
            }
        }
        
        return AutomationUI.showMultiOptionDialog(
            null,
            "Select templates to update:",
            "Template Selection",
            filteredHeaders
        );
    }
    
    private void processSelectedTemplates(WebDriver driver, List<WebElement> headers, 
            int[] selectedIndices, ProgressUI progressUI) {
List<WebElement> filteredHeaders = new ArrayList<>();
for (WebElement header : headers) {
String text = header.getText().trim();
if (!text.isEmpty() && !text.equals("Drawing Template") && !text.contains("Main organisation")) {
filteredHeaders.add(header);
}
}

progressUI.setMainProgressMax(selectedIndices.length);
progressUI.updateStatus("Processing " + selectedIndices.length + " templates");

for (int i = 0; i < selectedIndices.length; i++) {
int index = selectedIndices[i];
progressUI.updateMainProgress(i);

if (index >= filteredHeaders.size()) continue;

WebElement header = filteredHeaders.get(index);
String headerText = header.getText().trim();

try {
progressUI.updateStepProgress(0, "Processing " + headerText);
((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView({block:'center'});", header);

// Fixed XPath expression - added missing closing bracket
String xpath = String.format(
".//a[starts-with(@href,'/SurveySystem/DrawingBoard/Template/')]" +
"[preceding-sibling::div[@class='header3'][1][normalize-space()='%s']]" +
"[not(./div[contains(@class,'drawing_deleted')])]",  // Added closing bracket here
headerText.replace("'", "\\'"));

List<WebElement> templates = new WebDriverWait(driver, Duration.ofSeconds(20))
.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath(xpath)));

progressUI.updateStatus("Processing " + templates.size() + " templates under: " + headerText);

for (int j = 0; j < templates.size(); j++) {
WebElement template = templates.get(j);
String currentUrl = driver.getCurrentUrl();

progressUI.updateStepProgress(j * 100 / templates.size(), 
"Processing template " + (j+1) + "/" + templates.size());

try {
((JavascriptExecutor)driver).executeScript(
"arguments[0].scrollIntoView({block:'center'});" +
"arguments[0].click();", 
template);

new WebDriverWait(driver, Duration.ofSeconds(20))
.until(ExpectedConditions.not(ExpectedConditions.urlToBe(currentUrl)));

updateTemplateDefaults(driver, progressUI);
driver.navigate().back();
new WebDriverWait(driver, Duration.ofSeconds(20))
.until(ExpectedConditions.urlToBe(currentUrl));

} catch (Exception e) {
System.out.println("Skipping template: " + e.getMessage());
if (!driver.getCurrentUrl().equals(currentUrl)) {
driver.navigate().back();
}
}
}

progressUI.updateStepProgress(100, "✅ Completed");
} catch (Exception e) {
progressUI.updateStepProgress(100, "❌ Failed");
System.out.println("Error with header '" + headerText + "': " + e.getMessage());
}
}
}
    
    private void updateTemplateDefaults(WebDriver driver, ProgressUI progressUI) throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            System.out.println("[DEBUG] Starting updateTemplateDefaults");
            progressUI.updateStepProgress(10, "Opening template");
            
            System.out.println("[DEBUG] Looking for 'Finish & Ironmongery' node");
            WebElement ironmongeryNode = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//span[@class='tree-text' and contains(text(),'Finish & Ironmongery')]")));
            System.out.println("[DEBUG] Found ironmongery node, clicking...");
            js.executeScript("arguments[0].click();", ironmongeryNode);
            
            System.out.println("[DEBUG] Waiting for property table to appear");
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div.property-list table.property-table")));

            progressUI.updateStepProgress(30, "Opening settings");
            
            System.out.println("[DEBUG] Looking for main ironmongery row");
            WebElement ironmongeryRow = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//tr[.//td[contains(@class, 'main_ironmongery')]]")));
            System.out.println("[DEBUG] Found ironmongery row");
                
            System.out.println("[DEBUG] Looking for settings window icon");
            WebElement windowIcon = ironmongeryRow.findElement(
                By.cssSelector("img.property-icon-more[title*='Opens dialog window']"));
            System.out.println("[DEBUG] Found window icon, clicking...");
            js.executeScript("arguments[0].click();", windowIcon);
        
            System.out.println("[DEBUG] Waiting for modal dialog to appear");
            WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.ui-dialog")));
            System.out.println("[DEBUG] Modal dialog is visible");
                
            progressUI.updateStepProgress(50, "Applying defaults");
            
            System.out.println("[DEBUG] Looking for default button");
            WebElement defaultButton = modal.findElement(
                By.xpath(".//button[@name='default_button' and contains(@class, 'side_button')]"));
            System.out.println("[DEBUG] Found default button, scrolling into view...");
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", defaultButton);
            Thread.sleep(500);
            System.out.println("[DEBUG] Clicking default button...");
            defaultButton.click();
            
            System.out.println("[DEBUG] Waiting for defaults to be applied");
            try {
                // Wait for any input to appear in the dialog
                wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div.ui-dialog input[type='text']")));
                System.out.println("[DEBUG] Input fields detected - defaults applied");
            } catch (Exception e) {
                System.out.println("[DEBUG] Failed to detect input fields after applying defaults");
                throw e;
            }
            
            // Additional safety wait
            Thread.sleep(500);
            
            progressUI.updateStepProgress(70, "Saving settings");
            
            System.out.println("[DEBUG] Looking for OK button");
            WebElement okButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath(".//button[@name='ok_button' and contains(@class, 'side_button')]")));
            System.out.println("[DEBUG] Found OK button, attempting to click...");
            
            // Scroll and click with JavaScript for reliability
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", okButton);
            Thread.sleep(300);
            System.out.println("[DEBUG] Executing JavaScript click on OK button");
            js.executeScript("arguments[0].click();", okButton);
            
            System.out.println("[DEBUG] Waiting for dialog to close");
            try {
                wait.until(ExpectedConditions.invisibilityOf(okButton));
                System.out.println("[DEBUG] Dialog closed successfully");
            } catch (Exception e) {
                System.out.println("[DEBUG] Dialog may not have closed - checking current URL");
                System.out.println("[DEBUG] Current URL: " + driver.getCurrentUrl());
            }
            
            Thread.sleep(1000);
            
            progressUI.updateStepProgress(80, "Saving template");

            System.out.println("[DEBUG] Looking for Save Drawing button");
            WebElement saveButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath(".//button[contains(@class,'drawing-board-button') and contains(.,'Save Drawing')]")));
            System.out.println("[DEBUG] Found Save button, clicking...");
            saveButton.click();

            System.out.println("[DEBUG] Waiting for save to complete (button to get disabled attribute)");
            try {
                // Wait for the disabled attribute to appear
                wait.until(ExpectedConditions.attributeToBe(saveButton, "disabled", "disabled"));
                System.out.println("[DEBUG] Save button disabled - save complete");
                
                // Additional verification that the button is actually disabled
                if (saveButton.isEnabled()) {
                    System.out.println("[WARNING] Button has disabled attribute but is still enabled!");
                } else {
                    System.out.println("[DEBUG] Button confirmed disabled - safe to proceed");
                }
                
            } catch (Exception e) {
                System.out.println("[ERROR] Failed while waiting for save to complete");
                System.out.println("[DEBUG] Current button state:");
                System.out.println("[DEBUG] - Enabled: " + saveButton.isEnabled());
                System.out.println("[DEBUG] - Disabled attribute: " + saveButton.getDomProperty("disabled"));
                System.out.println("[DEBUG] - Outer HTML: " + saveButton.getDomAttribute("outerHTML"));
                
                throw new RuntimeException("Save operation did not complete properly", e);
            }

            progressUI.updateStepProgress(100, "✅ Saved");
            System.out.println("[DEBUG] Proceeding to next step");
            
        } catch (Exception e) {
            System.out.println("[ERROR] Exception occurred during template update: " + e.getMessage());
            progressUI.updateStepProgress(100, "❌ Failed");
            throw e;
        }
    }
}