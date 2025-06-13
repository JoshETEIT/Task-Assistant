package automation;

import automation.DrawingSettingsCSV.DrawingConfiguration;
import automation.DrawingSettingsCSV.DrawingSetting;
import automation.helpers.DrawingBoardHelper;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
            List<DrawingConfiguration> configurations = DrawingSettingsCSV.loadAllRowsFromCSV(csvPath);
            System.out.println("🔢 Configurations loaded: " + configurations.size());

            String currentTab = ""; // Track last clicked tab to avoid unnecessary re-clicks

            for (DrawingConfiguration config : configurations) {
                System.out.println("🎨 Injecting settings for drawing: " + config.drawingTitle);

                for (DrawingSetting setting : config.settings) {
                    String tabName = setting.tab.trim();
                    String label = setting.name.trim();
                    String type = setting.type.trim();
                    String value = setting.value.trim();

                    if (tabName.isEmpty() || label.isEmpty() || type.isEmpty()) {
                        System.out.printf("⚠️ Skipping incomplete setting: %s | %s | %s%n", tabName, label, type);
                        continue;
                    }

                    try {
                        // Only switch tabs when necessary
                        if (!tabName.equals(currentTab)) {
                            System.out.println("➡️ Attempting to find tab: '" + tabName + "'");
                            List<WebElement> tabElements = driver.findElements(By.cssSelector(".tree-node"));

                            boolean found = false;
                            for (WebElement tab : tabElements) {
                                String tabText = tab.findElement(By.className("tree-text")).getText().trim();
                                if (tabText.equalsIgnoreCase(tabName)) {
                                    System.out.println("✔️ Clicking tab: '" + tabText + "'");
                                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", tab);
                                    Thread.sleep(700); // Allow content to update
                                    currentTab = tabName;
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                System.out.println("❌ Could not find tab: " + tabName + " – skipping setting.");
                                continue;
                            }
                        }

                        String normalizedType = type.toLowerCase().replaceAll("[\\s\\-]", "");
                        //System.out.println("🔄 Injecting: " + tabName + " | " + label + " | " + type + " = " + value);

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
            }





            System.out.println("✅ Settings injection completed.");

        } catch (Exception ex) {
            System.out.println("💥 Unexpected error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
