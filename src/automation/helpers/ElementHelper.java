package automation.helpers;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;

public class ElementHelper {
    private static final int SHORT_WAIT_TIME = 3;

    public static void enterTextById(WebDriverWait wait, String id, String text) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id))).sendKeys(text);
        } catch (Exception e) {
            System.out.printf("Unable to find input field with ID '%s'. Skipping input.%n", id);
        }
    }

    public static void enterTextByClass(WebDriverWait wait, String className, String text) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.className(className))).sendKeys(text);
        } catch (Exception e) {
            System.out.printf("Unable to find input field with class '%s'. Skipping input.%n", className);
        }
    }

    public static void selectDropdownByVisibleText(WebDriver driver, String id, String visibleText) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT_TIME));
            WebElement dropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id(id)));
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();

            boolean found = options.stream().anyMatch(o -> o.getText().equals(visibleText));
            if (found) {
                select.selectByVisibleText(visibleText);
            } else if (!options.isEmpty()) {
                select.selectByVisibleText(options.get(0).getText());
                System.out.printf("Dropdown '%s': option '%s' not found. Using fallback.%n", id, visibleText);
            } else {
                System.out.printf("Dropdown '%s' has no options.%n", id);
            }
        } catch (Exception e) {
            System.out.printf("Dropdown with ID '%s' not found or not clickable.%n", id);
        }
    }

    public static void clickButtonById(WebDriver driver, String id) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT_TIME))
                    .until(ExpectedConditions.elementToBeClickable(By.id(id))).click();
        } catch (Exception e) {
            System.out.printf("Button with ID '%s' not found or not clickable.%n", id);
        }
    }

    public static boolean clickButtonWithRetry(WebDriver driver, String id, int retries, int waitSec) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSec));

        for (int i = 0; i < retries; i++) {
            try {
                String before = driver.getPageSource();
                clickButtonById(driver, id);
                Thread.sleep(2000);
                String after = driver.getPageSource();
                if (!before.equals(after)) return true;
                System.out.println("Button missed");
            } catch (Exception e) {
                try {
                    Thread.sleep(waitSec * 1000L);
                } catch (InterruptedException ignored) {}
            }
        }
        return false;
    }

    public static void selectCheckboxOrRadioButton(WebDriver driver, String id) {
        try {
            WebElement element = new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT_TIME))
                    .until(ExpectedConditions.elementToBeClickable(By.id(id)));
            if (!element.isSelected()) element.click();
        } catch (Exception e) {
            System.out.printf("Checkbox/radio with ID '%s' not clickable.%n", id);
        }
    }

    public static void selectMultiCheckboxDropdown(WebDriver driver, String[] values) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT_TIME));
            WebElement dropdown = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("[id^='ms-list-']")));
            String dropdownId = dropdown.getAttribute("id");
            dropdown.click();

            List<WebElement> checkboxes = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                    By.cssSelector("#" + dropdownId + " .ms-options input[type='checkbox']")));

            for (WebElement checkbox : checkboxes) {
                String value = checkbox.getAttribute("value");
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
}
