package automation;

import java.io.*;
import java.util.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.openqa.selenium.interactions.Actions;
import java.time.Duration;
import static automation.helpers.ElementHelper.*;

public class AddingIronmongery {

	public static class IronmongeryItem {
		String partNo, name, cost, unit, type, notes;

		public String getPartNo() {
			return partNo;
		}

		public void setPartNo(String partNo) {
			this.partNo = partNo;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCost() {
			return cost;
		}

		public void setCost(String cost) {
			this.cost = cost;
		}

		public String getUnit() {
			return unit;
		}

		public void setUnit(String unit) {
			this.unit = unit;
		}

		public String getNotes() {
			return notes;
		}

		public void setNotes(String notes) {
			this.notes = notes;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
	}

	public static ArrayList<IronmongeryItem> CSVReader(String csvPath) throws Exception {
		ArrayList<IronmongeryItem> itemList = new ArrayList<>();
		try (Scanner sc = new Scanner(new File(csvPath))) {
			if (sc.hasNextLine())
				sc.nextLine(); // Skip header

			while (sc.hasNextLine()) {
				String[] fields = sc.nextLine().split(",");
				IronmongeryItem item = new IronmongeryItem();
				item.setPartNo(fields[0]);
				item.setName(fields[1]);
				item.setCost(fields[2]);
				item.setUnit(fields[3]);
				item.setType(fields[4]);
				item.setNotes(fields.length > 5 ? fields[5] : "");
				itemList.add(item);
			}
		}
		return itemList;
	}

	public static void importIronmongery(ArrayList<IronmongeryItem> items, WebDriver driver, String baseUrl) {
	    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
	    Actions actions = new Actions(driver);
	    
	    // Navigate to the ironmongery part list
	    driver.get(baseUrl + "/PricingAndConfig/PartList/IM");
	    
	    for (IronmongeryItem item : items) {
	        try {
	            // Scroll to top before clicking Add Part button
	            ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
	            
	            // 1. Click Add Part button with explicit wait
	            WebElement addButton = wait.until(ExpectedConditions.elementToBeClickable(
	                By.id("add_part_button")));
	            addButton.click();
	            
	            // 2. Fill basic fields
	            enterTextById(wait, "part_no", item.getPartNo());
	            enterTextById(wait, "part_name", item.getName());
	            
	            // 3. Select dropdowns
	            selectDropdownByVisibleText(driver, "part_unit_name", item.getUnit());
	            selectDropdownByVisibleText(driver, "part_allocated_unit_name", "each");
	            
	            // 4. Handle special units
	            if (item.getUnit().equalsIgnoreCase("pair") || 
	                item.getUnit().equalsIgnoreCase("set") ||
	                item.getUnit().equalsIgnoreCase("roll")) {
	                enterTextById(wait, "part_allocated_amount_in_purchase_unit", "1");
	            }
	            
	            // 5. Enter cost
	            enterTextById(wait, "part_cost", item.getCost());
	            
	            // 6. Submit part - using the correct ID
	            clickButtonById(driver, "part_dialog_submit_new");
	            
	            // 7. Wait for dialog to close and scroll to top
	            wait.until(ExpectedConditions.invisibilityOfElementLocated(
	                By.id("part_dialog_submit_new")));
	            
	            // Scroll to top using multiple methods for reliability
	            ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, 0)");
	            actions.sendKeys(Keys.HOME).perform();
	            Thread.sleep(500); // Small pause to ensure scroll completes
	            
	        } catch (Exception e) {
	            System.out.println("Error adding part " + item.getPartNo() + ": " + e.getMessage());
	            // Try to close any open dialogs
	            try {
	                driver.findElement(By.cssSelector(".ui-dialog-titlebar-close")).click();
	            } catch (Exception ex) {
	                // Ignore if we can't close it
	            }
	        }
	    }
	}
}