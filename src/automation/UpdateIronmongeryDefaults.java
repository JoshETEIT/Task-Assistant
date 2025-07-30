package automation;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import automation.ui.AutomationUI;
import automation.ui.ProgressUI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class UpdateIronmongeryDefaults {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final ProgressUI progressUI;

    public UpdateIronmongeryDefaults(WebDriver driver, ProgressUI progressUI) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        this.progressUI = progressUI;
    }

    public boolean updateDefaults(String baseUrl) {
        try {
            if (!driver.getCurrentUrl().contains("/Home")) {
                driver.get(baseUrl + "/Home");
            }
            
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[contains(@href,'DrawingBoardConfig')]")));
            
            WebElement drawingBoardLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@href,'/PricingAndConfig/DrawingBoardConfig')]")));
            drawingBoardLink.click();
            
            wait.until(ExpectedConditions.urlContains("DrawingBoardConfig"));
            
            WebElement templateTab = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@class,'tab_inactive') and contains(@href,'DrawingTemplate')]")));
            templateTab.click();
            
            wait.until(ExpectedConditions.urlContains("DrawingTemplate"));

            List<WebElement> headers = driver.findElements(By.cssSelector("div.header2, div.header3"));

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

            List<WebElement> filteredHeaders = new ArrayList<>();
            for (WebElement header : headers) {
                String text = header.getText().trim();
                if (!text.isEmpty() && !text.equals("Drawing Template") && !text.contains("Main organisation")) {
                    filteredHeaders.add(header);
                }
            }

            // === PROGRESS TRACKING ADDED ===
            int totalTemplates = selectedIndices.length;
            progressUI.setMainProgressMax(totalTemplates);
            progressUI.setStepProgressMax(100);
            progressUI.updateStatus("Processing " + totalTemplates + " templates");
            // ===============================

            for (int i = 0; i < selectedIndices.length; i++) {
                int index = selectedIndices[i];
                
                // === PROGRESS TRACKING ADDED ===
                progressUI.updateMainProgress(i);
                progressUI.updateStepProgress(0, "Starting template " + (i+1));
                // ===============================
                
                if (index >= filteredHeaders.size()) continue;
                
                WebElement header = filteredHeaders.get(index);
                String headerText = header.getText().trim();
                
                try {
                    ((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView({block:'center'});", header);
                    
                    String xpath = String.format(
                        ".//a[starts-with(@href,'/SurveySystem/DrawingBoard/Template/')]" +
                        "[preceding-sibling::div[@class='header3'][1][normalize-space()='%s']]" +
                        "[not(./div[contains(@class,'drawing_deleted')])]",
                        headerText.replace("'", "\\'"));
                    
                    List<WebElement> templates = wait.until(ExpectedConditions
                        .presenceOfAllElementsLocatedBy(By.xpath(xpath)));
                    
                    System.out.println("\nProcessing " + templates.size() + " templates under: " + headerText);
                    
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
                            
                            wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(currentUrl)));
                            
                            processTemplate(driver);
                            driver.navigate().back();
                            wait.until(ExpectedConditions.urlToBe(currentUrl));
                            
                        } catch (Exception e) {
                            System.out.println("Skipping template: " + e.getMessage());
                            
                            // === NOW currentUrl IS ACCESSIBLE ===
                            if (!driver.getCurrentUrl().equals(currentUrl)) {
                                driver.navigate().back();
                            }
                        }
                    }
                    
                    // === PROGRESS TRACKING ADDED ===
                    progressUI.updateStepProgress(100, "✅ Completed");
                    // ===============================
                } catch (Exception e) {
                    System.out.println("Error with header '" + headerText + "': " + e.getMessage());
                    
                    // === PROGRESS TRACKING ADDED ===
                    progressUI.updateStepProgress(100, "❌ Failed");
                    // ===============================
                }
            }

            System.out.println("Finished processing all selected templates");
            return true;
        } catch (Exception e) {
            System.out.println("Error updating templates: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            progressUI.close();
        }
    }
    
    private void processTemplate(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(10, "Opening template");
            // ===============================
            
            WebElement ironmongeryNode = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//span[@class='tree-text' and contains(text(),'Finish & Ironmongery')]")));
                js.executeScript("arguments[0].click();", ironmongeryNode);
            
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div.property-list table.property-table")));

            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(30, "Opening settings");
            // ===============================
            
            WebElement ironmongeryRow = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//tr[.//td[contains(@class, 'main_ironmongery')]]"))); // Fixed missing bracket
                
                WebElement windowIcon = ironmongeryRow.findElement(
                    By.cssSelector("img.property-icon-more[title*='Opens dialog window']"));
                
                js.executeScript("arguments[0].click();", windowIcon);
            
            WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.ui-dialog")));
                
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(50, "Applying defaults");
            // ===============================
            
            WebElement defaultButton = modal.findElement(
                By.xpath(".//button[@name='default_button' and contains(@class, 'side_button')]"));
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", defaultButton);
            Thread.sleep(500);
            defaultButton.click();
            
            wait.until(d -> {
                try {
                    WebElement activeElement = driver.switchTo().activeElement();
                    return "input".equals(activeElement.getTagName()) && 
                           "text".equals(activeElement.getAttribute("type"));
                } catch (Exception e) {
                    return false;
                }
            });
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(70, "Saving settings");
            // ===============================
            
            WebElement okButton = modal.findElement(
                By.xpath(".//button[@name='ok_button' and contains(@class, 'side_button')]"));
            okButton.click();
            
            Thread.sleep(1000);
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(80, "Saving template");
            // ===============================
            
            WebElement saveButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath(".//button[contains(@class,'drawing-board-button') and contains(.,'Save Drawing')]")));
            saveButton.click();
            
            wait.until(ExpectedConditions.not(ExpectedConditions.elementToBeClickable(saveButton)));
            Thread.sleep(1000);
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(100, "✅ Saved");
            // ===============================
            
        } catch (Exception e) {
            System.out.println("Error updating template: " + e.getMessage());
            e.printStackTrace();
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(100, "❌ Failed");
            // ===============================
        }
    }
}