package automation.helpers;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class ElementHelper {
    private static final int SHORT_WAIT_SECONDS = 3;
    private static final int LONG_WAIT_SECONDS = 10;
    
    public enum LocatorType {ID, CLASS, CSS, XPATH, ACTIVE_CLASS}
    public enum Screenshot {ON,OFF}
    
    // Centralized wait methods
    private static WebDriverWait getShortWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT_SECONDS));
    }
    
    private static WebDriverWait getLongWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(LONG_WAIT_SECONDS));
    }
    
    public static boolean clickButton(WebDriver driver, LocatorType type, String locatorValue, Screenshot screenshot, int retries) {
        WebDriverWait wait = getLongWait(driver); // ✅ Use centralized long wait
        int attempts = Math.max(1, retries);

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                waitForDOMReady(driver);
                WebElement element = locateElement(driver, wait, type, locatorValue);
                element.click();
                return true;

            } catch (Exception e) {
                System.out.printf("Attempt %d failed to click (%s='%s'): %s%n",
                        attempt, type, locatorValue, e.getMessage());

                // JavaScript fallback
                try {
                    waitForDOMReady(driver);
                    WebElement element = locateElement(driver, wait, type, locatorValue);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                    return true;
                } catch (Exception ignored) {
                    // loop will retry if attempts remain
                }
            }
        }

        // All attempts failed
        if (screenshot == Screenshot.ON) {
            takeScreenshot(driver, "click-failure-" + locatorValue);
        }
        return false;
    }

    public static void enterText(WebDriverWait wait, LocatorType type, String locatorValue, String text) {
        try {
            By locator = (type == LocatorType.ID)
                    ? By.id(locatorValue)
                    : By.className(locatorValue);

            wait.until(ExpectedConditions
                    .visibilityOfElementLocated(locator))
                .sendKeys(text);

        } catch (Exception e) {
            System.out.printf(
                "Unable to find input field (%s='%s'). Skipping input.%n",
                type, locatorValue
            );
        }
    }

    public static void selectDropdownByVisibleText(WebDriver driver, String id, String visibleText) {
        try {
            WebDriverWait wait = getShortWait(driver); // ✅ Use centralized short wait
            WebElement dropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id(id)));
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();

            // Case-insensitive matching
            Optional<WebElement> matchingOption = options.stream()
                .filter(o -> o.getText().equalsIgnoreCase(visibleText))
                .findFirst();

            if (matchingOption.isPresent()) {
                select.selectByVisibleText(matchingOption.get().getText());
            } else if (!options.isEmpty()) {
                select.selectByIndex(0); // Fallback to first option
                System.out.printf("Dropdown '%s': option '%s' not found. Using fallback.%n", 
                    id, visibleText);
            } else {
                System.out.printf("Dropdown '%s' has no options.%n", id);
            }
        } catch (Exception e) {
            System.out.printf("Dropdown with ID '%s' not found or not clickable: %s%n", 
                id, e.getMessage());
        }
    }

    public static void clickButtonById(WebDriver driver, String id) {
        try {
            getShortWait(driver) // ✅ Use centralized short wait
                    .until(ExpectedConditions.elementToBeClickable(By.id(id))).click();
        } catch (Exception e) {
            System.out.printf("Button with ID '%s' not found or not clickable.%n", id);
        }
    }

    public static void selectCheckboxOrRadioButton(WebDriver driver, String id) {
        try {
            WebElement element = getShortWait(driver) // ✅ Use centralized short wait
                    .until(ExpectedConditions.elementToBeClickable(By.id(id)));
            if (!element.isSelected()) element.click();
        } catch (Exception e) {
            System.out.printf("Checkbox/radio with ID '%s' not clickable.%n", id);
        }
    }

    public static void selectMultiCheckboxDropdown(WebDriver driver, String[] values) {
        try {
            WebDriverWait wait = getShortWait(driver); // ✅ Use centralized short wait
            WebElement dropdown = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("[id^='ms-list-']")));
            String dropdownId = dropdown.getDomProperty("id");
            dropdown.click();

            List<WebElement> checkboxes = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                    By.cssSelector("#" + dropdownId + " .ms-options input[type='checkbox']")));

            for (WebElement checkbox : checkboxes) {
                String value = checkbox.getDomProperty("value");
                for (String target : values) {
                    if (value.equalsIgnoreCase(target) && !checkbox.isSelected()) {
                        checkbox.click();
                    }
                }
            }

            dropdown.click();
        } catch (Exception e) {
            System.out.println("Multi-select dropdown not found or not interactable.");
        }
    }
    
    public static void clickElementByCss(WebDriver driver, String cssSelector) {
        try {
            getShortWait(driver) // ✅ Use centralized short wait
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)))
                .click();
        } catch (Exception e) {
            System.out.printf("Element with CSS selector '%s' not clickable.%n", cssSelector);
        }
    }
    
    public static void enterCodeMirrorByLabel(WebDriver driver, String labelText, String value) {
        try {
            String xpath = "//span[normalize-space(.)='" + labelText + "']" +
                           "/following::div[contains(@class,'code-editor')][1]";
            WebElement editorContainer = getShortWait(driver) // ✅ Use centralized short wait
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));

            // Use CodeMirror API to set value
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].querySelector('.CodeMirror').CodeMirror.setValue(arguments[1]);",
                editorContainer, value
            );
            System.out.printf("Set CodeMirror field '%s' to '%s'%n", labelText, value);
        } catch (Exception e) {
            System.out.printf("Unable to set CodeMirror for '%s': %s%n", labelText, e.getMessage());
        }
    }

    public static void selectDropdownByLabel(WebDriver driver, String labelText, String optionText) {
        try {
            String xpath = "//span[normalize-space(.)='" + labelText + "']" +
                           "/following::select[1]";
            WebElement dropdown = getShortWait(driver) // ✅ Use centralized short wait
                    .until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
            new Select(dropdown).selectByVisibleText(optionText);
            System.out.printf("Dropdown '%s' set to '%s'%n", labelText, optionText);
        } catch (Exception e) {
            System.out.printf("Dropdown '%s' not found: %s%n", labelText, e.getMessage());
        }
    }

    public static void clickActiveButton(WebDriver driver, String yesOrNo) {
        String expected = yesOrNo.equalsIgnoreCase("yes") ? "Yes" : "No";
        String xpath = "//div[.//span[normalize-space(text())='Active']]//button[span[contains(.,'" + expected + "')]]";

        try {
            WebDriverWait wait = getShortWait(driver); // ✅ Use centralized short wait
            WebElement activeBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
            activeBtn.click();
            System.out.printf("Active set to %s%n", yesOrNo);
        } catch (Exception e) {
            System.out.printf("Active toggle '%s' not clickable: %s%n", yesOrNo, e.getMessage());
        }
    }
    
    private static WebElement locateElement(WebDriver driver,
            WebDriverWait wait,
            LocatorType type,
            String locatorValue) {
        switch (type) {
            case ID:
                return wait.until(ExpectedConditions.elementToBeClickable(By.id(locatorValue)));
            case CSS:
                return wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(locatorValue)));
            case XPATH:
                return wait.until(ExpectedConditions.elementToBeClickable(By.xpath(locatorValue)));
            case CLASS:
                return wait.until(ExpectedConditions.elementToBeClickable(By.className(locatorValue)));
            case ACTIVE_CLASS:
                List<WebElement> candidates = driver.findElements(By.className(locatorValue));
                for (WebElement el : candidates) {
                    if (el.isDisplayed() && el.isEnabled()
                            && el.getDomAttribute("class").contains("active")) {
                        return wait.until(ExpectedConditions.elementToBeClickable(el));
                    }
                }
                throw new NoSuchElementException("No active element found for class: " + locatorValue);
            default:
                throw new IllegalArgumentException("Unsupported locator type: " + type);
        }
    }

    private static void takeScreenshot(WebDriver driver, String filename) {
        try {
            String dirPath = "src/main/resources/images/errors";
            java.io.File dir = new java.io.File(dirPath);
            
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            TakesScreenshot ts = (TakesScreenshot) driver;
            java.io.File src = ts.getScreenshotAs(OutputType.FILE);
            java.nio.file.Path targetPath = java.nio.file.Paths.get(dirPath, filename + ".png");
            java.nio.file.Files.copy(src.toPath(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            System.out.println("Screenshot saved: " + filename + ".png at: " + targetPath.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("Screenshot failed: " + e.getMessage());
        }
    }
    
    private static void waitForDOMReady(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                .until(d -> ((JavascriptExecutor) d)
                    .executeScript("return document.readyState").equals("complete"));
        } catch (Exception e) {
            // Ignore - continue anyway
            System.out.println("DOM ready check timeout, continuing anyway");
        }
    }
}