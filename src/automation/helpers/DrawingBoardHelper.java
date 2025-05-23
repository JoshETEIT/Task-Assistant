package automation.helpers;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.util.*;

public class DrawingBoardHelper {

    public static Map<String, String> captureSettings(WebDriver driver, WebDriverWait wait) {
        Map<String, String> settings = new LinkedHashMap<>();
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.cssSelector(".property-table .property"), 2));

        List<WebElement> rows = driver.findElements(By.cssSelector(".property-table .property"));

        for (WebElement row : rows) {
            try {
                String label = row.findElement(By.cssSelector(".property-name")).getText().trim();
                WebElement valueCell = row.findElement(By.cssSelector(".property-value"));

                Set<String> buttonBased = Set.of("Divider Spacing", "Range Values", "Button Color");
                if (buttonBased.contains(label)) continue;

                if (!valueCell.findElements(By.cssSelector("input[type='checkbox']")).isEmpty()) {
                    WebElement checkbox = valueCell.findElement(By.cssSelector("input[type='checkbox']"));
                    settings.put(label, checkbox.isSelected() ? "true" : "false");
                } else if (!valueCell.findElements(By.tagName("select")).isEmpty()) {
                    WebElement select = valueCell.findElement(By.tagName("select"));
                    Select dropdown = new Select(select);
                    try {
                        String selectedText = dropdown.getFirstSelectedOption().getText().trim();
                        settings.put(label, selectedText);
                    } catch (Exception ignored) {}
                } else {
                    String text = valueCell.getText().trim();
                    if (!text.isEmpty() && !text.equalsIgnoreCase("No options selected")) {
                        settings.put(label, text);
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Error parsing property row: " + e.getMessage());
            }
        }

        return settings;
    }

    public static Map<String, Map<String, String>> captureAllDrawingTabSettings(WebDriver driver, WebDriverWait wait) {
        Map<String, Map<String, String>> allSettings = new LinkedHashMap<>();
        List<WebElement> tabs = driver.findElements(By.cssSelector(".tree-node"));

        for (int i = 0; i < tabs.size(); i++) {
            try {
                tabs = driver.findElements(By.cssSelector(".tree-node"));
                WebElement tab = tabs.get(i);
                String tabName = tab.findElement(By.className("tree-text")).getText().trim();
                if (tabName.isEmpty()) continue;

                tab.click();
                allSettings.put(tabName, captureSettings(driver, wait));
            } catch (Exception e) {
                System.out.printf("⚠️ Failed to capture settings for tab index %d: %s%n", i, e.getMessage());
            }
        }

        return allSettings;
    }

    public static void applySettings(WebDriver driver, Map<String, String> settings) {
        List<WebElement> rows = driver.findElements(By.cssSelector(".property-table .property"));

        for (WebElement row : rows) {
            try {
                String label = row.findElement(By.cssSelector(".property-name")).getText().trim();
                if (!settings.containsKey(label)) continue;

                String desiredValue = settings.get(label);
                WebElement valueCell = row.findElement(By.cssSelector(".property-value"));

                if (!valueCell.findElements(By.tagName("select")).isEmpty()) {
                    new Select(valueCell.findElement(By.tagName("select")))
                            .selectByVisibleText(desiredValue);
                } else if (!valueCell.findElements(By.cssSelector("input[type='checkbox']")).isEmpty()) {
                    WebElement checkbox = valueCell.findElement(By.cssSelector("input[type='checkbox']"));
                    boolean shouldBeChecked = desiredValue.equalsIgnoreCase("true");
                    if (checkbox.isEnabled() && checkbox.isSelected() != shouldBeChecked) {
                        checkbox.click();
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Could not apply setting for row: " + e.getMessage());
            }
        }
    }
}
