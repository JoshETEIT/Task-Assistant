package automation;

import java.io.*;
import java.util.*;

import com.opencsv.CSVReader;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static automation.helpers.DrawingBoardHelper.captureAllDrawingTabSettings;

public class DrawingSettingsCSV {
	
	public static class DrawingSetting {
	    public String tab;
	    public String name;
	    public String type;
	    public String value;

	    public DrawingSetting(String tab, String name, String type, String value) {
	        this.tab = tab;
	        this.name = name;
	        this.type = type;
	        this.value = value;
	    }
	}
	
	public static class DrawingConfiguration {
	    public String drawingTitle;
	    public List<DrawingSetting> settings = new ArrayList<>();

	    public void addSetting(DrawingSetting setting) {
	        settings.add(setting);
	    }
	}

    public static void saveSettings(WebDriver driver, WebDriverWait wait) {
        wait.until(ExpectedConditions.urlContains("/Home"));

        driver.findElement(By.xpath("//a[.//span[normalize-space(text())='DrawingBoard Config']]"))
                .click();
        System.out.println("Navigated to DrawingBoard Config...");

        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@href, '/DrawingBoardConfig/DrawingTemplate')]"))).click();
        System.out.println("Navigated to Drawing Template tab...");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".span_box2.tiny1")));

        List<String> tileHrefs = new ArrayList<>();
        List<WebElement> drawings = driver.findElements(By.cssSelector(".span_box2.tiny1:not(.drawing_deleted)"));
        System.out.println("✅ Found " + drawings.size() + " drawing tiles.");

        for (WebElement drawing : drawings) {
            try {
                WebElement parent = drawing.findElement(By.xpath("ancestor::a[1]"));
                String href = parent.getAttribute("href");
                if (href != null && !href.isEmpty()) {
                    tileHrefs.add(href);
                    System.out.println("🔗 Found href: " + href);
                }
            } catch (Exception e) {
                // skip
            }
        }

        List<Map<String, String>> allDrawings = new ArrayList<>();
        Set<String> allHeaders = new LinkedHashSet<>();

        for (int i = 0; i < tileHrefs.size(); i++) {
            if (i >= 5) break;

            String href = tileHrefs.get(i);
            driver.get(href);

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".property-table .property")));

            System.out.println("Fetching settings for tile #" + (i + 1));

            try {
                Map<String, Map<String, Map.Entry<String, String>>> settingsByTab = captureAllDrawingTabSettings(driver, wait);
                Map<String, String> row = new LinkedHashMap<>();

                for (Map.Entry<String, Map<String, Map.Entry<String, String>>> tabEntry : settingsByTab.entrySet()) {
                    String tabName = tabEntry.getKey();
                    Map<String, Map.Entry<String, String>> tabSettings = tabEntry.getValue();

                    for (Map.Entry<String, Map.Entry<String, String>> setting : tabSettings.entrySet()) {
                        String settingName = setting.getKey();
                        String value = setting.getValue().getKey();
                        String elementType = setting.getValue().getValue();

                        String key = tabName + " | " + settingName + " | " + elementType;
                        row.put(key, value);
                        allHeaders.add(key);
                    }
                }

                allDrawings.add(row);
            } catch (Exception e) {
                System.out.println("❌ Failed to extract settings for tile #" + (i + 1) + ": " + e.getMessage());
                e.printStackTrace();
            }

            driver.navigate().back();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".span_box2.tiny1")));
        }

        saveTableToCSV(allDrawings, allHeaders, "drawing_settings.csv");
        System.out.println("✅ Saved all drawing settings.");
    }

    public static void saveToCSV(Map<String, String> settings, String filePath) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println("Property,Value");
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                pw.printf("\"%s\",\"%s\"%n", escape(entry.getKey()), escape(entry.getValue()));
            }
        } catch (IOException e) {
            System.err.println("Failed to write settings to CSV: " + e.getMessage());
        }
    }

    public static void saveTableToCSV(List<Map<String, String>> data, Set<String> headers, String filePath) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.print("Drawing Title");
            for (String header : headers) {
                pw.print(",\"" + header.replace("\"", "\"\"") + "\"");
            }
            pw.println();

            for (Map<String, String> row : data) {
                pw.print("\"" + row.getOrDefault("Drawing Title", "").replace("\"", "\"\"") + "\"");
                for (String header : headers) {
                    String value = row.getOrDefault(header, "");
                    pw.print(",\"" + value.replace("\"", "\"\"") + "\"");
                }
                pw.println();
            }

            System.out.println("✅ CSV written to " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to write CSV: " + e.getMessage());
        }
    }

    public static List<DrawingConfiguration> loadAllRowsFromCSV(String csvPath) {
        List<DrawingConfiguration> allConfigurations = new ArrayList<>();

        try (CSVReader csvReader = new CSVReader(new FileReader(csvPath))) {
            String[] headers = csvReader.readNext();
            if (headers == null) {
                System.out.println("❌ CSV is empty.");
                return allConfigurations;
            }

            // Parse headers to get metadata
            List<String[]> headerPartsList = new ArrayList<>();
            for (String column : headers) {
                if (column.equalsIgnoreCase("Drawing Title")) {
                    headerPartsList.add(new String[] { "Drawing Title", "", "" });
                    continue;
                }

                String[] parts = column.split(" \\| ");
                if (parts.length == 3) {
                    headerPartsList.add(new String[] { parts[0].trim(), parts[1].trim(), parts[2].trim() });
                } else {
                    System.out.println("⚠️ Skipping malformed header: " + column);
                    headerPartsList.add(null); // Placeholder to keep index in sync
                }
            }

            String[] values;
            while ((values = csvReader.readNext()) != null) {
                DrawingConfiguration config = new DrawingConfiguration();

                for (int i = 0; i < values.length && i < headerPartsList.size(); i++) {
                    String[] parts = headerPartsList.get(i);
                    if (parts == null) continue;

                    String value = values[i].trim();

                    if (parts[0].equals("Drawing Title")) {
                        config.drawingTitle = value;
                    } else {
                        String tab = parts[0];
                        String name = parts[1];
                        String type = parts[2];

                        config.addSetting(new DrawingSetting(tab, name, type, value));
                    }
                }

                if (config.drawingTitle != null || !config.settings.isEmpty()) {
                    allConfigurations.add(config);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error reading CSV: " + e.getMessage());
            e.printStackTrace();
        }

        return allConfigurations;
    }



    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return value.replace("\"", "\"\"");
        }
        return value;
    }
}
