package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import automation.ui.ProgressUI;

import java.time.Duration;

public class PriceFileRuleRemover extends TaskBase {
    
    @Override
    public String getName() {
        return "Price File Rule Remover";
    }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        try {
            progressUI.updateStatus("Starting price file rule removal");
            progressUI.setMainProgressMax(1);
            progressUI.setStepProgressMax(100);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            
            if (!driver.getCurrentUrl().contains("/Home")) {
                driver.get(baseUrl + "/Home");
            }
            
            // Wait for the page to be fully loaded
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[contains(@href,'PricingAndConfig')]")));
            
            // Navigate to Price File Editor through the UI (more reliable than direct URL)
            progressUI.updateStepProgress(10, "Navigating to Price File Editor");
            
            // Then look for Price File Editor specifically
            WebElement priceFileEditorLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@href,'PriceFileEditor')]")));
            priceFileEditorLink.click();
            
            // Wait until we're actually on the Price File Editor page
            wait.until(ExpectedConditions.urlContains("PriceFileEditor"));
            
            // Close various panels that might be open
            progressUI.updateStepProgress(20, "Closing panels");
            closePanels(driver, wait, progressUI);
            
            // Check if there are already no rules
            if (isNoRulesMessagePresent(driver)) {
                progressUI.updateStepProgress(100, "✅ No rules found to remove");
                completeAndHide(progressUI, "No price file rules to remove");
                return;
            }
            
            // Get initial rule count
            progressUI.updateStepProgress(30, "Counting rules");
            int ruleCount = getRuleCount(driver, wait);
            
            if (ruleCount == 0) {
                progressUI.updateStepProgress(100, "✅ No rules found to remove");
                completeAndHide(progressUI, "No price file rules to remove");
                return;
            }
            
            progressUI.updateStatus("Found " + ruleCount + " rules to remove");
            progressUI.setMainProgressMax(ruleCount);
            
            // Expand all groups to see all rules
            progressUI.updateStepProgress(40, "Expanding all groups");
            expandAllGroups(driver, wait);
            
            // Try group deletion first (much faster)
            progressUI.updateStepProgress(50, "Attempting group deletion");
            if (tryGroupDeletion(driver, wait, progressUI)) {
                // Group deletion was successful
                return;
            }
            
            // Fall back to individual rule deletion if group deletion fails
            progressUI.updateStatus("Falling back to individual rule deletion");
            removeAllRules(driver, wait, progressUI, ruleCount);
            
            completeAndHide(progressUI, "Successfully removed " + ruleCount + " price file rules");
            
        } catch (Exception e) {
            handleError(progressUI, e);
        }
    }
    
    private boolean isNoRulesMessagePresent(WebDriver driver) {
        try {
            WebElement noRulesMessage = driver.findElement(
                By.xpath("//td[@class='alert' and contains(., 'No pricing rules')]"));
            return noRulesMessage.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean tryGroupDeletion(WebDriver driver, WebDriverWait wait, ProgressUI progressUI) {
        try {
            // Look for group delete buttons
            java.util.List<WebElement> groupDeleteButtons = driver.findElements(
                By.cssSelector("button.group_delete_all_button"));
            
            if (groupDeleteButtons.isEmpty()) {
                System.out.println("No group delete buttons found, using individual deletion");
                return false;
            }
            
            progressUI.updateStatus("Found " + groupDeleteButtons.size() + " groups to delete");
            progressUI.setMainProgressMax(groupDeleteButtons.size());
            
            int groupsDeleted = 0;
            
            for (WebElement deleteButton : groupDeleteButtons) {
                checkCancellation();
                
                try {
                    progressUI.updateMainProgress(groupsDeleted);
                    String groupName = deleteButton.getDomAttribute("group_short_name");
                    progressUI.updateStepProgress(60 + (groupsDeleted * 40 / groupDeleteButtons.size()), 
                        "Deleting group: " + groupName);
                    
                    // Scroll and click the group delete button
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", deleteButton);
                    Thread.sleep(500);
                    deleteButton.click();
                    
                    // Handle confirmation alert
                    try {
                        Alert alert = driver.switchTo().alert();
                        alert.accept();
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        // No alert present, continue
                    }
                    
                    // Wait for deletion to process
                    Thread.sleep(2000);
                    
                    groupsDeleted++;
                    
                    // Check if we're done after this deletion
                    if (isNoRulesMessagePresent(driver)) {
                        progressUI.updateStepProgress(100, "✅ All rules removed via group deletion");
                        completeAndHide(progressUI, "Successfully removed all rules from " + groupsDeleted + " groups");
                        return true;
                    }
                    
                } catch (Exception e) {
                    System.out.println("Failed to delete group " + (groupsDeleted + 1) + ": " + e.getMessage());
                    groupsDeleted++; // Continue to next group
                }
            }
            
            // If we finished all groups but there are still rules, fall back to individual
            if (!isNoRulesMessagePresent(driver)) {
                System.out.println("Group deletion completed but rules remain, falling back to individual deletion");
                return false;
            }
            
            completeAndHide(progressUI, "Successfully removed all rules from " + groupsDeleted + " groups");
            return true;
            
        } catch (Exception e) {
            System.out.println("Group deletion failed, falling back to individual: " + e.getMessage());
            return false;
        }
    }
    
    // ALL YOUR EXISTING METHODS STAY EXACTLY THE SAME:
    private void closePanels(WebDriver driver, WebDriverWait wait, ProgressUI progressUI) {
        try {
            // Close Price File List panel if present
            try {
                WebElement closePriceFileList = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("/html/body/div[6]/div[2]/img")));
                closePriceFileList.click();
                Thread.sleep(500);
            } catch (Exception e) {
                // Panel might not be open, continue
            }
            
            // Close Variables List panel if present
            try {
                WebElement closeVariablesList = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("/html/body/div[6]/div[6]/img")));
                closeVariablesList.click();
                Thread.sleep(500);
            } catch (Exception e) {
                // Panel might not be open, continue
            }
            
            // Close Pricing Groups List panel if present
            try {
                WebElement closePricingGroupsList = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("/html/body/div[6]/div[7]/img")));
                closePricingGroupsList.click();
                Thread.sleep(500);
            } catch (Exception e) {
                // Panel might not be open, continue
            }
            
        } catch (Exception e) {
            System.out.println("Warning: Could not close all panels: " + e.getMessage());
        }
    }
    
    private int getRuleCount(WebDriver driver, WebDriverWait wait) {
        try {
            WebElement qtyElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("/html/body/div[6]/div[1]/span")));
            String qtyText = qtyElement.getText();
            String[] parts = qtyText.split(" ");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            System.out.println("Could not determine rule count: " + e.getMessage());
            return 0;
        }
    }
    
    private void expandAllGroups(WebDriver driver, WebDriverWait wait) {
        try {
            // Try to find and click expand all button
            WebElement expandAllButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("expand_switch_for_all_groups")));
            expandAllButton.click();
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("Could not expand all groups: " + e.getMessage());
        }
    }
    
    private void removeAllRules(WebDriver driver, WebDriverWait wait, ProgressUI progressUI, int initialRuleCount) {
        int rulesRemoved = 0;
        
        for (int i = 0; i < initialRuleCount && rulesRemoved < initialRuleCount; i++) {
            checkCancellation();
            
            try {
                progressUI.updateMainProgress(rulesRemoved);
                progressUI.updateStepProgress(60 + (rulesRemoved * 40 / initialRuleCount), 
                    "Removing rule " + (rulesRemoved + 1) + "/" + initialRuleCount);
                
                // Find and click the pencil edit button
                java.util.List<WebElement> allEditIcons = driver.findElements(By.cssSelector("img.edit_icon"));
                WebElement pencilEditButton = null;
                
                for (WebElement icon : allEditIcons) {
                    String src = icon.getDomAttribute("src");
                    String title = icon.getDomAttribute("title");
                    if (src != null && src.contains("pencil.png") && "Edit".equals(title)) {
                        pencilEditButton = icon;
                        break;
                    }
                }
                
                if (pencilEditButton == null) {
                    throw new RuntimeException("No pencil edit button found");
                }
                
                // Scroll and click the pencil edit button
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", pencilEditButton);
                Thread.sleep(500);
                pencilEditButton.click();
                
                // Wait for the modal dialog to appear
                progressUI.updateStepProgress(70, "Waiting for edit dialog to open");
                Thread.sleep(2000);
                
                // Now we need to find the delete button INSIDE the modal dialog
                // The modal dialog will have a higher z-index and different structure
                
                // Method 1: Look for delete button in modal dialog
                WebElement deleteButton = null;
                try {
                    // Try to find delete button in modal dialog
                    deleteButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("div.ui-dialog button:contains('Delete'), " +
                                      "div.ui-dialog input[value='Delete'], " +
                                      "div[style*='z-index'] button:contains('Delete')")));
                } catch (Exception e) {
                    // Method 2: Look for any button with text "Delete" that's now visible
                    java.util.List<WebElement> allDeleteButtons = driver.findElements(
                        By.xpath("//button[contains(text(), 'Delete')]"));
                    
                    for (WebElement btn : allDeleteButtons) {
                        if (btn.isDisplayed() && btn.isEnabled()) {
                            deleteButton = btn;
                            break;
                        }
                    }
                }
                
                if (deleteButton == null) {
                    // Debug: See what buttons are available in the modal
                    System.out.println("No delete button found in modal. Available buttons:");
                    java.util.List<WebElement> allButtons = driver.findElements(By.tagName("button"));
                    for (WebElement btn : allButtons) {
                        if (btn.isDisplayed()) {
                            System.out.println("Button: " + btn.getText() + " - " + btn.getDomAttribute("outerHTML"));
                        }
                    }
                    throw new RuntimeException("Delete button not found in modal dialog");
                }
                
                // Click the delete button
                progressUI.updateStepProgress(80, "Clicking delete button");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", deleteButton);
                Thread.sleep(1000);
                
                // Handle confirmation alert
                try {
                    Alert alert = driver.switchTo().alert();
                    alert.accept();
                    Thread.sleep(500);
                } catch (Exception e) {
                    // No alert present, continue
                }
                
                // Wait for deletion to process and modal to close
                progressUI.updateStepProgress(90, "Waiting for deletion to complete");
                Thread.sleep(2000);
                
                rulesRemoved++;
                
            } catch (Exception e) {
                System.out.println("Failed to remove rule " + (rulesRemoved + 1) + ": " + e.getMessage());
                
                // If we're stuck in a modal, try to close it
                try {
                    // Look for modal close buttons
                    java.util.List<WebElement> closeButtons = driver.findElements(
                        By.cssSelector("div.ui-dialog-titlebar-close, .ui-dialog-titlebar-close"));
                    for (WebElement closeBtn : closeButtons) {
                        if (closeBtn.isDisplayed()) {
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeBtn);
                            Thread.sleep(1000);
                            break;
                        }
                    }
                } catch (Exception ex) {
                    // Ignore close errors
                }
                
                // Refresh and try again
                try {
                    driver.navigate().refresh();
                    Thread.sleep(2000);
                    closePanels(driver, wait, progressUI);
                    expandAllGroups(driver, wait);
                } catch (Exception ex) {
                    // Ignore refresh errors
                }
            }
        }
        
        progressUI.updateStatus("Successfully removed " + rulesRemoved + " rules");
    }
}