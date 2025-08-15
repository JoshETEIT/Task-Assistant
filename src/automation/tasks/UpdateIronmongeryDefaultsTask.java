package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import automation.ui.ProgressUI;
import automation.helpers.InteractionHelper;
import automation.helpers.ProgressTracker;
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
            // DON'T call showProgress() here - use the existing UI
            progressUI.updateStatus("Initializing ironmongery defaults update");
            progressUI.setMainProgressMax(1); // Will update when we know template count
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
            throw e;
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

                String xpath = String.format(
                    ".//a[starts-with(@href,'/SurveySystem/DrawingBoard/Template/')]" +
                    "[preceding-sibling::div[@class='header3'][1][normalize-space()='%s']]" +
                    "[not(./div[contains(@class,'drawing_deleted')])]",
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
        
        InteractionHelper interaction = new InteractionHelper(js, true, "#FFBF00");
        ProgressTracker progress = new ProgressTracker(progressUI, true);

        try {
            progress.updateProgress(10, "Opening template");
            WebElement ironmongeryNode = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[@class='tree-text' and contains(text(),'Finish & Ironmongery')]")));
            interaction.performAction(ironmongeryNode, "click");
            interaction.markCompleted(ironmongeryNode);

            progress.updateProgress(20, "Loading properties");
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("div.property-list table.property-table")));

            progress.updateProgress(30, "Opening settings");
            WebElement ironmongeryRow = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//tr[.//td[contains(@class, 'main_ironmongery')]]")));
            
            WebElement windowIcon = ironmongeryRow.findElement(
                By.cssSelector("img.property-icon-more[title*='Opens dialog window']"));
            interaction.performAction(windowIcon, "click");
            interaction.markCompleted(windowIcon);

            progress.updateProgress(40, "Processing dialog");
            WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.ui-dialog")));
            
            progress.updateProgress(50, "Applying defaults");
            WebElement defaultButton = modal.findElement(
                By.xpath(".//button[@name='default_button' and contains(@class, 'side_button')]"));
            interaction.performAction(defaultButton, "click");
            interaction.markCompleted(defaultButton);

            wait.until(d -> {
                WebElement activeElement = driver.switchTo().activeElement();
                return "input".equals(activeElement.getTagName()) && 
                       "text".equals(activeElement.getDomProperty("type"));
            });

            progress.updateProgress(70, "Confirming changes");
            WebElement okButton = modal.findElement(
                By.xpath(".//button[@name='ok_button' and contains(@class, 'side_button')]"));
            interaction.performAction(okButton, "click");
            interaction.markCompleted(okButton);
            
            progress.updateProgress(80, "Saving template");
            WebElement saveButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath(".//button[contains(@class,'drawing-board-button') and contains(.,'Save Drawing')]")));
            interaction.performAction(saveButton, "click");
            
            wait.until(ExpectedConditions.not(ExpectedConditions.elementToBeClickable(saveButton)));
            interaction.markCompleted(saveButton);
            
            progress.updateProgress(100, "✅ Saved");
            
        } catch (Exception e) {
            js.executeScript("document.body.style.border='4px solid red';");
            progress.updateProgress(100, "❌ Failed");
            throw e;
        }
    }
}