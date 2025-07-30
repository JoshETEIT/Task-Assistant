package automation;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.Select;
import automation.ui.ProgressUI;  // IMPORT ADDED
import java.time.Duration;
import static automation.helpers.ElementHelper.*;

public class AddNewLead {

    public static boolean testFormSubmission(WebDriver driver, ProgressUI progressUI) {  // ADDED ProgressUI PARAMETER
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(5, "Starting form submission");
            // ===============================
            
            wait.until(ExpectedConditions.urlContains("/Home"));
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(10, "Clicking Add New Lead");
            // ===============================
            
            WebElement addNewLeadButton = driver.findElement(
                By.xpath("//a[.//span[normalize-space(text())='Add New Lead']]"));
            addNewLeadButton.click();
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(15, "Filling basic fields");
            // ===============================
            
            enterTextById(wait, "label", "Test lead");
            selectDropdownByVisibleText(driver, "source_short_name", "Other");
            selectDropdownByVisibleText(driver, "sector_short_name", "Commercial Install");
            selectDropdownByVisibleText(driver, "type_short_name", "New Windows / Doors (Replacement)");
            selectDropdownByVisibleText(driver, "status_short_name", "New");
            selectDropdownByVisibleText(driver, "method_of_first_contact_short_name", "E-mail");
            enterTextById(wait, "notes", "This lead was created automatically");
            driver.findElement(By.tagName("body")).click();
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(30, "Creating new contact");
            // ===============================
            
            clickButtonWithRetry(driver, "coloured_button_new_contact", 3, 2);
            selectCheckboxOrRadioButton(driver, "customer");
            selectCheckboxOrRadioButton(driver, "main_contact");
            selectCheckboxOrRadioButton(driver, "radio_is_human_1");
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(40, "Filling contact details");
            // ===============================
            
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
                System.out.println("Contact type dropdown not found or not selectable.");
            }

            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(60, "Adding phone number");
            // ===============================
            
            enterTextByClass(wait, "dynamic_contact_input", "000-000-0000");
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(70, "Saving contact");
            // ===============================
            
            clickButtonById(driver, "coloured_button_create_box");
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(80, "Adding address");
            // ===============================
            
            clickButtonWithRetry(driver, "coloured_button_new_address", 3, 2);
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(85, "Filling address");
            // ===============================
            
            enterTextById(wait, "address_line_1", "123 Fake Street");
            enterTextById(wait, "address_line_2", "Suite 100");
            enterTextById(wait, "address_line_3", "Business Park");
            enterTextById(wait, "city", "Faketown");
            enterTextById(wait, "county", "Fakeshire");
            enterTextById(wait, "postcode", "FK12 3AB");
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(95, "Saving address");
            // ===============================
            
            clickButtonById(driver, "coloured_button_create_box");
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(100, "✅ Form submitted");
            // ===============================
            
            System.out.println("Form submission completed.");
            return true;

        } catch (Exception e) {
            System.out.println("Form test failed: " + e.getMessage());
            
            // === PROGRESS TRACKING ADDED ===
            progressUI.updateStepProgress(100, "❌ Failed: " + e.getMessage());
            // ===============================
            
            return false;
        }
    }
}