package automation;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.io.File;
import java.time.Duration;
import java.util.*;
import automation.ui.ProgressUI;  // IMPORT ADDED

public abstract class BasePartImageUploader {
    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final Set<String> excludedWords;
    protected final ProgressUI progressUI;

    public BasePartImageUploader(WebDriver driver, Set<String> excludedWords, ProgressUI progressUI) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        this.excludedWords = excludedWords;
        this.progressUI = progressUI;
    }

    public void uploadImagesFromFolder(String imagesFolderPath) {
        try {
            File imagesFolder = new File(imagesFolderPath);
            if (!imagesFolder.exists() || !imagesFolder.isDirectory()) {
                throw new RuntimeException("Image folder not found or not accessible: " + imagesFolderPath);
            }

            // Get all image files recursively
            List<File> imageFiles = listImageFilesRecursively(imagesFolder);

            if (imageFiles.isEmpty()) {
                throw new RuntimeException("No image files found in: " + imagesFolderPath);
            }

            System.out.println("\n=== Starting upload with " + imageFiles.size() + " images ===");
            
            navigateToPartList();
            List<WebElement> partRows = getPartRows();
            
            // === PROGRESS TRACKING ADDED ===
            int totalParts = partRows.size();
            progressUI.setMainProgressMax(totalParts);
            progressUI.setStepProgressMax(100);
            progressUI.updateStatus("Processing " + totalParts + " parts");
            // ===============================
            
            int processedCount = 0;
            int skippedCount = 0;
            int failedCount = 0;
            
            for (int i = 0; i < partRows.size(); i++) {
                WebElement partRow = partRows.get(i);
                
                // === PROGRESS TRACKING ADDED ===
                progressUI.updateMainProgress(i);
                progressUI.updateStepProgress(0, "Starting part " + (i+1));
                // ===============================
                
                try {
                    if (processPartRow(partRow, imageFiles)) {
                        processedCount++;
                    } else {
                        skippedCount++;
                    }
                } catch (Exception e) {
                    System.out.println("❌ Failed to process part row: " + e.getMessage());
                    failedCount++;
                    
                    // === PROGRESS TRACKING ADDED ===
                    progressUI.updateStepProgress(100, "❌ Failed");
                    // ===============================
                }
                
                // Small delay to allow UI updates
                Thread.sleep(50);
            }
            
            System.out.println("\n=== Completed: " + processedCount + " parts processed ===");
            System.out.println("=== Skipped: " + skippedCount + " parts (no match or already has image) ===");
            System.out.println("=== Failed: " + failedCount + " parts ===");
        } catch (Exception e) {
            System.out.println("\n❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            progressUI.close();
        }
    }

    protected abstract void navigateToPartList();
    protected abstract List<WebElement> getPartRows();
    protected abstract boolean isMatch(String imageName, String partName);

    protected boolean processPartRow(WebElement partRow, List<File> imageFiles) {
        try {
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(10, "Checking existing image");
            // ===============================
            
            if (hasExistingImage(partRow)) {
                System.out.println("ℹ️ Part '" + getPartName(partRow) + "' already has an image - skipping");
                clearHighlight(partRow);
                
                // === PROGRESS TRACKING ADDED ===
                progressUI.updateStepProgress(100, "⏭️ Skipped");
                // ===============================
                
                return false;
            }
            
            String partName = getPartName(partRow);
            String cleanPartName = cleanName(partName);
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(30, "Matching image");
            // ===============================
            
            Optional<File> matchingImage = findMatchingImage(cleanPartName, imageFiles);
            if (matchingImage.isEmpty()) {
                System.out.println("ℹ️ No matching image found for part: " + partName);
                highlightRow(partRow, "red");
                
                // === PROGRESS TRACKING ADDED ===
                progressUI.updateStepProgress(100, "⏭️ No match");
                // ===============================
                
                return false;
            }
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(60, "Uploading image");
            // ===============================
            
            uploadImage(partRow, matchingImage.get());
            highlightRow(partRow, "green");
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(100, "✅ Uploaded");
            // ===============================
            
            return true;
        } catch (Exception e) {
            System.out.println("❌ Error processing part row: " + e.getMessage());
            highlightRow(partRow, "red");
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(100, "❌ Failed");
            // ===============================
            
            return false;
        }
    }
    
    // Helper method to get part name
    private String getPartName(WebElement partRow) {
        WebElement partNameCell = partRow.findElements(By.tagName("td")).get(2);
        return ((JavascriptExecutor)driver).executeScript(
            "return arguments[0].childNodes[0].textContent.trim();", 
            partNameCell
        ).toString();
    }

    private List<File> listImageFilesRecursively(File folder) {
        List<File> result = new ArrayList<>();
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        result.addAll(listImageFilesRecursively(file));
                    } else {
                        String name = file.getName().toLowerCase();
                        if (name.matches(".*\\.(jpg|jpeg|png)$")) {
                            result.add(file);
                        }
                    }
                }
            }
        }
        return result;
    }

    protected boolean hasExistingImage(WebElement partRow) {
        try {
            WebElement imgElement = partRow.findElement(By.cssSelector("td.part_photo_dropdown_toggle img"));
            String src = imgElement.getDomProperty("src");
            return !src.contains("camera.svg") && 
                   (src.contains("Thumbnail") || src.matches(".*\\.(jpg|jpeg|png)$"));
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

    protected Optional<File> findMatchingImage(String cleanPartName, List<File> imageFiles) {
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
            attemptUpload(partRow, imageFile, false);
        } catch (StaleElementReferenceException | ElementNotInteractableException e) {
            System.out.println("⚠️ First attempt failed, retrying with scroll...");
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
        
        System.out.println("✅ Uploaded image for part: " + getPartName(partRow));
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