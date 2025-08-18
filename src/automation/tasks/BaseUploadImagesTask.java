package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import automation.ui.AutomationUI;
import automation.ui.ProgressUI;

import java.io.File;
import java.time.Duration;
import java.util.*;
import javax.swing.*;

public abstract class BaseUploadImagesTask extends TaskBase {
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected ProgressUI progressUI;
    protected Set<String> excludedWords = Collections.emptySet();

    protected abstract List<WebElement> getPartRows();
    protected abstract boolean isMatch(String imageName, String partName);

    protected String getTabXpath() { return null; }
    protected String getUrlSegment() { return null; }
    protected String getPartTypeName() { return this.getClass().getSimpleName(); }
    protected Set<String> getExcludedWords() { return excludedWords; }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        this.driver = driver;
        this.progressUI = progressUI;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        
        try {
            initializeProgress(progressUI, 1); // Will be updated when we know part count
            
            String folderPath = getDirectory(progressUI, getPartTypeName() + " Images");
            if (folderPath == null) {
                progressUI.showCancellation();
                return;
            }
            
            List<File> imageFiles = listImageFilesRecursively(new File(folderPath));
            if (imageFiles.isEmpty()) {
                throw new RuntimeException("No image files found in: " + folderPath);
            }
            
            navigateToPartList(baseUrl);
            List<WebElement> partRows = getPartRows();
            
            processParts(partRows, imageFiles);
            complete(progressUI, "Image upload completed");
        } catch (Exception e) {
            handleError(progressUI, e);
        }
    }
    
    protected String getImageFolderPath(ProgressUI progressUI) {
        try {
            progressUI.setVisible(false);
            return AutomationUI.showDirectoryChooser(
                null, 
                "Select Images Directory for " + getPartTypeName()
            );
        } finally {
            progressUI.setVisible(true);
        }
    }
    
    protected String getImageFolderPath() {
        progressUI.close();
        return AutomationUI.showDirectoryChooser(
            null, "Select Images Directory for " + getPartTypeName());
    }
    
    protected void navigateToPartList(String baseUrl) {
        // If URL segment is provided, use direct navigation
        if (getUrlSegment() != null) {
            driver.get(baseUrl + "/PricingAndConfig/PartList/" + getUrlSegment());
        } 
        // Otherwise use legacy UI navigation (must be overridden)
        else {
            legacyUINavigation(baseUrl);
        }
    }

    // For classes using UI navigation
    protected void legacyUINavigation(String baseUrl) {
        throw new UnsupportedOperationException(
            "Either implement legacyUINavigation() or provide getUrlSegment()");
    }
    
    protected void processParts(List<WebElement> partRows, List<File> imageFiles) 
            throws InterruptedException {
        progressUI.setMainProgressMax(partRows.size());
        progressUI.setStepProgressMax(100);
        progressUI.updateStatus("Processing " + partRows.size() + " parts");
        
        int processed = 0, skipped = 0, failed = 0;
        
        for (int i = 0; i < partRows.size(); i++) {
            WebElement row = partRows.get(i);
            progressUI.updateMainProgress(i);
            progressUI.updateStepProgress(0, "Starting part " + (i+1));
            
            try {
                if (processPartRow(row, imageFiles)) {
                    processed++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                failed++;
                progressUI.updateStepProgress(100, "❌ Failed");
            }
            Thread.sleep(50);
        }
        
        System.out.println("\n=== Results ===");
        System.out.println("Processed: " + processed);
        System.out.println("Skipped: " + skipped);
        System.out.println("Failed: " + failed);
    }
    
    protected boolean processPartRow(WebElement partRow, List<File> imageFiles) 
            throws Exception {
        // Check for existing image
        progressUI.updateStepProgress(10, "Checking existing image");
        if (hasExistingImage(partRow)) {
            progressUI.updateStepProgress(100, "⏭️ Skipped (has image)");
            return false;
        }
        
        // Get and clean part name
        String partName = getPartName(partRow);
        String cleanPartName = cleanName(partName);
        
        // Find matching image
        progressUI.updateStepProgress(30, "Matching image");
        Optional<File> matchingImage = findMatchingImage(cleanPartName, imageFiles);
        if (matchingImage.isEmpty()) {
            progressUI.updateStepProgress(100, "⏭️ No match");
            highlightRow(partRow, "yellow");
            return false;
        }
        
        // Upload image
        progressUI.updateStepProgress(60, "Uploading image");
        uploadImage(partRow, matchingImage.get());
        highlightRow(partRow, "green");
        
        progressUI.updateStepProgress(100, "✅ Uploaded");
        return true;
    }
    
    // Original helper methods preserved:
    protected boolean hasExistingImage(WebElement partRow) {
        try {
            WebElement img = partRow.findElement(
                By.cssSelector("td.part_photo_dropdown_toggle img"));
            String src = img.getDomAttribute("src");
            return !src.contains("camera.svg") && 
                  (src.contains("Thumbnail") || src.matches(".*\\.(jpg|jpeg|png)$"));
        } catch (Exception e) {
            return false;
        }
    }
    
    protected String getPartName(WebElement partRow) {
        WebElement nameCell = partRow.findElements(By.tagName("td")).get(2);
        return ((JavascriptExecutor)driver).executeScript(
            "return arguments[0].childNodes[0].textContent.trim();", nameCell).toString();
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
            System.out.println("⚠️ Retrying with scroll...");
            attemptUpload(partRow, imageFile, true);
        }
    }
    
    private void attemptUpload(WebElement partRow, File imageFile, boolean forceScroll) 
            throws Exception {
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
        
        WebElement fileInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("form#part_photo_form input#file")));
        
        if (forceScroll) {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", 
                fileInput);
        }
        
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = '';", fileInput);
        fileInput.sendKeys(imageFile.getAbsolutePath());
        Thread.sleep(1000);
    }
    
    protected void highlightRow(WebElement row, String color) {
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].style.border = '3px solid " + color + "';", row);
    }
    
    protected void handleError(Exception e) {
        System.err.println("Error in " + getPartTypeName() + " upload: " + e.getMessage());
        progressUI.updateStepProgress(100, "❌ Failed: " + e.getMessage());
        AutomationUI.showMessageDialog(
            null, 
            getPartTypeName() + " upload failed: " + e.getMessage(), 
            "Error", 
            JOptionPane.ERROR_MESSAGE
        );
    }
    
    protected List<File> listImageFilesRecursively(File folder) {
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
}