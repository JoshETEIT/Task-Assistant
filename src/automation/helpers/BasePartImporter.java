package automation.helpers;

import java.io.*;
import java.util.ArrayList;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;
import static automation.helpers.ElementHelper.*;

public abstract class BasePartImporter<T> {
    
    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final ScreenshotHandler screenshotHandler;
    
    public BasePartImporter(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        this.screenshotHandler = new ScreenshotHandler(driver);
    }
    
    protected abstract T createItem(String[] fields);
    protected abstract String getImportUrl();
    protected abstract void fillItemSpecificFields(T item);

    public ArrayList<T> CSVReader(String csvPath) throws Exception {
        ArrayList<T> itemList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                itemList.add(createItem(line.split(",")));
            }
        }
        return itemList;
    }

    public void importItems(ArrayList<T> items, String baseUrl) {
        driver.get(baseUrl + getImportUrl());
        
        for (T item : items) {
            try {
                scrollToTop();
                clickAddButton();
                fillItemSpecificFields(item);
                submitForm();
            } catch (Exception e) {
                handleError(e);
            }
        }
    }
    
    private void scrollToTop() {
        ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
    }
    
    protected void clickAddButton() {
        WebElement addButton = wait.until(ExpectedConditions.elementToBeClickable(
            By.id("add_part_button")));
        
        try {
            addButton.click();
        } catch (ElementClickInterceptedException e) {
            // Take screenshot when interception occurs
            screenshotHandler.screenshot("click_intercepted", addButton);
            
            // Fallback to JavaScript click
            System.out.println("⚠️ Click intercepted, using JavaScript fallback");
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addButton);
        }
    }
    
    private void submitForm() {
        clickButtonById(driver, "part_dialog_submit_new");
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
            By.id("part_dialog_submit_new")));
    }
    
    private void handleError(Exception e) {
        System.out.println("Error importing item: " + e.getMessage());
        
        // Take screenshot on error
        screenshotHandler.screenshotOnError(e, "import_error");
        
        try {
            driver.findElement(By.cssSelector(".ui-dialog-titlebar-close")).click();
        } catch (Exception ex) {
            // Ignore if we can't close the dialog
        }
    }
}