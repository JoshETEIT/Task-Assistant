package automation;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;

import static automation.helpers.ProgressBarHelper.*;
import static automation.helpers.ElementHelper.*;

public class AddNewLead {

    public static boolean testFormSubmission(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            // Update overall progress bar (Starting Test)
            updateProgress("Starting form submission", 10);

            wait.until(ExpectedConditions.urlContains("/Home"));
            //updateStepProgress("Navigating to Add New Lead", 20);

            // Click the 'Add New Lead' button
            driver.findElement(By.xpath("//a[.//span[normalize-space(text())='Add New Lead']]"))
                  .click();
            //updateStepProgress("Clicked Add New Lead", 30);

            // Fill out the form fields
            enterTextById(wait, "label", "Test lead");
            selectDropdownByVisibleText(driver, "source_short_name", "Other");
            selectDropdownByVisibleText(driver, "sector_short_name", "Commercial Install");
            selectDropdownByVisibleText(driver, "type_short_name", "New Windows / Doors (Replacement)");
            selectDropdownByVisibleText(driver, "status_short_name", "New");
            selectDropdownByVisibleText(driver, "method_of_first_contact_short_name", "E-mail");

            enterTextById(wait, "notes", "This lead was created automatically");
            driver.findElement(By.tagName("body")).click();
            //updateStepProgress("Form fields filled", 50);

            // Submit new contact details
            clickButtonWithRetry(driver, "coloured_button_new_contact", 3, 2);
            selectCheckboxOrRadioButton(driver, "customer");
            selectCheckboxOrRadioButton(driver, "main_contact");
            selectCheckboxOrRadioButton(driver, "radio_is_human_1");
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

            enterTextByClass(wait, "dynamic_contact_input", "000-000-0000");
            clickButtonById(driver, "coloured_button_create_box");

            //updateStepProgress("New contact details submitted", 70);

            // Add address information
            clickButtonWithRetry(driver, "coloured_button_new_address", 3, 2);
            updateProgress("Form submission completed", 80);

            enterTextById(wait, "address_line_1", "123 Fake Street");
            enterTextById(wait, "address_line_2", "Suite 100");
            enterTextById(wait, "address_line_3", "Business Park");
            enterTextById(wait, "city", "Faketown");
            enterTextById(wait, "county", "Fakeshire");
            enterTextById(wait, "postcode", "FK12 3AB");

            // Optional postcode lookup
            // clickButtonById(wait, "postcode_lookup_submit");

            updateProgress("Form submission completed", 95);
            clickButtonById(driver, "coloured_button_create_box");

            System.out.println("Form submission completed.");
            return true;

        } catch (Exception e) {
            System.out.println("Form test failed: " + e.getMessage());
            return false;
        }
    }
}
