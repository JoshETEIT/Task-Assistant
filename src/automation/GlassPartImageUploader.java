package automation;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;
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
        // For glass parts, we can be more lenient with matching
        String[] imageWords = Arrays.stream(imageName.split(" "))
            .filter(word -> !excludedWords.contains(word))
            .toArray(String[]::new);
        
        // At least 50% of the words should match
        long matchingWords = Arrays.stream(imageWords)
            .filter(partName::contains)
            .count();
            
        return matchingWords >= (imageWords.length / 2.0);
    }
}