package automation;

import java.io.*;
import java.util.*;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;
import static automation.helpers.ElementHelper.*;

public class GlassPartImport {
    
    public static class GlassPartItem {
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

    public static ArrayList<GlassPartItem> CSVReader(String csvPath) throws Exception {
        ArrayList<GlassPartItem> itemList = new ArrayList<>();
        try (Scanner sc = new Scanner(new File(csvPath))) {
            if (sc.hasNextLine()) sc.nextLine(); // Skip header
            
            while (sc.hasNextLine()) {
                String[] fields = sc.nextLine().split(",");
                GlassPartItem item = new GlassPartItem();
                item.setPartNo(fields[0]);
                item.setPartName(fields[1]);
                item.setpUnit(fields[2]);
                item.setCost(fields[3]);
                item.setObscure(fields.length > 4 ? fields[4] : "No");
                itemList.add(item);
            }
        }
        return itemList;
    }

    public static void importGlassParts(ArrayList<GlassPartItem> items,
            WebDriver driver,
            String baseUrl) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Actions actions = new Actions(driver);
        
        driver.get(baseUrl + "/PricingAndConfig/PartList/GL");

        for (GlassPartItem item : items) {
            try {
                // 1. Scroll to top and click Add Part button
                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
                
                int attempts = 0;
                while (attempts < 3) {
                    try {
                        WebElement addButton = wait.until(ExpectedConditions.elementToBeClickable(
                            By.id("add_part_button")));
                        ((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView(true);", addButton);
                        addButton.click();
                        break;
                    } catch (Exception e) {
                        attempts++;
                        if (attempts == 3) throw e;
                        Thread.sleep(1000);
                    }
                }

                // 2. Fill basic fields
                enterTextById(wait, "part_no", item.getPartNo());
                enterTextById(wait, "part_name", item.getPartName());

                // 3. Select dropdowns
                selectDropdownByVisibleText(driver, "part_unit_name", item.getpUnit());
                selectDropdownByVisibleText(driver, "part_allocated_unit_name", "each");

                // 4. Set allocated amount
                enterTextById(wait, "part_allocated_amount_in_purchase_unit", "1");

                // 5. Enter cost
                enterTextById(wait, "part_cost", item.getCost());

                // 6. Handle obscure glass
                if ("yes".equalsIgnoreCase(item.getObscure())) {
                    selectCheckboxOrRadioButton(driver, "part_is_obscure_glass");
                }

                // 7. Submit part
                clickButtonById(driver, "part_dialog_submit_new");
                
                // 8. Wait for dialog to close and scroll to top
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.id("part_dialog_submit_new")));
                
                // Scroll to top using multiple methods
                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
                actions.sendKeys(Keys.HOME).perform();
                Thread.sleep(500); // Small pause

            } catch (Exception e) {
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