package automation;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.util.List;

public class SeleniumHelpers {
	
    private static final int SHORT_WAIT_TIME = 3;  // Seconds
    private static final int DEFAULT_WAIT_TIME = 10; // Seconds
    private static JProgressBar progressBar;  // Overall progress bar
    private static JProgressBar stepProgressBar;  // Step-specific progress bar

    public static void initializeProgressBars(JProgressBar overallProgress, JProgressBar individualStepProgress) {
        progressBar = overallProgress;
        stepProgressBar = individualStepProgress;

        // Set a fixed size for progress bars (width: 400px, height: 20px)
        if (progressBar != null) {
            progressBar.setPreferredSize(new Dimension(400, 20)); // Main Progress Bar
            progressBar.setOrientation(SwingConstants.HORIZONTAL); // Ensure horizontal layout
        }
        if (stepProgressBar != null) {
            stepProgressBar.setPreferredSize(new Dimension(400, 20)); // Step Progress Bar
            stepProgressBar.setOrientation(SwingConstants.HORIZONTAL); // Ensure horizontal layout
        }
    }
    
    public static Map<String, String> captureDrawingBoardSettings(WebDriver driver, WebDriverWait wait) {
        Map<String, String> settings = new LinkedHashMap<>();
        Set<String> skippedOnce = new HashSet<>();

        // Wait for at least 3 settings to be present (more lenient for sparse tabs)
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
            By.cssSelector(".property-table .property"), 2));
        
        
        
        

        List<WebElement> rows = driver.findElements(By.cssSelector(".property-table .property"));

        for (WebElement row : rows) {
            try {
                WebElement nameCell = row.findElement(By.cssSelector(".property-name"));
                String label = nameCell.getText().trim();

                WebElement valueCell = row.findElement(By.cssSelector(".property-value"));

                // Skip noisy button-based settings only once
                Set<String> buttonBased = Set.of("Divider Spacing", "Range Values", "Button Color");
                if (buttonBased.contains(label)) {
                    continue;
                }

                // 1. Handle checkboxes
                if (!valueCell.findElements(By.cssSelector("input[type='checkbox']")).isEmpty()) {
                    WebElement checkbox = valueCell.findElement(By.cssSelector("input[type='checkbox']"));
                    settings.put(label, checkbox.isSelected() ? "true" : "false");
                }

                // 2. Handle dropdown selects
                else if (!valueCell.findElements(By.tagName("select")).isEmpty()) {
                    WebElement select = valueCell.findElement(By.tagName("select"));
                    Select dropdown = new Select(select);
                    try {
                        WebElement selectedOption = dropdown.getFirstSelectedOption();
                        String selectedText = selectedOption.getText().trim();
                        settings.put(label, selectedText);
                    } catch (Exception e) {
                        System.out.printf("⚠️ Setting '%s' (type: dropdown) had no selected value. Skipping.%n", label);
                    }
                }

                // 3. Fallback to text (e.g., span, div, input value, etc.)
                else {
                    String text = valueCell.getText().trim();

                    // Only add non-empty settings to the map
                    if (!text.isEmpty() && !text.equalsIgnoreCase("No options selected")) {
                        settings.put(label, text);
                    } else {
                        // Silent skip if empty
                        // Uncomment this to debug: System.out.println("🔎 Skipped empty: " + label);
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
                // Re-fetch tabs to avoid stale elements
                tabs = driver.findElements(By.cssSelector(".tree-node"));

                WebElement tab = tabs.get(i);
                String tabName = tab.findElement(By.className("tree-text")).getText().trim();

                if (tabName.isEmpty()) continue;

                tab.click(); // Simulate selecting the tab

                // Wait + capture the settings for this tab
                Map<String, String> settings = captureDrawingBoardSettings(driver, wait);

                allSettings.put(tabName, settings);

            } catch (Exception e) {
                System.out.printf("⚠️ Failed to capture settings for tab index %d: %s%n", i, e.getMessage());
            }
        }

        return allSettings;
    }

    public static void applyDrawingBoardSettings(WebDriver driver, Map<String, String> settings) {
        List<WebElement> rows = driver.findElements(By.cssSelector(".property-table .property"));

        for (WebElement row : rows) {
            try {
                String label = row.findElement(By.cssSelector(".property-name")).getText().trim();
                if (!settings.containsKey(label)) continue;

                String desiredValue = settings.get(label);
                WebElement valueCell = row.findElement(By.cssSelector(".property-value"));

                if (!valueCell.findElements(By.tagName("select")).isEmpty()) {
                    WebElement select = valueCell.findElement(By.tagName("select"));
                    new Select(select).selectByVisibleText(desiredValue);
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

    public static void updateAnyProgress(JProgressBar bar, String message, int targetPercent, int sleepMs) {
        if (bar == null) return;

        SwingUtilities.invokeLater(() -> {
            bar.setString(message);
            bar.setStringPainted(true);
        });

        new Thread(() -> {
            int current = bar.getValue();
            while (current < targetPercent) {
                current++;
                final int progress = current;
                SwingUtilities.invokeLater(() -> bar.setValue(progress));
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }
    
    public static void updateProgress(String message, int targetPercent) {
        updateAnyProgress(progressBar, message, targetPercent, 60); // Or whatever speed you like
    }

    public static void updateStepProgress(String message, int targetPercent) {
        updateAnyProgress(stepProgressBar, message, targetPercent, 60); // Slightly slower, maybe
    }

    public static void closeProgressBars() {
        if (progressBar != null) {
            progressBar.setValue(0);
            progressBar.setString("");
        }
        if (stepProgressBar != null) {
            stepProgressBar.setValue(0);
            stepProgressBar.setString("");
        }
    }

    public static void enterTextById(WebDriverWait wait, String elementId, String text) {
        try {
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(elementId)));
            element.sendKeys(text);
        } catch (Exception e) {
            System.out.printf("Unable to find input field with ID '%s'. Skipping input.%n", elementId);
        }
    }

    public static void enterTextByClass(WebDriverWait wait, String className, String text) {
        try {
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className(className)));
            element.sendKeys(text);
        } catch (Exception e) {
            System.out.printf("Unable to find input field with class '%s'. Skipping input.%n", className);
        }
    }

    public static void selectDropdownByVisibleText(WebDriver driver, String elementId, String visibleText) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT_TIME));
            WebElement dropdownElement = shortWait.until(ExpectedConditions.elementToBeClickable(By.id(elementId)));
            Select select = new Select(dropdownElement);
            List<WebElement> options = select.getOptions();

            boolean found = options.stream().anyMatch(opt -> opt.getText().equals(visibleText));
            if (found) {
                select.selectByVisibleText(visibleText);
            } else {
                String fallback = options.isEmpty() ? null : options.get(0).getText();
                if (fallback != null) {
                    select.selectByVisibleText(fallback);
                    System.out.printf("Dropdown '%s': option '%s' not found. Selected fallback: '%s'%n",
                            elementId, visibleText, fallback);
                } else {
                    System.out.printf("Dropdown '%s' has no options. Skipping.%n", elementId);
                }
            }
        } catch (Exception e) {
            System.out.printf("Dropdown with ID '%s' not found or not selectable. Skipping.%n", elementId);
        }
    }

    public static void clickButtonById(WebDriver driver, String buttonId) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT_TIME));
            WebElement button = shortWait.until(ExpectedConditions.elementToBeClickable(By.id(buttonId)));
            button.click();
        } catch (Exception e) {
            System.out.printf("Button with ID '%s' not found or not clickable.%n", buttonId);
        }
    }
    
    public static boolean clickButtonWithRetry(WebDriver driver, String buttonId, int retries, int waitTimeInSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitTimeInSeconds));

        for (int i = 0; i < retries; i++) {
            try {
            	String domBefore = driver.getPageSource();
                // Try clicking the button
                clickButtonById(driver, buttonId);

                // Wait for a moment (2 seconds) to see if the page changes
                Thread.sleep(2000); // 2 seconds wait before checking if the click had any effect

                // Check if the page has changed (if DOM changes after click)
                String domAfter = driver.getPageSource();

                if (!domBefore.equals(domAfter)) {
                    // If the page has changed, return true
                    return true;
                }

            } catch (Exception e) {
                // If something fails, try again after the specified wait
                try {
                	System.out.println("Button failed");
                    Thread.sleep(waitTimeInSeconds * 1000); // Wait for retry interval
                } catch (InterruptedException ignored) {}
            }
        }
        return false; // If no success after all retries
    }

    public static void selectCheckboxOrRadioButton(WebDriver driver, String elementId) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT_TIME));
            WebElement element = shortWait.until(ExpectedConditions.elementToBeClickable(By.id(elementId)));
            if (!element.isSelected()) {
                element.click();
            }
        } catch (Exception e) {
            System.out.printf("Checkbox or radio with ID '%s' not found or not clickable.%n", elementId);
        }
    }

    public static void selectMultiCheckboxDropdown(WebDriver driver, String[] targetValues) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT_TIME));
            WebElement dropdownButton = shortWait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("[id^='ms-list-']")));
            String dropdownId = dropdownButton.getAttribute("id");
            dropdownButton.click();

            List<WebElement> checkboxes = shortWait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                    By.cssSelector("#" + dropdownId + " .ms-options input[type='checkbox']")));

            for (WebElement checkbox : checkboxes) {
                String value = checkbox.getAttribute("value");
                for (String target : targetValues) {
                    if (value.equalsIgnoreCase(target) && !checkbox.isSelected()) {
                        checkbox.click();
                    }
                }
            }

            dropdownButton.click();
        } catch (Exception e) {
            System.out.println("Multi-select dropdown not found or not interactable. Skipping.");
        }
    }
}