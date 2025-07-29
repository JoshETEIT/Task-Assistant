package automation;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import automation.ui.AutomationUI;
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

            // Show dialog and get selections
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

            // Create filtered list of selectable headers
            List<WebElement> filteredHeaders = new ArrayList<>();
            List<String> filteredTitles = new ArrayList<>();
            for (WebElement header : headers) {
                String text = header.getText().trim();
                if (!text.isEmpty() && !text.equals("Drawing Template") && !text.contains("Main organisation")) {
                    filteredHeaders.add(header);
                    filteredTitles.add(text);
                }
            }

            // Process selected templates
            processSelectedTemplates(selectedIndices, filteredHeaders, driver);

            System.out.println("Finished processing all selected templates");
            return true;

        } catch (Exception e) {
            System.out.println("Error updating templates: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void processSelectedTemplates(int[] selectedIndices, List<WebElement> filteredHeaders, WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String currentUrl = driver.getCurrentUrl();

        for (int index : selectedIndices) {
            if (index >= filteredHeaders.size()) continue;
            
            WebElement header = filteredHeaders.get(index);
            String headerText = header.getText().trim();
            
            try {
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", header);
                
                String xpath = String.format(
                    ".//a[starts-with(@href,'/SurveySystem/DrawingBoard/Template/')]" +
                    "[preceding-sibling::div[@class='header3'][1][normalize-space()='%s']]" +
                    "[not(./div[contains(@class,'drawing_deleted')])]",
                    headerText.replace("'", "\\'"));
                
                List<WebElement> templates = wait.until(ExpectedConditions
                    .presenceOfAllElementsLocatedBy(By.xpath(xpath)));
                
                System.out.println("\nProcessing " + templates.size() + " templates under: " + headerText);
                
                for (WebElement template : templates) {
                    try {
                        js.executeScript(
                            "arguments[0].scrollIntoView({block:'center'});" +
                            "arguments[0].click();", 
                            template);
                        
                        wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(currentUrl)));
                        System.out.println("Processing: " + driver.getCurrentUrl());
                        
                        processTemplate(driver);
                        driver.navigate().back();
                        wait.until(ExpectedConditions.urlToBe(currentUrl));
                        
                    } catch (Exception e) {
                        System.out.println("Skipping template: " + e.getMessage());
                        if (!driver.getCurrentUrl().equals(currentUrl)) driver.navigate().back();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error with header '" + headerText + "': " + e.getMessage());
            }
        }
    }
    
    private void processTemplate(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            // 1. Click Finish & Ironmongery node
            WebElement ironmongeryNode = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[@class='tree-text' and contains(text(),'Finish & Ironmongery')]")));
            js.executeScript("arguments[0].style.border='2px solid yellow';", ironmongeryNode);
            js.executeScript("arguments[0].click();", ironmongeryNode);
            System.out.println("Clicked Finish & Ironmongery node");

            // 2. Wait for the property editor to load
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("div.property-list table.property-table")));

            // 3. Find and click the Ironmongery window icon
            WebElement ironmongeryRow = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//tr[.//td[contains(@class, 'main_ironmongery')]]")));
            WebElement windowIcon = ironmongeryRow.findElement(
                By.cssSelector("img.property-icon-more[title*='Opens dialog window']"));
            
            js.executeScript("arguments[0].style.border='2px solid orange';", windowIcon);
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", windowIcon);
            Thread.sleep(500);
            js.executeScript("arguments[0].click();", windowIcon);
            System.out.println("Clicked Ironmongery window icon");

            // 4. Process the dialog
            WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.ui-dialog")));
                
            // Click Default button
            WebElement defaultButton = modal.findElement(
                By.xpath(".//button[@name='default_button' and contains(@class, 'side_button')]"));
            js.executeScript("arguments[0].style.border='2px solid green';", defaultButton);
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", defaultButton);
            Thread.sleep(500);
            defaultButton.click();
            System.out.println("Clicked Default button");
            
            // Wait for cursor to move to a text field
            wait.until(d -> {
                try {
                    WebElement activeElement = driver.switchTo().activeElement();
                    return "input".equals(activeElement.getTagName()) && 
                           "text".equals(activeElement.getAttribute("type"));
                } catch (Exception e) {
                    return false;
                }
            });
            System.out.println("Detected cursor moved to text field - default action complete");
            
            // Click OK button (without scrolling)
            WebElement okButton = modal.findElement(
                By.xpath(".//button[@name='ok_button' and contains(@class, 'side_button')]"));
            js.executeScript("arguments[0].style.border='2px solid blue';", okButton);
            okButton.click();
            System.out.println("Clicked OK button");
            
            // Wait 1 second after clicking OK
            Thread.sleep(1000);
            
            // Find and click the Save button
            WebElement saveButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath(".//button[contains(@class,'drawing-board-button') and contains(.,'Save Drawing')]")));
            
            js.executeScript("arguments[0].style.border='2px solid orange';", saveButton);
            saveButton.click();
            System.out.println("Clicked Save button");
            
            // Wait until Save button becomes disabled (greyed out)
            wait.until(ExpectedConditions.not(ExpectedConditions.elementToBeClickable(saveButton)));
            System.out.println("Save button disabled - save operation complete");
            
            // Additional wait for safety
            Thread.sleep(1000);
            
        } catch (Exception e) {
            System.out.println("Error updating template: " + e.getMessage());
            e.printStackTrace();
        }
    }
}