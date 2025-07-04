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
    
    public BasePartImporter(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
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
                fillCommonFields(item);
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
    
    private void clickAddButton() {
        WebElement addButton = wait.until(ExpectedConditions.elementToBeClickable(
            By.id("add_part_button")));
        addButton.click();
    }
    
    private void fillCommonFields(T item) {
        // Implement common field filling logic here
        // Example: enterTextById(wait, "part_no", item.getPartNo());
    }
    
    private void submitForm() {
        clickButtonById(driver, "part_dialog_submit_new");
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
            By.id("part_dialog_submit_new")));
    }
    
    private void handleError(Exception e) {
        System.out.println("Error importing item: " + e.getMessage());
        try {
            driver.findElement(By.cssSelector(".ui-dialog-titlebar-close")).click();
        } catch (Exception ex) {
            // Ignore if we can't close the dialog
        }
    }
}