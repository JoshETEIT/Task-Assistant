package automation;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.io.File;
import java.time.Duration;
import java.util.*;

public abstract class BasePartImageUploader {
    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final Set<String> excludedWords;

    public BasePartImageUploader(WebDriver driver, Set<String> excludedWords) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        this.excludedWords = excludedWords;
    }

    public void uploadImagesFromFolder(String imagesFolderPath) {
        try {
            // Normalize folder path
            imagesFolderPath = imagesFolderPath.endsWith(File.separator) 
                ? imagesFolderPath 
                : imagesFolderPath + File.separator;

            File imagesFolder = new File(imagesFolderPath);
            if (!imagesFolder.exists()) {
                throw new RuntimeException("Image folder not found: " + imagesFolderPath);
            }

            File[] imageFiles = imagesFolder.listFiles((dir, name) -> 
                name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));
            
            if (imageFiles == null || imageFiles.length == 0) {
                throw new RuntimeException("No image files found");
            }

            System.out.println("\n=== Starting upload with " + imageFiles.length + " images ===");
            
            navigateToPartList();
            List<WebElement> partRows = getPartRows();
            
            int processedCount = 0;
            for (WebElement partRow : partRows.subList(0, Math.min(41, partRows.size()))) {
                if (processPartRow(partRow, imageFiles)) {
                    processedCount++;
                }
            }
            
            System.out.println("\n=== Completed: " + processedCount + " parts processed ===");
        } catch (Exception e) {
            System.out.println("\n❌ Error: " + e.getMessage());
        }
    }

    protected abstract void navigateToPartList();
    protected abstract List<WebElement> getPartRows();
    protected abstract boolean isMatch(String imageName, String partName);

    protected boolean processPartRow(WebElement partRow, File[] imageFiles) {
        try {
            String partName = partRow.findElements(By.tagName("td")).get(2).getText().trim();
            String cleanPartName = cleanName(partName);
            
            Optional<File> matchingImage = findMatchingImage(cleanPartName, imageFiles);
            if (matchingImage.isEmpty()) return false;
            
            uploadImage(partRow, matchingImage.get());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected String cleanName(String name) {
        return name.toLowerCase()
            .replaceAll("\\d+\\.?\\d*mm", "")
            .replaceAll("[^a-z0-9]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    protected Optional<File> findMatchingImage(String cleanPartName, File[] imageFiles) {
        for (File imageFile : imageFiles) {
            String imageName = imageFile.getName()
                .substring(0, imageFile.getName().lastIndexOf('.'))
                .toLowerCase()
                .replaceAll("[_\\-]", " ");
            
            if (isMatch(imageName, cleanPartName)) {
                return Optional.of(imageFile);
            }
        }
        return Optional.empty();
    }

    protected void uploadImage(WebElement partRow, File imageFile) throws Exception {
        WebElement cameraIcon = partRow.findElement(
            By.cssSelector("td.part_photo_dropdown_toggle img[src*='camera.svg']"));
        
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", 
            cameraIcon);
        Thread.sleep(300);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cameraIcon);
        
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("form#part_photo_form input#file")));
        fileInput.sendKeys(imageFile.getAbsolutePath());
        Thread.sleep(500);
    }
}