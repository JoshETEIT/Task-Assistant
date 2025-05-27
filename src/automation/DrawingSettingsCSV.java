package automation;

import java.io.*;
import java.util.*;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.io.IOException;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import automation.helpers.DrawingBoardHelper;
import automation.helpers.DrawingBoardHelper.*;

public class DrawingSettingsCSV {
	
	public static void saveSettings(WebDriver driver, WebDriverWait wait) {
	    wait.until(ExpectedConditions.urlContains("/Home"));

	    // Navigate to DrawingBoard Config
	    driver.findElement(By.xpath("//a[.//span[normalize-space(text())='DrawingBoard Config']]")).click();
	    System.out.println("Navigated to DrawingBoard Config...");

	    // Navigate to Drawing Template tab
	    wait.until(ExpectedConditions.elementToBeClickable(
	        By.xpath("//a[contains(@href, '/DrawingBoardConfig/DrawingTemplate')]"))).click();
	    System.out.println("Navigated to Drawing Template tab...");

	    // Wait for the thumbnails to load
	    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".span_box2.tiny1")));

	    // Extract all drawing elements (ignoring the deleted ones)
	    List<String> tileHrefs = new ArrayList<>();
	    List<WebElement> drawings = driver.findElements(By.cssSelector(".span_box2.tiny1:not(.drawing_deleted)"));
	    System.out.println("✅ Found " + drawings.size() + " drawing tiles.");

	    // Collect href links of each tile
	    for (WebElement drawing : drawings) {
	        try {
	            WebElement parent = drawing.findElement(By.xpath("ancestor::a[1]"));
	            String href = parent.getAttribute("href");
	            if (href != null && !href.isEmpty()) {
	                tileHrefs.add(href);
	                System.out.println("🔗 Found href: " + href);
	            }
	        } catch (Exception e) {
	            // silently skip tiles without <a> parents
	        }
	    }

	    // Store settings for all drawings
	    List<Map<String, String>> allDrawings = new ArrayList<>();
	    Set<String> allHeaders = new LinkedHashSet<>(); // to build the full set of columns

	    // Iterate through each tile, click it, and fetch settings
	    for (int i = 0; i < tileHrefs.size(); i++) {
	        // Limit to first 5 tiles for testing purposes
	        if (i >= 5) {
	        	System.out.println("Fetched all five");
	            break;
	        }
	        
	        System.out.println("Fetching settings for tile #" + (i + 1));
	        String href = tileHrefs.get(i);
	        
	        // Open the tile's page by navigating to the href
	        driver.get(href);

	        // Wait for the settings table to load
	        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".property-table .property")));

	        System.out.println("Fetching settings for tile #" + (i + 1));

	        // Extract settings for the current tile
	        try {
	        	// Capture all tabbed settings for the current drawing
	        	Map<String, Map<String, Map.Entry<String, String>>> settingsByTab = DrawingBoardHelper.captureAllDrawingTabSettings(driver, wait);


	        	// Flattened row to store tab-prefixed settings
	        	Map<String, String> row = new LinkedHashMap<>();

	        	// Flatten and store each tab’s settings
	        	for (Map.Entry<String, Map<String, Map.Entry<String, String>>> tabEntry : settingsByTab.entrySet()) {
	        	    String tabName = tabEntry.getKey();
	        	    Map<String, Map.Entry<String, String>> tabSettings = tabEntry.getValue();

	        	    for (Map.Entry<String, Map.Entry<String, String>> setting : tabSettings.entrySet()) {
	        	        String label = setting.getKey();
	        	        String value = setting.getValue().getKey();        // the setting's value
	        	        String elementType = setting.getValue().getValue(); // the element type

	        	        String key = tabName + " | " + label + " | " + elementType;
	        	        row.put(key, value);
	        	        allHeaders.add(key);
	        	    }
	        	}


	        	allDrawings.add(row);  // Store row for this drawing
	        } catch (Exception e) {
	            System.out.println("❌ Failed to extract settings for tile #" + (i + 1) + ": " + e.getMessage());
	            e.printStackTrace();
	        }

	        // Return to the previous page
	        driver.navigate().back();
	        // Wait for the drawing template page to reload
	        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".span_box2.tiny1")));
	    }

	    // Save the settings to a CSV file
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
            // Write header
            pw.print("Drawing Title");
            for (String header : headers) {
                pw.print("," + "\"" + header.replace("\"", "\"\"") + "\"");
            }
            pw.println();

            // Write each row
            for (Map<String, String> row : data) {
                pw.print("\"" + row.getOrDefault("Drawing Title", "").replace("\"", "\"\"") + "\"");
                for (String header : headers) {
                    String value = row.getOrDefault(header, "");
                    pw.print("," + "\"" + value.replace("\"", "\"\"") + "\"");
                }
                pw.println();
            }

            System.out.println("✅ CSV written to " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to write CSV: " + e.getMessage());
        }
    }

    public static Map<String, String> loadFromCSV(String filePath) {
        Map<String, String> settings = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\",\"");
                if (parts.length == 2) {
                    String key = parts[0].replace("\"", "").trim();
                    String value = parts[1].replace("\"", "").trim();
                    settings.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load settings from CSV: " + e.getMessage());
        }

        return settings;
    }

    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return value.replace("\"", "\"\"");  // Escape internal quotes
        }
        return value;
    }
}
