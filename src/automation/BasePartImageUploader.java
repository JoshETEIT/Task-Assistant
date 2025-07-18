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
            File imagesFolder = new File(imagesFolderPath);
            if (!imagesFolder.exists() || !imagesFolder.isDirectory()) {
                throw new RuntimeException("Image folder not found or not accessible: " + imagesFolderPath);
            }

            File[] imageFiles = imagesFolder.listFiles((dir, name) -> 
                name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));
            
            if (imageFiles == null || imageFiles.length == 0) {
                throw new RuntimeException("No image files found in: " + imagesFolderPath);
            }

            System.out.println("\n=== Starting upload with " + imageFiles.length + " images ===");
            
            navigateToPartList();
            List<WebElement> partRows = getPartRows();
            
            int processedCount = 0;
            int skippedCount = 0;
            int failedCount = 0;
            
            for (WebElement partRow : partRows) {
                try {
                    if (processPartRow(partRow, imageFiles)) {
                        processedCount++;
                    } else {
                        skippedCount++;
                    }
                } catch (Exception e) {
                    System.out.println("❌ Failed to process part row: " + e.getMessage());
                    failedCount++;
                }
            }
            
            System.out.println("\n=== Completed: " + processedCount + " parts processed ===");
            System.out.println("=== Skipped: " + skippedCount + " parts (no match or already has image) ===");
            System.out.println("=== Failed: " + failedCount + " parts ===");
        } catch (Exception e) {
            System.out.println("\n❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected abstract void navigateToPartList();
    protected abstract List<WebElement> getPartRows();
    protected abstract boolean isMatch(String imageName, String partName);

    protected boolean processPartRow(WebElement partRow, File[] imageFiles) {
        try {
            WebElement partNameCell = partRow.findElements(By.tagName("td")).get(2);
            
            // Get only the direct text content, excluding button text
            String partName = ((JavascriptExecutor)driver).executeScript(
                "return arguments[0].childNodes[0].textContent.trim();", 
                partNameCell
            ).toString();
            
            String cleanPartName = cleanName(partName);
            
            // Check if part already has an image
            if (hasExistingImage(partRow)) {
                System.out.println("ℹ️ Part '" + partName + "' already has an image - skipping");
                clearHighlight(partRow); // No highlight for existing images
                return false;
            }
            
            Optional<File> matchingImage = findMatchingImage(cleanPartName, imageFiles);
            if (matchingImage.isEmpty()) {
                System.out.println("ℹ️ No matching image found for part: " + partName);
                highlightRow(partRow, "red"); // Red for no match
                return false;
            }
            
            uploadImage(partRow, matchingImage.get());
            highlightRow(partRow, "green"); // Green for successful upload
            return true;
        } catch (Exception e) {
            System.out.println("❌ Error processing part row: " + e.getMessage());
            highlightRow(partRow, "red"); // Red for errors
            return false;
        }
    }

    protected boolean hasExistingImage(WebElement partRow) {
        try {
            WebElement imgElement = partRow.findElement(By.cssSelector("td.part_photo_dropdown_toggle img"));
            String src = imgElement.getDomProperty("src");
            // Check if src contains a thumbnail URL pattern or isn't the default camera icon
            return !src.contains("camera.svg") && 
                   (src.contains("Thumbnail") || src.matches(".*\\.(jpg|jpeg|png)$"));
        } catch (Exception e) {
            return false;
        }
    }

    protected String cleanName(String name) {
        return name.toLowerCase()
            .replaceAll("\\d+\\.?\\d*mm", "") // Remove measurements
            .replaceAll("[^a-z0-9]", " ")     // Replace special chars with space
            .replaceAll("\\s+", " ")          // Collapse multiple spaces
            .trim();
    }

    protected Optional<File> findMatchingImage(String cleanPartName, File[] imageFiles) {
        List<File> potentialMatches = new ArrayList<>();
        
        for (File imageFile : imageFiles) {
            String imageName = imageFile.getName()
                .substring(0, imageFile.getName().lastIndexOf('.'))
                .toLowerCase()
                .replaceAll("[_\\-]", " ");
            
            if (isMatch(imageName, cleanPartName)) {
                potentialMatches.add(imageFile);
            }
        }
        
        // If multiple matches, pick the one with the closest name length
        if (!potentialMatches.isEmpty()) {
            potentialMatches.sort((f1, f2) -> 
                Integer.compare(
                    Math.abs(f1.getName().length() - cleanPartName.length()),
                    Math.abs(f2.getName().length() - cleanPartName.length())
                ));
            return Optional.of(potentialMatches.get(0));
        }
        
        return Optional.empty();
    }

    protected void uploadImage(WebElement partRow, File imageFile) throws Exception {
        try {
            // First attempt without extra scrolling
            attemptUpload(partRow, imageFile, false);
        } catch (StaleElementReferenceException | ElementNotInteractableException e) {
            System.out.println("⚠️ First attempt failed, retrying with scroll...");
            // Second attempt with forced scroll
            attemptUpload(partRow, imageFile, true);
        }
    }

    private void attemptUpload(WebElement partRow, File imageFile, boolean forceScroll) throws Exception {
        WebElement cameraIcon = partRow.findElement(
            By.cssSelector("td.part_photo_dropdown_toggle img[src*='camera.svg']"));
        
        if (forceScroll) {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", 
                cameraIcon);
            Thread.sleep(300);
        }
        
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cameraIcon);
        Thread.sleep(500);
        
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("form#part_photo_form input#file")));
        
        if (forceScroll) {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", 
                fileInput);
        }
        
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", fileInput);
        fileInput.sendKeys(imageFile.getAbsolutePath());
        Thread.sleep(1000);
        
        System.out.println("✅ Uploaded image for part: " + 
            partRow.findElements(By.tagName("td")).get(2).getText().trim());
    }
    
    protected void highlightRow(WebElement row, String color) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].style.border = '3px solid " + color + "';", 
                row
            );
        } catch (Exception e) {
            System.out.println("Couldn't highlight row: " + e.getMessage());
        }
    }

    protected void clearHighlight(WebElement row) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].style.border = '';", 
                row
            );
        } catch (Exception e) {
            System.out.println("Couldn't clear highlight: " + e.getMessage());
        }
    }
}