package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import automation.ui.AutomationUI;
import automation.ui.ProgressUI;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class DefaultValuesExportTask extends TaskBase {
    
    @Override
    public String getName() {
        return "Export Default Values";
    }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        try {
            progressUI.updateStatus("Starting default values export");
            progressUI.setMainProgressMax(1);
            progressUI.setStepProgressMax(100);
            
            // Navigate to Default Values page
            progressUI.updateStepProgress(10, "Navigating to Default Values");
            navigateToDefaultValues(driver, baseUrl);
            
            // Wait for page to fully load
            progressUI.updateStepProgress(20, "Waiting for page to load");
            waitForPageToLoad(driver);
            
            // Get available columns
            progressUI.updateStepProgress(30, "Reading available templates");
            List<String> availableColumns = getAvailableColumns(driver);
            
            if (availableColumns.isEmpty()) {
                AutomationUI.showMessageDialog(
                    null, 
                    "No template columns found on the page", 
                    "No Data", 
                    JOptionPane.WARNING_MESSAGE
                );
                cancelAndHide(progressUI);
                return;
            }
            
            // Let user select which column to export
            progressUI.updateStepProgress(50, "Waiting for user selection");
            String selectedColumn = showColumnSelectionDialog(availableColumns);
            
            if (selectedColumn == null) {
                cancelAndHide(progressUI);
                return;
            }
            
            // Read property values
            progressUI.updateStepProgress(70, "Reading property values");
            List<PropertyValue> propertyValues = readPropertyValues(driver, selectedColumn);
            
            if (propertyValues.isEmpty()) {
                AutomationUI.showMessageDialog(
                    null, 
                    "No property values found for the selected column", 
                    "No Data", 
                    JOptionPane.WARNING_MESSAGE
                );
                cancelAndHide(progressUI);
                return;
            }
            
            // Export to CSV
            progressUI.updateStepProgress(90, "Exporting to CSV");
            String csvPath = exportToCsv(propertyValues, selectedColumn);
            
            completeAndHide(progressUI, "Successfully exported " + propertyValues.size() + " properties to: " + csvPath);
            
        } catch (Exception e) {
            handleError(progressUI, e);
        }
    }
    
    private void navigateToDefaultValues(WebDriver driver, String baseUrl) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        
        if (!driver.getCurrentUrl().contains("/Home")) {
            driver.get(baseUrl + "/Home");
        }
        
        // Navigate through the UI to Drawing Board Config
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//a[contains(@href,'PricingAndConfig')]")));
        
        automation.helpers.ElementHelper.clickButton(driver, 
            automation.helpers.ElementHelper.LocatorType.XPATH, 
            "//a[contains(@href,'DrawingBoardConfig')]", 
            automation.helpers.ElementHelper.Screenshot.ON, 3);
        
        wait.until(ExpectedConditions.urlContains("DrawingBoardConfig"));
        
        // Click on Default Values tab
        automation.helpers.ElementHelper.clickButton(driver, 
            automation.helpers.ElementHelper.LocatorType.XPATH, 
            "//a[contains(@class,'tab_inactive') and contains(@href,'DefaultValue')]", 
            automation.helpers.ElementHelper.Screenshot.ON, 3);
        
        wait.until(ExpectedConditions.urlContains("DefaultValue"));
    }
    
    private void waitForPageToLoad(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        
        try {
            // Wait for document ready state
            wait.until(d -> ((JavascriptExecutor) d)
                .executeScript("return document.readyState").equals("complete"));
            
            // Wait for the specific table div to be present
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tableDiv")));
            
            // Wait for content to load
            wait.until(d -> {
                WebElement tableDiv = d.findElement(By.id("tableDiv"));
                return tableDiv.findElements(By.className("propertyHeaderRow")).size() > 0;
            });
            
            // Brief pause to ensure everything is settled
            Thread.sleep(1000);
            
        } catch (Exception e) {
            System.out.println("Page load wait: " + e.getMessage());
        }
    }
    
    private List<String> getAvailableColumns(WebDriver driver) {
        List<String> columns = new ArrayList<>();
        
        try {
            // Find the table div
            WebElement tableDiv = driver.findElement(By.id("tableDiv"));
            
            // Find the first header row
            WebElement firstHeaderRow = tableDiv.findElement(By.className("propertyHeaderRow"));
            
            // Get all header cells (skip the first one which is "All")
            List<WebElement> headerCells = firstHeaderRow.findElements(By.tagName("th"));
            
            for (int i = 1; i < headerCells.size(); i++) { // Start from 1 to skip "All"
                WebElement headerCell = headerCells.get(i);
                String columnName = headerCell.getText().trim();
                if (!columnName.isEmpty()) {
                    columns.add(columnName);
                }
            }
            
            System.out.println("Found " + columns.size() + " columns: " + columns);
            
        } catch (Exception e) {
            System.out.println("Error reading columns: " + e.getMessage());
        }
        
        return columns;
    }
    
    private String showColumnSelectionDialog(List<String> availableColumns) {
        // Convert to array for the dialog
        String[] columnArray = availableColumns.toArray(new String[0]);
        
        // Show selection dialog
        int choice = AutomationUI.showOptionDialog(
            null,
            "Select which template column to export:",
            "Export Default Values - Select Column",
            columnArray
        );
        
        if (choice == JOptionPane.CLOSED_OPTION || choice < 0 || choice >= columnArray.length) {
            return null;
        }
        
        return columnArray[choice];
    }
    
    private List<PropertyValue> readPropertyValues(WebDriver driver, String selectedColumn) {
        List<PropertyValue> propertyValues = new ArrayList<>();
        
        try {
            // Find the table div
            WebElement tableDiv = driver.findElement(By.id("tableDiv"));
            
            // Find the column index for the selected column
            int columnIndex = getColumnIndex(driver, selectedColumn);
            if (columnIndex == -1) {
                throw new RuntimeException("Selected column not found: " + selectedColumn);
            }
            
            // Get all property rows (skip header rows)
            List<WebElement> propertyRows = tableDiv.findElements(By.className("propertyRow"));
            
            for (WebElement row : propertyRows) {
                checkCancellation();
                
                try {
                    // Get property name from first column
                    List<WebElement> cells = row.findElements(By.tagName("td"));
                    if (cells.size() == 0) continue;
                    
                    WebElement propertyNameCell = cells.get(0);
                    String propertyName = propertyNameCell.getText().trim();
                    
                    if (propertyName.isEmpty()) {
                        continue; // Skip empty rows
                    }
                    
                    // Get value from selected column
                    if (cells.size() > columnIndex) {
                        WebElement valueCell = cells.get(columnIndex);
                        String value = extractValueFromCell(valueCell);
                        
                        PropertyValue propValue = new PropertyValue();
                        propValue.propertyName = propertyName;
                        propValue.value = value;
                        
                        propertyValues.add(propValue);
                    }
                    
                } catch (Exception e) {
                    System.out.println("Error reading row: " + e.getMessage());
                    // Continue with next row
                }
            }
            
        } catch (Exception e) {
            System.out.println("Error reading property values: " + e.getMessage());
        }
        
        return propertyValues;
    }
    
    private int getColumnIndex(WebDriver driver, String selectedColumn) {
        try {
            // Find the table div
            WebElement tableDiv = driver.findElement(By.id("tableDiv"));
            
            // Find the first header row
            WebElement firstHeaderRow = tableDiv.findElement(By.className("propertyHeaderRow"));
            
            // Get all header cells
            List<WebElement> headerCells = firstHeaderRow.findElements(By.tagName("th"));
            
            for (int i = 0; i < headerCells.size(); i++) {
                WebElement headerCell = headerCells.get(i);
                String columnName = headerCell.getText().trim();
                if (columnName.equals(selectedColumn)) {
                    return i;
                }
            }
            
        } catch (Exception e) {
            System.out.println("Error finding column index: " + e.getMessage());
        }
        
        return -1;
    }
    
    private String extractValueFromCell(WebElement valueCell) {
        try {
            // Check for input fields
            List<WebElement> inputs = valueCell.findElements(By.cssSelector("input[type='text'], input[type='number']"));
            if (!inputs.isEmpty()) {
                String value = inputs.get(0).getDomProperty("value");
                return value != null ? value : "";
            }
            
            // Check for select dropdowns
            List<WebElement> selects = valueCell.findElements(By.tagName("select"));
            if (!selects.isEmpty()) {
                WebElement select = selects.get(0);
                return new org.openqa.selenium.support.ui.Select(select)
                    .getFirstSelectedOption().getText().trim();
            }
            
            // Check for textarea
            List<WebElement> textareas = valueCell.findElements(By.tagName("textarea"));
            if (!textareas.isEmpty()) {
                return textareas.get(0).getText();
            }
            
            // Check for checkbox
            List<WebElement> checkboxes = valueCell.findElements(By.cssSelector("input[type='checkbox']"));
            if (!checkboxes.isEmpty()) {
                return checkboxes.get(0).isSelected() ? "true" : "false";
            }
            
            // Fallback: get cell text
            return valueCell.getText().trim();
            
        } catch (Exception e) {
            System.out.println("Error extracting value from cell: " + e.getMessage());
            return "";
        }
    }
    
    private String exportToCsv(List<PropertyValue> propertyValues, String selectedColumn) throws IOException {
        // Create safe filename from column name
        String safeColumnName = selectedColumn.replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = "DefaultValues_" + safeColumnName + "_" + System.currentTimeMillis() + ".csv";
        String filePath = "resources/CSVs/" + fileName;
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Write header
            writer.println("PropertyName,Value");
            
            // Write data
            for (PropertyValue prop : propertyValues) {
                writer.printf("%s,%s%n",
                    escapeCsv(prop.propertyName),
                    escapeCsv(prop.value)
                );
            }
        }
        
        System.out.println("Exported " + propertyValues.size() + " properties to: " + filePath);
        return filePath;
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    // Data class for property values
    private static class PropertyValue {
        String propertyName;
        String value;
    }
}