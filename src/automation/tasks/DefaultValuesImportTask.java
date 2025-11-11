package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import automation.TestSuite;
import automation.helpers.CsvReader;
import automation.ui.AutomationUI;
import automation.ui.ProgressUI;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;

public class DefaultValuesImportTask extends TaskBase {
    
    @Override
    public String getName() {
        return "Import Default Values";
    }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        try {
            progressUI.updateStatus("Starting default values import");
            progressUI.setMainProgressMax(1);
            progressUI.setStepProgressMax(100);
            
            // 1. Get CSV file
            String csvPath = getCsvFile(progressUI, "Default Values Import");
            if (csvPath == null) return;
            
            // 2. Read CSV data
            progressUI.updateStepProgress(10, "Reading CSV file");
            List<PropertyValue> importData = CsvReader.read(csvPath, this::createPropertyValue)
                .stream()
                .filter(pv -> pv != null && pv.propertyName != null && !pv.propertyName.trim().isEmpty())
                .collect(Collectors.toList());
            
            if (importData.isEmpty()) {
                AutomationUI.showMessageDialog(null, "No valid data found in CSV", "Import Error", JOptionPane.ERROR_MESSAGE);
                cancelAndHide(progressUI);
                return;
            }
            
            // 3. Navigate to page
            progressUI.updateStepProgress(20, "Navigating to Default Values");
            navigateToDefaultValues(driver, baseUrl);
            waitForPageToLoad(driver);
            
            // 4. Let user select target column
            progressUI.updateStepProgress(30, "Reading available columns");
            List<String> availableColumns = getAvailableColumns(driver);
            String targetColumn = showColumnSelectionDialog(availableColumns, "Import");
            
            if (targetColumn == null) {
                cancelAndHide(progressUI);
                return;
            }
            
            // 5. Perform import
            progressUI.updateStepProgress(50, "Importing values");
            progressUI.setMainProgressMax(importData.size());
            int importedCount = importDataToColumn(driver, importData, targetColumn, progressUI);
            
            completeAndHide(progressUI, "Successfully imported " + importedCount + " values to " + targetColumn);
            
        } catch (Exception e) {
            handleError(progressUI, e);
        }
    }
    
    private PropertyValue createPropertyValue(String[] fields) {
        PropertyValue pv = new PropertyValue();
        if (fields.length >= 2) {
            pv.propertyName = cleanCsvValue(fields[0]);
            pv.value = cleanCsvValue(fields[1]);
            
            // Skip completely empty rows
            if (pv.propertyName.isEmpty() && pv.value.isEmpty()) {
                return null;
            }
        } else if (fields.length >= 1) {
            pv.propertyName = cleanCsvValue(fields[0]);
            pv.value = "";
        }
        return pv;
    }

    private String cleanCsvValue(String value) {
        if (value == null) return "";
        
        String cleaned = value.trim();
        
        // Remove surrounding quotes if present
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        
        // Also handle single quotes
        if (cleaned.startsWith("'") && cleaned.endsWith("'")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        
        return cleaned;
    }
    
    private String showColumnSelectionDialog(List<String> availableColumns, String action) {
        String[] columnArray = availableColumns.toArray(new String[0]);
        
        int choice = AutomationUI.showOptionDialog(
            null,
            "Select which template column to " + action + ":",
            "Default Values - Select Target Column",
            columnArray
        );
        
        if (choice == JOptionPane.CLOSED_OPTION || choice < 0 || choice >= columnArray.length) {
            return null;
        }
        
        return columnArray[choice];
    }
    
    private int importDataToColumn(WebDriver driver, List<PropertyValue> importData, 
                                  String targetColumn, ProgressUI progressUI) {
        int importedCount = 0;
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Find the column index
        int columnIndex = getColumnIndex(driver, targetColumn);
        if (columnIndex == -1) {
            throw new RuntimeException("Target column not found: " + targetColumn);
        }
        
        // Get ALL property rows in order
        List<WebElement> allPropertyRows = getAllPropertyRowsInOrder(driver);
        
        System.out.println("CSV rows: " + importData.size() + ", Page rows: " + allPropertyRows.size());
        
        // Smart sequential matching with recovery
        int csvIndex = 0;
        int pageIndex = 0;
        int rowsToProcess = Math.min(importData.size(), allPropertyRows.size());
        
        progressUI.setMainProgressMax(rowsToProcess);
        
        while (csvIndex < importData.size() && pageIndex < allPropertyRows.size() && !TestSuite.isTaskCancelled()) {
            PropertyValue property = importData.get(csvIndex);
            WebElement propertyRow = allPropertyRows.get(pageIndex);
            
            // Skip null properties in CSV
            if (property.propertyName == null || property.propertyName.trim().isEmpty()) {
                System.out.println("⚠️ Skipping null property at CSV index " + csvIndex);
                csvIndex++;
                continue;
            }
            
            String currentPropertyName = getPropertyNameFromRow(propertyRow);
            String normalizedCsvName = normalizePropertyName(property.propertyName);
            String normalizedPageName = normalizePropertyName(currentPropertyName);
            
            // Check if we're aligned
            boolean isAligned = normalizedCsvName.equals(normalizedPageName);
            
            if (isAligned) {
                // Perfect match - import this row
                progressUI.updateMainProgress(csvIndex);
                progressUI.updateStepProgress(50 + (csvIndex * 40 / rowsToProcess), 
                    "Importing " + (csvIndex + 1) + "/" + rowsToProcess);
                
                if (importSingleValueToRow(driver, propertyRow, property.value, columnIndex, wait)) {
                    importedCount++;
                    progressUI.updateStatus("Imported: " + property.propertyName + " (" + importedCount + "/" + rowsToProcess + ")");
                }
                
                csvIndex++;
                pageIndex++;
                
            } else {
                // Misalignment detected - try to recover
                System.out.println("⚠️ Misalignment at CSV index " + csvIndex + ": " +
                                 "CSV='" + normalizedCsvName + "', " +
                                 "Page='" + normalizedPageName + "'");
                
                RecoveryResult recovery = recoverFromMisalignment(importData, allPropertyRows, csvIndex, pageIndex);
                
                if (recovery.success) {
                    System.out.println("✅ Recovery successful: CSV index " + csvIndex + " → " + recovery.newCsvIndex + 
                                     ", Page index " + pageIndex + " → " + recovery.newPageIndex);
                    csvIndex = recovery.newCsvIndex;
                    pageIndex = recovery.newPageIndex;
                } else {
                    // If recovery fails, skip this CSV row and continue
                    System.out.println("❌ Recovery failed, skipping CSV row: " + property.propertyName);
                    csvIndex++;
                }
            }
        }
        
        System.out.println("Import completed: " + importedCount + " values imported");
        return importedCount;
    }
    
    private String normalizePropertyName(String propertyName) {
        if (propertyName == null) return "";
        
        // Remove quotes and normalize line breaks
        return propertyName
            .replace("\"", "") // Remove quotes
            .replace("\n", " ") // Replace newlines with spaces
            .replace("\r", " ") // Replace carriage returns with spaces
            .replaceAll("\\s+", " ") // Collapse multiple spaces
            .trim()
            .toLowerCase();
    }
    
    private RecoveryResult recoverFromMisalignment(List<PropertyValue> importData, 
            List<WebElement> allPropertyRows, 
            int currentCsvIndex, int currentPageIndex) {
RecoveryResult result = new RecoveryResult();
result.success = false;

System.out.println("🔍 ===== DETAILED MISALIGNMENT DEBUG ===== ");
System.out.println("Current position - CSV index: " + currentCsvIndex + ", Page index: " + currentPageIndex);
System.out.println("Current CSV: '" + importData.get(currentCsvIndex).propertyName + "' → Value: '" + importData.get(currentCsvIndex).value + "'");
System.out.println("Current Page: '" + getPropertyNameFromRow(allPropertyRows.get(currentPageIndex)) + "'");

// Show context around the mismatch
System.out.println("\n📋 CSV CONTEXT (5 rows around):");
for (int i = Math.max(0, currentCsvIndex - 2); i < Math.min(importData.size(), currentCsvIndex + 3); i++) {
String marker = (i == currentCsvIndex) ? ">>> " : "    ";
System.out.println(marker + "CSV[" + i + "]: '" + importData.get(i).propertyName + "' → '" + importData.get(i).value + "'");
}

System.out.println("\n🌐 PAGE CONTEXT (5 rows around):");
for (int i = Math.max(0, currentPageIndex - 2); i < Math.min(allPropertyRows.size(), currentPageIndex + 3); i++) {
String marker = (i == currentPageIndex) ? ">>> " : "    ";
System.out.println(marker + "PAGE[" + i + "]: '" + getPropertyNameFromRow(allPropertyRows.get(i)) + "'");
}

// Look ahead in both CSV and page to find the next match
int maxLookAhead = 10;

for (int csvOffset = 0; csvOffset <= maxLookAhead && (currentCsvIndex + csvOffset) < importData.size(); csvOffset++) {
for (int pageOffset = 0; pageOffset <= maxLookAhead && (currentPageIndex + pageOffset) < allPropertyRows.size(); pageOffset++) {

int testCsvIndex = currentCsvIndex + csvOffset;
int testPageIndex = currentPageIndex + pageOffset;

PropertyValue testProperty = importData.get(testCsvIndex);
String testPageName = getPropertyNameFromRow(allPropertyRows.get(testPageIndex));

// Skip null properties in lookahead
if (testProperty.propertyName == null || testProperty.propertyName.trim().isEmpty()) {
continue;
}

String normalizedCsvName = normalizePropertyName(testProperty.propertyName);
String normalizedPageName = normalizePropertyName(testPageName);

if (normalizedCsvName.equals(normalizedPageName)) {
// Found a match!
result.success = true;
result.newCsvIndex = testCsvIndex;
result.newPageIndex = testPageIndex;

System.out.println("\n✅ MATCH FOUND:");
System.out.println("CSV[" + testCsvIndex + "]: '" + testProperty.propertyName + "' → Value: '" + testProperty.value + "'");
System.out.println("PAGE[" + testPageIndex + "]: '" + testPageName + "'");
System.out.println("Offset: CSV+" + csvOffset + ", PAGE+" + pageOffset);

// Log what we skipped
if (csvOffset > 0 || pageOffset > 0) {
System.out.println("\n⏭️ SKIPPED ROWS:");
if (csvOffset > 0) {
System.out.println("Skipped " + csvOffset + " CSV rows:");
for (int i = currentCsvIndex; i < testCsvIndex; i++) {
System.out.println("  - CSV[" + i + "]: '" + importData.get(i).propertyName + "' → '" + importData.get(i).value + "'");
}
}
if (pageOffset > 0) {
System.out.println("Skipped " + pageOffset + " page rows:");
for (int i = currentPageIndex; i < testPageIndex; i++) {
System.out.println("  - PAGE[" + i + "]: '" + getPropertyNameFromRow(allPropertyRows.get(i)) + "'");
}
}
}

System.out.println("===== END DEBUG ===== 🔍");
return result;
}
}
}

System.out.println("❌ No match found within lookahead range of " + maxLookAhead);
System.out.println("===== END DEBUG ===== 🔍");
return result;
}
    
    private List<WebElement> getAllPropertyRowsInOrder(WebDriver driver) {
        List<WebElement> propertyRows = new ArrayList<>();
        try {
            WebElement tableDiv = driver.findElement(By.id("tableDiv"));
            propertyRows = tableDiv.findElements(By.className("propertyRow"));
            System.out.println("Found " + propertyRows.size() + " property rows in order");
        } catch (Exception e) {
            System.out.println("Error getting property rows: " + e.getMessage());
        }
        return propertyRows;
    }
    
    private String getPropertyNameFromRow(WebElement propertyRow) {
        try {
            List<WebElement> cells = propertyRow.findElements(By.tagName("td"));
            if (!cells.isEmpty()) {
                WebElement firstCell = cells.get(0);
                
                // Try to get the caption title first, then fall back to caption
                List<WebElement> captionTitles = firstCell.findElements(By.className("caption_title"));
                if (!captionTitles.isEmpty()) {
                    return captionTitles.get(0).getText().trim();
                }
                
                // Fall back to caption
                List<WebElement> captions = firstCell.findElements(By.className("caption"));
                if (!captions.isEmpty()) {
                    return captions.get(0).getText().trim();
                }
                
                // Last resort: get all text from the cell
                return firstCell.getText().trim();
            }
        } catch (Exception e) {
            System.out.println("Error reading property name from row: " + e.getMessage());
        }
        return "";
    }
    
    private boolean importSingleValueToRow(WebDriver driver, WebElement propertyRow, 
                                         String value, int columnIndex, WebDriverWait wait) {
        try {
            // Get the target cell from the pre-found row
            List<WebElement> cells = propertyRow.findElements(By.tagName("td"));
            if (cells.size() <= columnIndex) {
                System.out.println("Column index out of bounds");
                return false;
            }
            
            WebElement targetCell = cells.get(columnIndex);
            
            // Set the value based on input type
            return setCellValue(driver, targetCell, value, wait);
            
        } catch (Exception e) {
            System.out.println("Error importing value to row: " + e.getMessage());
            return false;
        }
    }
    
    private boolean setCellValue(WebDriver driver, WebElement cell, String value, WebDriverWait wait) {
        try {
            // Wait for cell to be stable
            wait.until(ExpectedConditions.elementToBeClickable(cell));
            
            // Try different input types
            
            // 1. Check for text/number inputs
            List<WebElement> textInputs = cell.findElements(By.cssSelector("input[type='text'], input[type='number']"));
            if (!textInputs.isEmpty()) {
                WebElement input = textInputs.get(0);
                
                // Check if input is enabled
                if (!input.isEnabled()) {
                    System.out.println("Input is disabled, skipping");
                    return false;
                }
                
                try {
                    // Clear using JavaScript to avoid "invalid element state"
                    ((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", input);
                    input.sendKeys(value);
                    return true;
                } catch (Exception e) {
                    System.out.println("Error setting input value, trying JavaScript: " + e.getMessage());
                    // Fallback to JavaScript
                    ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", input, value);
                    return true;
                }
            }
            
            // 2. Check for dropdowns
            List<WebElement> selects = cell.findElements(By.tagName("select"));
            if (!selects.isEmpty()) {
                WebElement select = selects.get(0);
                
                // Check if dropdown is enabled
                if (!select.isEnabled()) {
                    System.out.println("Dropdown is disabled, skipping");
                    return false;
                }
                
                try {
                    new Select(select).selectByVisibleText(value);
                    return true;
                } catch (NoSuchElementException e) {
                    System.out.println("Dropdown option not found: '" + value + "'");
                    return false;
                }
            }
            
            // 3. Check for checkboxes
            List<WebElement> checkboxes = cell.findElements(By.cssSelector("input[type='checkbox']"));
            if (!checkboxes.isEmpty()) {
                WebElement checkbox = checkboxes.get(0);
                
                if (!checkbox.isEnabled()) {
                    System.out.println("Checkbox is disabled, skipping");
                    return false;
                }
                
                boolean shouldBeChecked = "true".equalsIgnoreCase(value) || "1".equals(value);
                if (checkbox.isSelected() != shouldBeChecked) {
                    checkbox.click();
                }
                return true;
            }
            
            // 4. Check for textareas
            List<WebElement> textareas = cell.findElements(By.tagName("textarea"));
            if (!textareas.isEmpty()) {
                WebElement textarea = textareas.get(0);
                
                if (!textarea.isEnabled()) {
                    System.out.println("Textarea is disabled, skipping");
                    return false;
                }
                
                try {
                    textarea.clear();
                    textarea.sendKeys(value);
                    return true;
                } catch (Exception e) {
                    System.out.println("Error setting textarea, trying JavaScript: " + e.getMessage());
                    ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", textarea, value);
                    return true;
                }
            }
            
            System.out.println("No supported input type found in cell");
            return false;
            
        } catch (Exception e) {
            System.out.println("Error setting cell value: " + e.getMessage());
            return false;
        }
    }
    
    private void navigateToDefaultValues(WebDriver driver, String baseUrl) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        
        if (!driver.getCurrentUrl().contains("/Home")) {
            driver.get(baseUrl + "/Home");
        }
        
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//a[contains(@href,'PricingAndConfig')]")));
        
        automation.helpers.ElementHelper.clickButton(driver, 
            automation.helpers.ElementHelper.LocatorType.XPATH, 
            "//a[contains(@href,'DrawingBoardConfig')]", 
            automation.helpers.ElementHelper.Screenshot.ON, 3);
        
        wait.until(ExpectedConditions.urlContains("DrawingBoardConfig"));
        
        automation.helpers.ElementHelper.clickButton(driver, 
            automation.helpers.ElementHelper.LocatorType.XPATH, 
            "//a[contains(@class,'tab_inactive') and contains(@href,'DefaultValue')]", 
            automation.helpers.ElementHelper.Screenshot.ON, 3);
        
        wait.until(ExpectedConditions.urlContains("DefaultValue"));
    }
    
    private void waitForPageToLoad(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        
        try {
            wait.until(d -> ((JavascriptExecutor) d)
                .executeScript("return document.readyState").equals("complete"));
            
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tableDiv")));
            
            wait.until(d -> {
                WebElement tableDiv = d.findElement(By.id("tableDiv"));
                return tableDiv.findElements(By.className("propertyHeaderRow")).size() > 0;
            });
            
            Thread.sleep(1000);
            
        } catch (Exception e) {
            System.out.println("Page load wait: " + e.getMessage());
        }
    }
    
    private List<String> getAvailableColumns(WebDriver driver) {
        List<String> columns = new ArrayList<>();
        
        try {
            WebElement tableDiv = driver.findElement(By.id("tableDiv"));
            WebElement firstHeaderRow = tableDiv.findElement(By.className("propertyHeaderRow"));
            List<WebElement> headerCells = firstHeaderRow.findElements(By.tagName("th"));
            
            for (int i = 1; i < headerCells.size(); i++) {
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
    
    private int getColumnIndex(WebDriver driver, String selectedColumn) {
        try {
            WebElement tableDiv = driver.findElement(By.id("tableDiv"));
            WebElement firstHeaderRow = tableDiv.findElement(By.className("propertyHeaderRow"));
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
    
    // Data classes
    private static class PropertyValue {
        String propertyName;
        String value;
    }
    
    private static class RecoveryResult {
        boolean success;
        int newCsvIndex;
        int newPageIndex;
    }
}