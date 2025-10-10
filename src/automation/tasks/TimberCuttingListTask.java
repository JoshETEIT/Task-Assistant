package automation.tasks;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import automation.helpers.CsvReader;
import automation.helpers.ElementHelper;
import automation.helpers.FileChooserHelper;
import automation.ui.ProgressUI;

public class TimberCuttingListTask extends TaskBase {

    private String csvPath;
    private WebDriverWait wait;

    public TimberCuttingListTask() {
        super(); // TaskBase no-arg
    }

    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // Ask user for CSV path using ProgressUI-aware chooser
        if (csvPath == null) {
            csvPath = FileChooserHelper.showFileChopper(progressUI, "Timber Cutting List CSV");
            if (csvPath == null || csvPath.isBlank()) {
                FileChooserHelper.showErrorDialog("No CSV selected. Task aborted.");
                cancelAndHide(progressUI);
                return;
            }
        }

        // Read CSV rows into a list of maps
        List<Map<String, String>> records;
        try {
            records = CsvReader.read(csvPath, TimberCuttingListTask::mapRow);
        } catch (IOException e) {
            FileChooserHelper.showErrorDialog("Failed to read CSV: " + e.getMessage());
            return;
        }

        driver.get(baseUrl + "/Production/LookupEditor/TimberCuttingList");
        wait.until(ExpectedConditions.urlContains("/TimberCuttingList"));

        for (Map<String, String> row : records) {
        	driver.switchTo().defaultContent();

        	try {
        	    WebElement createBtn = new WebDriverWait(driver, Duration.ofSeconds(15))
        	        .until(ExpectedConditions.elementToBeClickable(
        	            By.xpath("//div[@class='highlighted-button']//button[span[contains(.,'Create new TCL rule')]]")
        	        ));
        	    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        	    
        	    createBtn.click();
        	} catch (TimeoutException e) {
        	    System.out.println("Timed out waiting for 'Create new TCL rule' button");
        	}
            fillForm(driver, wait, row);
        }
        completeAndHide(progressUI, "Timber cutting list rules imported");
    }

    private static Map<String, String> mapRow(String[] columns) {
        // 🔄 Adjust headers to match your CSV
        String[] headers = {
            "Component", "Group", "Loop", "Active", "Stocked",
            "SortOrder", "Condition", "Quantity", "FinalLength",
            "RoughLength", "Width", "Thickness", "Material", "Comment"
        };

        return java.util.stream.IntStream.range(0, Math.min(headers.length, columns.length))
            .boxed()
            .collect(Collectors.toMap(i -> headers[i], i -> columns[i].trim()));
    }
    
    private static String clean(String s) {
        // removes a single leading and trailing quote if present
    	return s.replaceAll("^\"|\"$", "").replaceAll("[\\[\\]]", "");
    }

    private void fillForm(WebDriver driver, WebDriverWait wait, Map<String, String> row) {

        // CodeMirror fields        
        ElementHelper.selectDropdownByLabel(driver, "Group", row.get("Group"));
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        ElementHelper.selectDropdownByLabel(driver, "Loop", row.get("Loop").replace("_", " "));
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        // CodeMirror fields (trim any surrounding quotes)
        ElementHelper.enterCodeMirrorByLabel(driver, "Component", clean(row.get("Component")));
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        ElementHelper.enterCodeMirrorByLabel(driver, "Condition", clean(row.get("Condition").replace("!", "not ")));
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        ElementHelper.enterCodeMirrorByLabel(driver, "Quantity", clean(row.get("Quantity")));
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        ElementHelper.enterCodeMirrorByLabel(driver, "Final Length", clean(row.get("FinalLength")));
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        ElementHelper.enterCodeMirrorByLabel(driver, "Rough Length", clean(row.get("RoughLength")));
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        ElementHelper.enterCodeMirrorByLabel(driver, "Width", clean(row.get("Width")));
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        ElementHelper.enterCodeMirrorByLabel(driver, "Thickness", clean(row.get("Thickness")));
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        ElementHelper.enterCodeMirrorByLabel(driver, "Material", clean(row.get("Material")));
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        // Comment input
        WebElement commentInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//span[normalize-space(.)='Comment']/following::input[1]")));
        commentInput.sendKeys(row.get("Comment"));
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        // Sort Order input (clean numeric value, clear existing value first)
        WebElement sortOrderInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//div[normalize-space(text())='Sort Order']/following-sibling::div//input[@type='number']")));
        sortOrderInput.clear();
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        String rawSortOrder = row.get("SortOrder");
        if (rawSortOrder != null && !rawSortOrder.isBlank()) {
            String digitsOnly = rawSortOrder.replaceAll("[^0-9]", "").trim();
            if (!digitsOnly.isEmpty()) {
                sortOrderInput.sendKeys(digitsOnly);
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        }

        // Active toggle
        ElementHelper.ActiveToggle(driver, row.getOrDefault("Active", "No"));
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        // Click "Create New Rule" button
        try {
            WebElement createRuleBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'highlighted-button')]//button[span[contains(normalize-space(.),'Create New Rule')]]")
            ));
            try { Thread.sleep(500); } catch (InterruptedException ignored) {} // extra wait for safety
            createRuleBtn.click();
        } catch (TimeoutException e) {
            System.out.println("Timed out waiting for 'Create New Rule' button");
        }

        try { Thread.sleep(300); } catch (InterruptedException ignored) {} // slight wait before next form
    }



    @Override
    public String getName() {
        return "Timber Cutting List";
    }
}
