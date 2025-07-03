package automation;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlassPartImageUploader extends BasePartImageUploader {
    private static final Set<String> GLASS_EXCLUDED_WORDS = Set.of(
        "glass", "no", "to", "supply", "customer", "mm", "float", "toughened"
    );

    public GlassPartImageUploader(WebDriver driver) {
        super(driver, GLASS_EXCLUDED_WORDS);
    }

    @Override
    protected void navigateToPartList() {
        // Click Part List
        WebElement partListLink = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//a[.//span[contains(text(),'Part List')]]")));
        partListLink.click();
        
        wait.until(ExpectedConditions.urlContains("/PricingAndConfig/PartList"));
        
        // Click Glass tab with better handling
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

    @Override
    protected List<WebElement> getPartRows() {
        return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
            By.cssSelector("tr.main_part_row:not(.part_photo_row)")));
    }

    @Override
    protected boolean isMatch(String imageName, String partName) {
        String cleanImageName = cleanName(imageName).toLowerCase();
        String cleanPartName = cleanName(partName).toLowerCase();
        
        Set<String> imageWords = new HashSet<>(Arrays.asList(cleanImageName.split("\\s+")));
        imageWords.removeAll(excludedWords);
        
        Set<String> partWords = new HashSet<>(Arrays.asList(cleanPartName.split("\\s+")));
        partWords.removeAll(excludedWords);
        
        for (String word : imageWords) {
            if (partWords.contains(word)) {
                return true;
            }
        }
        return false;
    }
}