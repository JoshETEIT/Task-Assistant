package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import automation.helpers.CsvReader;
import automation.ui.ProgressUI;

import static automation.helpers.ElementHelper.*;
import java.util.List;
import java.time.Duration;

public class GlassPartImportTask extends TaskBase {
    
    @Override
    public String getName() {
        return "Import Glass Parts";
    }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        initializeProgress(progressUI, 1); // Will be updated when we know item count
        
        try {
            progressUI.updateStatus("Selecting CSV file...");
            String csvPath = getFile(progressUI, "Glass Parts CSV");
            if (csvPath == null) {
                progressUI.showCancellation();
                return;
            }

            progressUI.updateStatus("Reading CSV...");
            List<GlassPartItem> items = CsvReader.read(csvPath, this::createItem);
            
            progressUI.updateStatus("Importing items...");
            progressUI.setMainProgressMax(items.size());
            performImport(items, driver, baseUrl, progressUI);
            
            complete(progressUI, "Import completed");
        } catch (Exception e) {
            handleError(progressUI, e);
        }
    }

    private GlassPartItem createItem(String[] fields) {
        GlassPartItem item = new GlassPartItem();
        item.setPartNo(fields[0]);
        item.setPartName(fields[1]);
        item.setpUnit(fields[2]);
        item.setCost(fields[3]);
        item.setObscure(fields.length > 4 ? fields[4] : "No");
        return item;
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

    private void performImport(List<GlassPartItem> items, WebDriver driver, 
                            String baseUrl, ProgressUI progressUI) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Actions actions = new Actions(driver);
        
        driver.get(baseUrl + "/PricingAndConfig/PartList/GL");
        wait.until(ExpectedConditions.urlContains("/PricingAndConfig/PartList/GL"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("add_part_button")));
        
        for (int i = 0; i < items.size(); i++) {
            GlassPartItem item = items.get(i);
            progressUI.updateMainProgress(i);
            progressUI.updateStepProgress(0, "Processing " + item.getPartNo());
            
            try {
                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
                clickAddPartButtonWithRetry(driver, wait);
                
                enterText(wait, LocatorType.ID, "part_no", item.getPartNo());
                enterText(wait, LocatorType.ID, "part_name", item.getPartName());
                selectDropdownByVisibleText(driver, "part_unit_name", item.getpUnit());
                selectDropdownByVisibleText(driver, "part_allocated_unit_name", "each");
                enterText(wait, LocatorType.ID, "part_allocated_amount_in_purchase_unit", "1");
                enterText(wait, LocatorType.ID, "part_cost", item.getCost());

                if ("yes".equalsIgnoreCase(item.getObscure())) {
                    selectCheckboxOrRadioButton(driver, "part_is_obscure_glass");
                }

                WebElement submitButton = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("part_dialog_submit_new")));
                ((JavascriptExecutor)driver).executeScript("arguments[0].click();", submitButton);
                
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.id("part_dialog_submit_new")));
                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
                actions.sendKeys(Keys.HOME).perform();
                
                progressUI.updateStepProgress(100, "✅ Part added");
            } catch (Exception e) {
                progressUI.updateStepProgress(100, "❌ Failed: " + e.getMessage());
                System.out.println("Error adding part " + item.getPartNo() + ": " + e.getMessage());
                
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
                WebElement addButton = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("add_part_button")));
                ((JavascriptExecutor)driver).executeScript(
                    "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", 
                    addButton);
                ((JavascriptExecutor)driver).executeScript("arguments[0].click();", addButton);
                wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("part_dialog_submit_new")));
                return;
            } catch (Exception e) {
                attempts++;
                if (attempts == 3) throw e;
                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
                driver.navigate().refresh();
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("add_part_button")));
            }
        }
    }
}