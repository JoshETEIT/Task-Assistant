package automation;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

public class UploadPartImage {

    private WebDriver driver;
    private WebDriverWait wait;

    public UploadPartImage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public void uploadImageForPart(String partNo, String imagePath) {
        // Convert to absolute path and verify existence
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new RuntimeException("Image file not found: " + imageFile.getAbsolutePath());
        }
        String absolutePath = imageFile.getAbsolutePath();

        try {
            // 1. Locate part row using robust XPath
            String partRowXpath = String.format(
                "//tr[contains(@class, 'main_part_row') and .//td[text()='%s']]", 
                partNo
            );
            
            WebElement partRow = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath(partRowXpath))
            );
            
            // 2. Find camera icon within the row
            WebElement cameraIcon = partRow.findElement(
                By.cssSelector("td.part_photo_dropdown_toggle img[src*='camera']")
            );
            
            // 3. Scroll and click using JavaScript
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", 
                cameraIcon
            );
            Thread.sleep(500); // Allow scrolling to complete
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cameraIcon);

            // 4. Wait for upload section using part number in ID
            String uploadSectionId = String.format("%s_photo_row", partNo.replace(".", "_"));
            WebElement fileInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("#" + uploadSectionId + " input[type='file']")
                )
            );

            // 5. Upload the image
            fileInput.sendKeys(absolutePath);
            System.out.println("✅ Successfully uploaded image for part: " + partNo);

        } catch (Exception e) {
            throw new RuntimeException("Image upload failed: " + e.getMessage(), e);
        }
    }
}
