package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import automation.helpers.CsvReader;
import automation.helpers.ElementHelper.LocatorType;
import automation.helpers.ElementHelper.Screenshot;
import automation.ui.ProgressUI;

import static automation.helpers.ElementHelper.*;
import java.time.Duration;
import java.util.List;

public class IronmongeryImportTask extends TaskBase {
    
    @Override
    public String getName() {
        return "Import Ironmongery";
    }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        initializeProgress(progressUI, 1); // Will be updated when we know item count
        
        try {
            progressUI.updateStatus("Selecting CSV file...");
            String csvPath = getFile(progressUI, "Ironmongery CSV");
            if (csvPath == null) {
                cancelAndHide(progressUI);
                return;
            }

            progressUI.updateStatus("Reading CSV...");
            List<IronmongeryItem> items = CsvReader.read(csvPath, this::createItem);
            
            progressUI.updateStatus("Importing items...");
            progressUI.setMainProgressMax(items.size());
            performImport(items, driver, baseUrl, progressUI);
            
            completeAndHide(progressUI, "Import completed");
        } catch (Exception e) {
            handleError(progressUI, e);
        }
    }

    private IronmongeryItem createItem(String[] fields) {
        IronmongeryItem item = new IronmongeryItem();
        item.setPartNo(fields[0]);
        item.setName(fields[1]);
        item.setCost(fields[2]);
        item.setUnit(fields[3]);
        item.setType(fields[4]);
        item.setNotes(fields.length > 5 ? fields[5] : "");
        return item;
    }

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

    private void performImport(List<IronmongeryItem> items, WebDriver driver, 
                             String baseUrl, ProgressUI progressUI) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Actions actions = new Actions(driver);
        
        driver.get(baseUrl + "/PricingAndConfig/PartList/IM");
        
        for (int i = 0; i < items.size(); i++) {
            IronmongeryItem item = items.get(i);
            progressUI.updateMainProgress(i);
            progressUI.updateStepProgress(0, "Processing " + item.getPartNo());
            
            try {
                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
                WebElement addButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("add_part_button")));
                addButton.click();
                
                enterText(wait, LocatorType.ID, "part_no", item.getPartNo());
                enterText(wait, LocatorType.ID, "part_name", item.getName());
                selectDropdownByVisibleText(driver, "part_unit_name", item.getUnit());
                selectDropdownByVisibleText(driver, "part_allocated_unit_name", "each");
                
                if (item.getUnit().equalsIgnoreCase("pair") || 
                    item.getUnit().equalsIgnoreCase("set") ||
                    item.getUnit().equalsIgnoreCase("roll")) {
                    enterText(wait, LocatorType.ID, "part_allocated_amount_in_purchase_unit", "1");
                }
                
                enterText(wait, LocatorType.ID, "part_cost", item.getCost());
                clickButton(driver, LocatorType.ID, "part_dialog_submit_new", Screenshot.ON, 3);
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.id("part_dialog_submit_new")));
                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
                actions.sendKeys(Keys.HOME).perform();
                Thread.sleep(500);
                
                progressUI.updateStepProgress(100, "✅ Part added");
            } catch (Exception e) {
                progressUI.updateStepProgress(100, "❌ Failed: " + e.getMessage());
                System.out.println("Error adding part " + item.getPartNo() + ": " + e.getMessage());
                
                try {
                    driver.findElement(By.cssSelector(".ui-dialog-titlebar-close")).click();
                } catch (Exception ex) {
                    // Ignore if we can't close it
                }
            }
        }
    }
}