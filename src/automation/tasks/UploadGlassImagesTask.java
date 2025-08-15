package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.util.*;

public class UploadGlassImagesTask extends BaseUploadImagesTask {
    private static final Set<String> EXCLUDED_WORDS = Set.of(
        "glass", "no", "to", "supply", "customer", "mm", "float", "toughened"
    );

    @Override
    public String getName() { return "Glass Images"; }

    @Override
    protected String getPartTypeName() { return "Glass"; }
    
    @Override
    protected Set<String> getExcludedWords() { return EXCLUDED_WORDS; }

    @Override
    protected void legacyUINavigation(String baseUrl) {
        // Your original working navigation code EXACTLY as-is:
        WebElement partListLink = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//a[.//span[contains(text(),'Part List')]]")));
        partListLink.click();
        
        try {
            WebElement glassTab = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(text(),'Glass')]")));
            glassTab.click();
        } catch (TimeoutException e) {
            driver.findElements(By.cssSelector(".tab_inactive")).stream()
                .filter(tab -> tab.getText().contains("Glass"))
                .findFirst()
                .ifPresent(WebElement::click);
        }
        wait.until(ExpectedConditions.urlContains("/GL"));
    }

    // 3. EXACT LEGACY MATCHING LOGIC
    @Override
    protected boolean isMatch(String imageName, String partName) {
        // Your original implementation:
        String cleanImageName = cleanName(imageName).toLowerCase();
        String cleanPartName = cleanName(partName).toLowerCase();
        
        Set<String> imageWords = new HashSet<>(Arrays.asList(cleanImageName.split("\\s+")));
        imageWords.removeAll(EXCLUDED_WORDS);
        
        Set<String> partWords = new HashSet<>(Arrays.asList(cleanPartName.split("\\s+")));
        partWords.removeAll(EXCLUDED_WORDS);
        
        for (String word : imageWords) {
            if (partWords.contains(word)) {
                return true;
            }
        }
        return false;
    }

    // 4. COPIED VERBATIM FROM LEGACY
    protected String cleanName(String name) {
        return name.toLowerCase()
            .replaceAll("\\d+\\.?\\d*mm", "")
            .replaceAll("[^a-z0-9]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    // 5. UNCHANGED PART ROWS SELECTION
    @Override
    protected List<WebElement> getPartRows() {
        return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
            By.cssSelector("tr.main_part_row:not(.part_photo_row)")));
    }
}