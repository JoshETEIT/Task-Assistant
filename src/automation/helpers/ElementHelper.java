package automation.helpers;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import automation.config.ConfigManager;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class ElementHelper {
    private static final int SHORT_WAIT_SECONDS = 3;
    private static final int LONG_WAIT_SECONDS = 10;
    private static final boolean VISUAL_DEBUG = ConfigManager.getInstance().getConfig().getAutomation().isVisualDebug();

    public enum LocatorType {ID, CLASS, CSS, XPATH}
    public enum Screenshot {ON, OFF}

    // =========================================================
    //  WAIT HELPERS
    // =========================================================
    private static WebDriverWait getShortWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT_SECONDS));
    }

    private static WebDriverWait getLongWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(LONG_WAIT_SECONDS));
    }

    private static WebElement waitForClickable(WebDriverWait wait, By locator) {
        return wait.until(ExpectedConditions.refreshed(
            ExpectedConditions.elementToBeClickable(locator)
        ));
    }

    private static void waitForDOMReady(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                .until(d -> ((JavascriptExecutor) d)
                    .executeScript("return document.readyState").equals("complete"));
        } catch (Exception e) {
            System.out.println("DOM ready check timeout, continuing anyway");
        }
    }

    private static WebElement locateElement(WebDriver driver,
                                            WebDriverWait wait,
                                            LocatorType type,
                                            String locatorValue) {
        switch (type) {
            case ID: return waitForClickable(wait, By.id(locatorValue));
            case CSS: return waitForClickable(wait, By.cssSelector(locatorValue));
            case XPATH: return waitForClickable(wait, By.xpath(locatorValue));
            case CLASS: return waitForClickable(wait, By.className(locatorValue));
            default:
                throw new IllegalArgumentException("Unsupported locator type: " + type);
        }
    }

    // =========================================================
    //  CLICK ACTIONS
    // =========================================================
    public static boolean clickButton(WebDriver driver, LocatorType type, String locatorValue, Screenshot screenshot, int retries) {
        WebDriverWait wait = getLongWait(driver);
        int attempts = Math.max(1, retries);

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                WebElement element = locateElement(driver, wait, type, locatorValue);
                Thread.sleep(100);

                HighlightHelper.highlight(driver, element, HighlightHelper.Color.AMBER, HighlightHelper.Linger.OFF);

                element.click();

                HighlightHelper.highlight(driver, element, HighlightHelper.Color.GREEN, HighlightHelper.Linger.OFF);

                return true;

            } catch (Exception e) {
                if (VISUAL_DEBUG) {
                    System.out.printf("❌ Click failed: %s='%s' (%s)%n", type, locatorValue, e.getClass().getSimpleName());
                } else {
                    System.out.printf("Attempt %d failed to click (%s='%s'): %s%n",
                            attempt, type, locatorValue, e.getMessage());
                }

                try {
                    waitForDOMReady(driver);
                    WebElement element = locateElement(driver, wait, type, locatorValue);

                    HighlightHelper.highlight(driver, element, HighlightHelper.Color.AMBER, HighlightHelper.Linger.OFF);

                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);

                    HighlightHelper.highlight(driver, element, HighlightHelper.Color.BLUE, HighlightHelper.Linger.OFF);

                    return true;
                } catch (Exception jsException) {
                    WebElement element = locateElement(driver, wait, type, locatorValue);
                    HighlightHelper.highlight(driver, element, HighlightHelper.Color.RED, HighlightHelper.Linger.OFF);
                }
            }
        }
        WebElement element = locateElement(driver, wait, type, locatorValue);
        HighlightHelper.highlight(driver, element, HighlightHelper.Color.RED, HighlightHelper.Linger.OFF);

        if (screenshot == Screenshot.ON) {
            ScreenshotHandler screenshotHandler = new ScreenshotHandler(driver);
            screenshotHandler.screenshot("click-failure-" + locatorValue);
        }
        return false;
    }

    public static void clickElementByCss(WebDriver driver, String cssSelector) {
        try {
            getShortWait(driver)
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)))
                .click();
        } catch (Exception e) {
            System.out.printf("Element with CSS selector '%s' not clickable.%n", cssSelector);
        }
    }

    public static void ActiveToggle(WebDriver driver, String yesOrNo) {
        String xpath = "//div[.//span[normalize-space(text())='Active']]//button[span[contains(.,'" +
                       (yesOrNo.equalsIgnoreCase("yes") ? "Yes" : "No") + "')]]";
        clickButton(driver, LocatorType.XPATH, xpath, Screenshot.OFF, 1);
    }

    // =========================================================
    //  TEXT ENTRY / INPUTS
    // =========================================================
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

    public static void enterCodeMirrorByLabel(WebDriver driver, String labelText, String value) {
        try {
            String xpath = "//span[normalize-space(.)='" + labelText + "']" +
                           "/following::div[contains(@class,'code-editor')][1]";
            WebElement editorContainer = getShortWait(driver)
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));

            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].querySelector('.CodeMirror').CodeMirror.setValue(arguments[1]);",
                editorContainer, value
            );
            System.out.printf("Set CodeMirror field '%s' to '%s'%n", labelText, value);
        } catch (Exception e) {
            System.out.printf("Unable to set CodeMirror for '%s': %s%n", labelText, e.getMessage());
        }
    }

    // =========================================================
    //  DROPDOWNS & SELECTS
    // =========================================================
    public static void selectDropdownByVisibleText(WebDriver driver, String id, String visibleText) {
        try {
            WebDriverWait wait = getShortWait(driver);
            WebElement dropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id(id)));
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();

            Optional<WebElement> matchingOption = options.stream()
                .filter(o -> o.getText().equalsIgnoreCase(visibleText))
                .findFirst();

            if (matchingOption.isPresent()) {
                select.selectByVisibleText(matchingOption.get().getText());
            } else if (!options.isEmpty()) {
                select.selectByIndex(0);
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

    public static void selectDropdownByLabel(WebDriver driver, String labelText, String optionText) {
        try {
            String xpath = "//span[normalize-space(.)='" + labelText + "']" +
                           "/following::select[1]";
            WebElement dropdown = getShortWait(driver)
                    .until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
            new Select(dropdown).selectByVisibleText(optionText);
            System.out.printf("Dropdown '%s' set to '%s'%n", labelText, optionText);
        } catch (Exception e) {
            System.out.printf("Dropdown '%s' not found: %s%n", labelText, e.getMessage());
        }
    }

    public static void selectMultiCheckboxDropdown(WebDriver driver, String[] values) {
        try {
            WebDriverWait wait = getShortWait(driver);
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

    // =========================================================
    //  CHECKBOXES & RADIO BUTTONS
    // =========================================================
    public static void selectCheckboxOrRadioButton(WebDriver driver, String id) {
        try {
            WebElement element = getShortWait(driver)
                    .until(ExpectedConditions.elementToBeClickable(By.id(id)));
            if (!element.isSelected()) element.click();
        } catch (Exception e) {
            System.out.printf("Checkbox/radio with ID '%s' not clickable.%n", id);
        }
    }
}
