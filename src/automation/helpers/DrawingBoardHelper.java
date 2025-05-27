package automation.helpers;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;
import java.util.NoSuchElementException;
import java.util.AbstractMap;

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

    // ▼▼▼ UI-setting helpers ▼▼▼

    public static boolean enterTextByLabel(WebDriver driver, String label, String text) {
        try {
            WebElement row = findRowByLabel(driver, label);
            WebElement input = row.findElement(By.cssSelector("input[type='text'], textarea"));
            input.clear();
            input.sendKeys(text);
            return true;
        } catch (Exception e) {
            System.out.printf("❌ Failed to enter text '%s' for '%s': %s%n", text, label, e.getMessage());
            return false;
        }
    }

    public static boolean selectDropdownByLabel(WebDriver driver, String label, String visibleText) {
        try {
            WebElement row = findRowByLabel(driver, label);
            WebElement selectElement = row.findElement(By.tagName("select"));
            Select dropdown = new Select(selectElement);
            dropdown.selectByVisibleText(visibleText);
            return true;
        } catch (Exception e) {
            System.out.printf("❌ Failed to select dropdown '%s' for '%s': %s%n", visibleText, label, e.getMessage());
            return false;
        }
    }

    public static boolean setCheckboxByLabel(WebDriver driver, String label, boolean checked) {
        try {
            WebElement row = findRowByLabel(driver, label);
            WebElement checkbox = row.findElement(By.cssSelector("input[type='checkbox']"));
            if (checkbox.isSelected() != checked) {
                checkbox.click();
            }
            return true;
        } catch (Exception e) {
            System.out.printf("❌ Failed to set checkbox '%s' for '%s': %s%n", checked, label, e.getMessage());
            return false;
        }
    }

    public static boolean selectRadioByLabel(WebDriver driver, String label, String visibleText) {
        try {
            WebElement row = findRowByLabel(driver, label);
            List<WebElement> radios = row.findElements(By.cssSelector("input[type='radio']"));
            for (WebElement radio : radios) {
                String valueLabel = radio.findElement(By.xpath("following-sibling::label")).getText().trim();
                if (valueLabel.equalsIgnoreCase(visibleText)) {
                    radio.click();
                    return true;
                }
            }
            System.out.printf("⚠️ No radio button labeled '%s' found for '%s'%n", visibleText, label);
            return false;
        } catch (Exception e) {
            System.out.printf("❌ Failed to select radio '%s' for '%s': %s%n", visibleText, label, e.getMessage());
            return false;
        }
    }

    private static WebElement findRowByLabel(WebDriver driver, String label) throws NoSuchElementException {
        List<WebElement> rows = driver.findElements(By.cssSelector(".property-table .property"));
        for (WebElement row : rows) {
            String rowLabel = row.findElement(By.cssSelector(".property-name")).getText().trim();
            if (rowLabel.equalsIgnoreCase(label)) {
                return row;
            }
        }
        throw new NoSuchElementException("No setting row found with label: " + label);
    }

    // ▼▼▼ Injection method ▼▼▼

    public static void injectSettingsIntoDrawing(WebDriver driver, WebDriverWait wait, Map<String, String> flatSettings) {
        WebElement addNewButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.drawing_inner_box.drawing_button_edit.create")));
        addNewButton.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".property-table")));

        for (Map.Entry<String, String> entry : flatSettings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (!key.contains("|")) continue;

            String[] parts = key.split("\\|");
            if (parts.length < 2) continue;

            String label = parts[1].trim();
            String elementType = (parts.length > 2) ? parts[2].trim() : "";

            boolean success = false;
            if (elementType.equalsIgnoreCase("Checkbox")) {
                success = setCheckboxByLabel(driver, label, value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
            } else if (elementType.equalsIgnoreCase("Drop-down")) {
                success = selectDropdownByLabel(driver, label, value);
            } else {
                success = enterTextByLabel(driver, label, value);
            }

            if (!success) {
                System.out.printf("⚠️ Could not set '%s' to '%s' (type: %s)%n", label, value, elementType);
            }
        }

        System.out.println("✅ Settings injected.");
    }
}
