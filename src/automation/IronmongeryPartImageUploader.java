package automation;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class IronmongeryPartImageUploader extends BasePartImageUploader {
    private static final Set<String> IRONMONGERY_EXCLUDED_WORDS = Set.of(
        "ironmongery", "hardware", "no", "to", "supply", "customer", "mm"
    );

    public IronmongeryPartImageUploader(WebDriver driver) {
        super(driver, IRONMONGERY_EXCLUDED_WORDS);
    }

    @Override
    protected void navigateToPartList() {
        // Click Part List
        WebElement partListLink = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//a[.//span[contains(text(),'Part List')]]")));
        partListLink.click();
        
        wait.until(ExpectedConditions.urlContains("/PricingAndConfig/PartList"));
        
        // Click Ironmongery tab
        try {
            WebElement ironmongeryTab = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(text(),'Ironmongery')]")));
            ironmongeryTab.click();
        } catch (TimeoutException e) {
            driver.findElements(By.cssSelector(".tab_inactive")).stream()
                .filter(tab -> tab.getText().contains("Ironmongery"))
                .findFirst()
                .ifPresent(WebElement::click);
        }
        wait.until(ExpectedConditions.urlContains("/IM"));
    }

    @Override
    protected List<WebElement> getPartRows() {
        return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
            By.cssSelector("tr.main_part_row")));
    }

    @Override
    protected boolean isMatch(String imageName, String partName) {
        // Stricter matching - require at least 3 matching words
        long matchCount = Arrays.stream(imageName.split(" "))
            .filter(word -> !excludedWords.contains(word))
            .filter(partName::contains)
            .count();
            
        return matchCount >= 3;
    }
}