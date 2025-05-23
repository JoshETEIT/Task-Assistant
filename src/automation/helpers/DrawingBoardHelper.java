package automation.helpers;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;

public class DrawingBoardHelper {

    public static Map<String, Map.Entry<String, String>> captureDrawingBoardSettings(WebDriver driver, WebDriverWait wait) {
        Map<String, Map.Entry<String, String>> settings = new LinkedHashMap<>();

        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.cssSelector(".property-table .property"), 2));

        List<WebElement> rows = driver.findElements(By.cssSelector(".property-table .property"));

        for (WebElement row : rows) {
            try {
                WebElement nameCell = row.findElement(By.cssSelector(".property-name"));
                String label = nameCell.getText().trim();

                WebElement valueCell = row.findElement(By.cssSelector(".property-value"));

                // Skip noisy button-based settings
                Set<String> buttonBased = Set.of("Divider Spacing", "Range Values", "Button Color");
                if (buttonBased.contains(label)) {
                    continue;
                }

                String value = "";
                String elementType = "";

                if (!valueCell.findElements(By.cssSelector("input[type='checkbox']")).isEmpty()) {
                    WebElement checkbox = valueCell.findElement(By.cssSelector("input[type='checkbox']"));
                    value = checkbox.isSelected() ? "true" : "false";
                    elementType = "Checkbox";
                } else if (!valueCell.findElements(By.tagName("select")).isEmpty()) {
                    WebElement select = valueCell.findElement(By.tagName("select"));
                    Select dropdown = new Select(select);
                    try {
                        WebElement selectedOption = dropdown.getFirstSelectedOption();
                        value = selectedOption.getText().trim();
                    } catch (Exception e) {
                        System.out.printf("⚠️ Setting '%s' (type: dropdown) had no selected value. Skipping.%n", label);
                        continue;
                    }
                    elementType = "Drop-down";
                } else {
                    value = valueCell.getText().trim();
                    if (value.isEmpty() || value.equalsIgnoreCase("No options selected")) {
                        continue;
                    }
                    elementType = "Text Field";
                }

                settings.put(label, Map.entry(value, elementType));

            } catch (Exception e) {
                System.out.println("❌ Error parsing property row: " + e.getMessage());
            }
        }

        return settings;
    }

    public static Map<String, Map<String, Map.Entry<String, String>>> captureAllDrawingTabSettings(WebDriver driver, WebDriverWait wait) {
        Map<String, Map<String, Map.Entry<String, String>>> allSettings = new LinkedHashMap<>();

        List<WebElement> tabs = driver.findElements(By.cssSelector(".tree-node"));

        for (int i = 0; i < tabs.size(); i++) {
            try {
                tabs = driver.findElements(By.cssSelector(".tree-node"));

                WebElement tab = tabs.get(i);
                String tabName = tab.findElement(By.className("tree-text")).getText().trim();

                if (tabName.isEmpty()) continue;

                tab.click();

                Map<String, Map.Entry<String, String>> settings = captureDrawingBoardSettings(driver, wait);
                allSettings.put(tabName, settings);

            } catch (Exception e) {
                System.out.printf("⚠️ Failed to capture settings for tab index %d: %s%n", i, e.getMessage());
            }
        }

        return allSettings;
    }
}
