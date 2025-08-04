package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import automation.ui.ProgressUI;

import java.time.Duration;
import java.util.List;

public class UploadIronmongeryImagesTask extends BaseUploadImagesTask {
    @Override 
    public String getName() { return "Ironmongery Images"; }

    @Override
    protected String getPartTypeName() { return "Ironmongery"; }

    @Override
    protected void legacyUINavigation(String baseUrl) {
        // Your original working navigation code EXACTLY as-is:
        WebElement partListLink = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//a[.//span[contains(text(),'Part List')]]")));
        partListLink.click();
        
        try {
            WebElement glassTab = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(text(),'Ironmongery')]")));
            glassTab.click();
        } catch (TimeoutException e) {
            driver.findElements(By.cssSelector(".tab_inactive")).stream()
                .filter(tab -> tab.getText().contains("Ironmongery"))
                .findFirst()
                .ifPresent(WebElement::click);
        }
        wait.until(ExpectedConditions.urlContains("/IM"));
    }

    // Original part selection
    @Override
    protected List<WebElement> getPartRows() {
        return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
            By.cssSelector("tr.main_part_row:not(.part_photo_row)")));
    }

    // Legacy exact matching (no excluded words)
    @Override
    protected boolean isMatch(String imageName, String partName) {
        // Original normalization and exact comparison
        String normalizedImage = imageName.toLowerCase()
            .replaceAll("[^a-z0-9]", "");
        String normalizedPart = partName.toLowerCase()
            .replaceAll("[^a-z0-9]", "");
        return normalizedImage.equals(normalizedPart);
    }

    // Original helper method (if used by base class)
    protected String cleanName(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9]", "")
            .trim();
    }
}