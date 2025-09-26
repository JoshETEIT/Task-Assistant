package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import automation.ui.ProgressUI;
import automation.ui.AutomationUI;
import javax.swing.*;

import java.time.Duration;
import static automation.helpers.ElementHelper.*;

public class AddLeadTask implements AutomationTask {
    
    @Override
    public String getName() {
        return "Add Lead";
    }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        try {
            // Ask user for number of leads to add
            String leadCountStr = AutomationUI.showInputDialog(
                null, 
                "How many leads would you like to add?", 
                "Add Lead Configuration"
            );
            
            if (leadCountStr == null || leadCountStr.trim().isEmpty()) {
                progressUI.showCancellation();
                returnToTaskSelection(progressUI);
                return;
            }
            
            int leadCount;
            try {
                leadCount = Integer.parseInt(leadCountStr.trim());
                if (leadCount <= 0) {
                    AutomationUI.showMessageDialog(
                        null, 
                        "Please enter a positive number", 
                        "Invalid Input", 
                        JOptionPane.WARNING_MESSAGE
                    );
                    returnToTaskSelection(progressUI);
                    return;
                }
            } catch (NumberFormatException e) {
                AutomationUI.showMessageDialog(
                    null, 
                    "Please enter a valid number", 
                    "Invalid Input", 
                    JOptionPane.WARNING_MESSAGE
                );
                returnToTaskSelection(progressUI);
                return;
            }
            
            progressUI.setMainProgressMax(leadCount);
            progressUI.setStepProgressMax(100);
            
            for (int i = 0; i < leadCount; i++) {
                progressUI.updateMainProgress(i);
                progressUI.updateStatus("Adding lead " + (i+1) + "/" + leadCount);
                progressUI.updateStepProgress(0, "Starting new lead");
                
                boolean success = submitLeadForm(driver, progressUI, wait);
                System.out.println((success ? "✅" : "❌") + " Lead addition result");
                
                // Return to home page after each lead (except the last one)
                if (i < leadCount - 1) {
                    driver.get(baseUrl + "/Home");
                }
            }
            
            progressUI.showCompletion();
            Thread.sleep(1000);
            
            // Return to task selection instead of closing
            returnToTaskSelection(progressUI);
            
        } catch (Exception e) {
            progressUI.updateStepProgress(100, "❌ Failed: " + e.getMessage());
            AutomationUI.showMessageDialog(
                null, 
                "Lead addition failed: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE
            );
            returnToTaskSelection(progressUI);
        }
    }
    
    private void returnToTaskSelection(ProgressUI progressUI) {
        SwingUtilities.invokeLater(() -> {
            progressUI.close();
            System.out.println("Add Lead task completed - control should return to main application");
        });
    }
    
    private boolean submitLeadForm(WebDriver driver, ProgressUI progressUI, WebDriverWait wait) {
        try {
            progressUI.updateStepProgress(5, "Starting form submission");
            
            wait.until(ExpectedConditions.urlContains("/Home"));
            
            progressUI.updateStepProgress(10, "Clicking Add New Lead");
            WebElement addNewLeadButton = driver.findElement(
                By.xpath("//a[.//span[normalize-space(text())='Add New Lead']]"));
            addNewLeadButton.click();
            
            progressUI.updateStepProgress(15, "Filling basic fields");
            enterTextById(wait, "label", "Test lead");
            selectDropdownByVisibleText(driver, "source_short_name", "Other");
            selectDropdownByVisibleText(driver, "sector_short_name", "Commercial Install");
            selectDropdownByVisibleText(driver, "type_short_name", "New Windows / Doors (Replacement)");
            selectDropdownByVisibleText(driver, "status_short_name", "New");
            selectDropdownByVisibleText(driver, "method_of_first_contact_short_name", "E-mail");
            enterTextById(wait, "notes", "This lead was created automatically");
            driver.findElement(By.tagName("body")).click();
            
            progressUI.updateStepProgress(30, "Creating new contact");
            clickButtonWithRetry(driver, "coloured_button_new_contact", 3, 2);
            selectCheckboxOrRadioButton(driver, "customer");
            selectCheckboxOrRadioButton(driver, "main_contact");
            selectCheckboxOrRadioButton(driver, "radio_is_human_1");
            
            progressUI.updateStepProgress(40, "Filling contact details");
            selectDropdownByVisibleText(driver, "human_title", "Mrs");
            enterTextById(wait, "human_forename", "Jane");
            enterTextById(wait, "human_surname", "Doe");
            enterTextById(wait, "contact_notes", "This is not a real person");
            selectMultiCheckboxDropdown(driver, new String[]{"quantity_surveyor", "landlord"});

            try {
                WebElement contactTypeDropdown = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("#lead_box_editor div[style='width: 340px;'] select")));
                new Select(contactTypeDropdown).selectByVisibleText("Personal");
            } catch (Exception e) {
                System.out.println("Contact type dropdown not found, continuing anyway");
            }

            progressUI.updateStepProgress(60, "Adding phone number");
            enterTextByClass(wait, "dynamic_contact_input", "000-000-0000");
            
            progressUI.updateStepProgress(70, "Saving contact");
            clickButtonById(driver, "coloured_button_create_box");
            
            progressUI.updateStepProgress(80, "Adding address");
            // Simple retry - just call the method again if it fails
            if (!clickButtonWithRetry(driver, "coloured_button_new_address", 3, 2)) {
                progressUI.updateStepProgress(100, "❌ Failed to add address");
                return false;
            }
            
            progressUI.updateStepProgress(85, "Filling address");
            enterTextById(wait, "address_line_1", "123 Fake Street");
            enterTextById(wait, "address_line_2", "Suite 100");
            enterTextById(wait, "address_line_3", "Business Park");
            enterTextById(wait, "city", "Faketown");
            enterTextById(wait, "county", "Fakeshire");
            enterTextById(wait, "postcode", "FK12 3AB");
            
            progressUI.updateStepProgress(95, "Saving address");
            clickButtonById(driver, "coloured_button_create_box");
            clickButtonById(driver, "convert_to_real_lead_button");
            
            progressUI.updateStepProgress(100, "✅ Lead created successfully");
            return true;

        } catch (Exception e) {
            progressUI.updateStepProgress(100, "❌ Failed: " + e.getMessage());
            System.out.println("Lead creation failed: " + e.getMessage());
            return false;
        }
    }
}