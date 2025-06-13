package automation;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

public class UploadPartImage {

    private WebDriver driver;
    private WebDriverWait wait;

    public UploadPartImage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void uploadImageForPart(String partNo, String relativeImagePath) {
        // Step 1: Convert relative path to absolute path
        File imageFile = new File(relativeImagePath);
        String absolutePath = imageFile.getAbsolutePath();

        if (!imageFile.exists()) {
            throw new RuntimeException("Image file not found at: " + absolutePath);
        }

        // Step 2: Click the camera icon for the specified part number
        String cameraIconSelector = String.format("td.part_photo_dropdown_toggle[part_no='%s'] img[src*='camera.svg']", partNo);
        WebElement cameraIcon = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cameraIconSelector)));
        cameraIcon.click();

        // Step 3: Wait for the file input and send the file path
        WebElement fileInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='file']#file")));
        fileInput.sendKeys(absolutePath);


        System.out.println("Image upload complete for part: " + partNo);
    }
}
