package automation;

import automation.helpers.DrawingBoardHelper;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.Map;
import java.util.Scanner;

import automation.helpers.DrawingBoardHelper;

public class DrawingSettingsInjector {

    public static void injectSettingsFromCSV(WebDriver driver, WebDriverWait wait, String csvPath) {
        try {
            wait.until(ExpectedConditions.urlContains("/Home"));
            System.out.println("🌐 Arrived on home page: " + driver.getCurrentUrl());

            driver.findElement(By.xpath("//a[.//span[normalize-space(text())='DrawingBoard Config']]"))
                    .click();
            System.out.println("🔗 Clicked DrawingBoard Config...");

            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[contains(@href, '/DrawingBoardConfig/DrawingTemplate')]"))).click();
            System.out.println("📁 Clicked Drawing Template tab...");

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".span_box2.tiny1")));
            System.out.println("👀 Detected template list table...");

            // Step 1: Navigate to Template Editor
            System.out.println("🧭 Looking for Template Editor...");
            WebElement templateEditor = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//td[contains(text(), 'Template Editor')]/ancestor::table")));
            templateEditor.click();
            System.out.println("📝 Navigated to Template Editor");

            // Step 2 + 3: Try clicking "Add new" and waiting for edit screen
            try {
                System.out.println("➕ Waiting for Add new tile button...");
                WebElement addNewButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("div.drawing_inner_box.drawing_button_edit.create")));
                addNewButton.click();
                System.out.println("🧱 Clicked Add new template");

                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".property-table")));
                System.out.println("✅ Editing screen loaded");

            } catch (UnhandledAlertException alertEx) {
                System.out.println("🚨 Alert popped up: " + alertEx.getAlertText());
                System.out.println("❗ Please dismiss the alert manually in the browser.");
                System.out.print("➡️ Then type 'k' and press Enter to continue: ");

                Scanner scanner = new Scanner(System.in);
                while (!scanner.nextLine().trim().equalsIgnoreCase("k")) {
                    System.out.print("⌛ Waiting... type 'k' to continue: ");
                }

                // Retry after manual alert dismissal
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".property-table")));
                System.out.println("✅ Editing screen loaded after alert");
            }

            // Step 4: Load settings from CSV
            System.out.println("📄 Attempting to load CSV from path: " + csvPath);
            Map<String, String> settings = DrawingSettingsCSV.loadFromCSV(csvPath);
            System.out.println("🔢 Settings loaded: " + settings.size());

            for (Map.Entry<String, String> entry : settings.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (!key.contains("|")) {
                    System.out.println("⚠️ Skipping invalid key: " + key);
                    continue;
                }

                String[] parts = key.split("\\|");
                if (parts.length < 3) {
                    System.out.println("⚠️ Skipping malformed key: " + key);
                    continue;
                }

                String tabName = parts[0].trim();
                String label = parts[1].trim();
                String type = parts[2].trim().toLowerCase();
                String normalizedType = type.toLowerCase().replaceAll("[\\s\\-]", "");

                try {
                    System.out.println("🔄 Injecting: " + tabName + " | " + label + " | " + type + " = " + value);
                    switch (normalizedType) {
                    case "textbox":
                    case "textfield":
                        DrawingBoardHelper.enterTextByLabel(driver, label, value);
                        break;
                    case "dropdown":
                    case "dropdownbox":
                        DrawingBoardHelper.selectDropdownByLabel(driver, label, value);
                        break;
                    case "checkbox":
                        DrawingBoardHelper.setCheckboxByLabel(driver, label, Boolean.parseBoolean(value));
                        break;
                    case "radiobutton":
                        DrawingBoardHelper.selectRadioByLabel(driver, label, value);
                        break;
                    default:
                        System.out.println("❓ Unknown element type: " + type);
                }
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to inject setting: " + label + " → " + e.getMessage());
                }
            }

            System.out.println("✅ Settings injection completed.");

        } catch (Exception ex) {
            System.out.println("💥 Unexpected error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
