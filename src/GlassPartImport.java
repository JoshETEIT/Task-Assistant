//Created by @AlexLovick 24/10/2024
/**
 * Make sure the right columns are added in list organisation settings, and ticked on the column selector in the part list
 * Should be:
 * Default ones
 * Cost
 * Multiplier
 * Price
 * Waste
 * IPL
 * Stocked
 * Van Stock
 * Special
 * Notes
 * Last Updated
 * Updated by
 */


//Java imports
import java.time.Duration;
import java.awt.SystemColor;
import java.io.*;  
import java.util.Scanner;  
import java.util.ArrayList;
import java.util.List;

//Selenium imports
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.locators.RelativeLocator;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;


public class GlassPartImport {
	
	public static final String demoURL = "https://demo.integratejs.co.uk";
	public static final String NMJURL = "https://nathanmccarter.work/";
	public static final String priceFileURL = "/PricingAndConfig/PriceFileEditor";
	public static final String dboardURL = "https://dboard.integratejs.co.uk";
	public static final String quotesURL = "/Lead/Details/Quote/";
	public static final String localURL = "http://192.168.170.75:8081";
	public static final String STLURL = "https://stilesashwindows.work";
	public static final String twentyOneDegURL = "https://21degrees.work";
	public static final String LDSURL = "https://ldsj.work/";
	public static final String glassURL = "/PricingAndConfig/PartList/GL";
	public static final String e2eURL = "https://e2eit.co.uk";
	
	public static final String userJosh = "holdenj";
	public static final String userLocalTest = "testu";
	public static final String userNMJ = "holdenj";
	public static final String passwordLocalTest = "12345aA";
	public static final String passwordLDN = "Y8Tunm2e";
	public static final String passwordSTL = "9m2JtCRr";
	public static final String passwordTwentyOneDeg = "qvzIL1wI";
	public static final String passwordE2E = "6k0MRZYt";
	public static final String passwordDemo = "F42brzH3";
	public static final String passwordNMJ = "5iuWF7b4";
	public static final String passwordLDS = "F42brzH3";
	
	//Main Java class to run application
    public static void main(String[] args) throws Exception {
    	
    	// Read CSV and get glassPartLists
    	ArrayList<glassPartList> glassPartLists = CSVReader(GetCSV());
    	
    	System.out.println("Reading CSV...");
    	
    	System.out.println(glassPartLists);
    	
    	// Pass the glassPartLists list to login
    	glassListPartAdder(glassPartLists,getSystem());  
    }
    
    public static String getSystem() {

      	Scanner input = new Scanner(System.in);
      	
      	String system;
      	
      	System.out.print("\nPlease pick a system:\n1) Local System\n2) Live System\n");
      	system = (input.nextLine());
      	
      	//input.close();
      	
      	return system;
    }
    
    public static String GetCSV() {
    	String CSV;
    	CSV = "C:\\Eclipse and testing\\Input CSVs\\Glass_to_import_clean.csv";
    	
    	return CSV;
    }
    
    
    public static void hold() {
    	
    	System.out.println("Press the 'Enter' key to continue...");
        
        Scanner scanner = new Scanner(System.in);
        
        while (!scanner.nextLine().trim().equals("")) {
            System.out.println("Waiting for the Enter key...");
        }

        // Proceed with the next steps
        System.out.println("Resuming script...");
    	
    }
    
    //Data Class for holding the parts
    public static class glassPartList{


    	//Timber Part Variables
    	String PartNo,partName,pUnit,cost,obscure;

		public String getPartNo() {
			return PartNo;
		}

		public void setPartNo(String partNo) {
			PartNo = partNo;
		}

		public String getPartName() {
			return partName;
		}

		public void setPartName(String partName) {
			this.partName = partName;
		}

		public String getpUnit() {
			return pUnit;
		}

		public void setpUnit(String pUnit) {
			this.pUnit = pUnit;
		}

		public String getCost() {
			return cost;
		}

		public void setCost(String cost) {
			this.cost = cost;
		}

		public String getObscure() {
			return obscure;
		}

		public void setObscure(String obscure) {
			this.obscure = obscure;
		}
    }
    
    //Reads the CSV data and map it to the data class
    public static ArrayList<glassPartList> CSVReader(String CSV) throws Exception {

        // Create a list to hold multiple glassPartLists
        ArrayList<glassPartList> completePartList = new ArrayList<>();
        
        // Define a scanner object to read the CSV file
        Scanner sc = new Scanner(new File(CSV));
        
        System.out.println(sc);
        System.out.println(sc.hasNextLine());
        
        // Skip the header (assuming the first line has headers)
		
		if (sc.hasNextLine()) { sc.nextLine();}

        // Read the CSV data
        while (sc.hasNextLine()) {
        	        	
            String[] glassPartListDetails = sc.nextLine().split(",");  // Split line by commas
            
            glassPartList glassPartList = new glassPartList();
            
            //PartNumber
            glassPartList.setPartNo(glassPartListDetails[0]);
            
            glassPartList.setPartName(glassPartListDetails[1]);
            
            glassPartList.setpUnit(glassPartListDetails[2]);
            
            glassPartList.setCost(glassPartListDetails[3]);
            
            glassPartList.setObscure(glassPartListDetails[4]);
            
            // Add the glassPartList to the list
            completePartList.add(glassPartList);
        }
        
        
        // Close the scanner
        sc.close();
        
        //Return the user list
        return completePartList;
    }
    
    //Main Selenium code for add the wizard Selenium things  
    public static void glassListPartAdder(ArrayList<glassPartList> glassPartLists, String system) {

    	WebDriver driver = new ChromeDriver();
        String URL = "";
        String username = "";
        String password = "";
        
        if (system.equals("1")) {
        	URL = localURL;
        	username = userLocalTest;
        	password = passwordLocalTest;
        	//URL = STLURL;
        	//username = userLiam;
        	//password = passwordSTL;
        }
        else {
        	//URL = localURL;
        	//username = userLocalTest;
        	//password = passwordLocalTest;
        	URL = LDSURL;
        	username = userJosh;
        	password = passwordLDS;
        }
        
        driver.get(URL);
        //Maximise the window to be full screen but not f11 full screen
        driver.manage().window().maximize();
               
        WebDriverWait wait = new WebDriverWait(driver,Duration.ofSeconds(10));
        WebDriverWait wait2 = new WebDriverWait(driver,Duration.ofSeconds(20));
        
        WebElement loginUsername = wait.until(ExpectedConditions.elementToBeClickable(By.id("login_user_name")));
        loginUsername.click();
        loginUsername.sendKeys(username);        
            
        WebElement loginPassword = wait.until(ExpectedConditions.elementToBeClickable(By.id("login_password")));
        loginPassword.click();
        loginPassword.sendKeys(password); 
            
        WebElement submitLogin = wait.until(ExpectedConditions.elementToBeClickable(By.id("submit_button")));
        submitLogin.click();
            
        try {
        	Thread.sleep(500);
    	} catch (InterruptedException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
        
        hold();

                          
        // -------------------------------------------- Navigate to part list -------------------------------------------- //

        //Now we can start item loop
        driver.get(URL + glassURL);
        Actions actions = new Actions(driver);
        Select dropdown;
        
        for (glassPartList glassPartList : glassPartLists) {
        	
            actions.sendKeys(Keys.HOME).build().perform(); //scroll to top
            
        	WebElement qty = driver.findElement(By.xpath("/html/body/div[7]/div[2]"));
			String qtyText = qty.getText();
			qtyText = qtyText.substring(5);

        	WebElement addPart = wait.until(ExpectedConditions.elementToBeClickable(By.id("add_part_button")));
        	addPart.click();
     
        	
        	//Add the part number 
        	try {
        		
				WebElement partNumber = driver.findElement(By.id("part_no"));
				partNumber.sendKeys(glassPartList.getPartNo());
			} 
        	catch (Exception e) 
        	{
				//print failure to console	
				System.out.println("Failed to add part number");
			}
        	
        	//----------------------------------------------------------------------------------------------------------------------------------------- \\
        	
        	try {
				//Add the Name
				WebElement partName = driver.findElement(By.id("part_name"));
				partName.sendKeys(glassPartList.getPartName());
			} 
        	catch (Exception e) 
        	{
        		System.out.println(glassPartList.getPartNo() + " - failed to add part name");			
        	}
        	
        	//-----------------------------------------------------------------Purchase UOM------------------------------------------------------------------------ \\
        	try {
				WebElement purchaseUOM = wait.until(
						ExpectedConditions.elementToBeClickable(By.id("part_unit_name"))
				    );
				

				dropdown = new Select(purchaseUOM);
				dropdown.selectByVisibleText(glassPartList.getpUnit());
				/**
				WebElement UOM = driver.findElement(By.xpath("//*[@id=\"part_unit_name\"]"));
				UOM.click();
				
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"part_unit_name\"]")));
				

				//replace with proper drop-down selection
				
				String IPL = "//*[@id=\"part_unit_name\"]/option[contains(text(), '" + "m2" + "')]";
							
				WebElement UOFDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(IPL)));

				
				**/

			} catch (Exception e) {
				System.out.println(glassPartList.getPartNo() + " - failed to add part UOM");
			}
        	
        	//-----------------------------------------------------------------Cost of 1 M2------------------------------------------------------------------------ \\
        	
        	
        	try {
        		
        		WebElement COF = driver.findElement(By.id("part_allocated_amount_in_purchase_unit"));
        		COF.sendKeys("1");
				
			} catch (Exception e) {
				// TODO: handle exception
			}
        	
        	
        	
        	//-----------------------------------------------------------------Allocated UOM------------------------------------------------------------------------ \\
        	try {
				WebElement allocatedUOM = wait.until(
						ExpectedConditions.elementToBeClickable(By.id("part_allocated_unit_name"))
				    );
				
				/**
				 
				aUOM.click();
				
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"part_allocated_unit_name\"]")));
				

				String IPL = "//*[@id=\"part_allocated_unit_name\"]/option[contains(text(), '" + "each" + "')]";
							
				WebElement dropdownOption = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(IPL)));
				
				dropdownOption.click();
				*/

				dropdown = new Select(allocatedUOM);
				dropdown.selectByVisibleText("each");
				
				
			} catch (Exception e) {
				System.out.println(glassPartList.getPartNo() + " - failed to add part aUOM");
			}
        	

        	//-----------------------------------------------------------------COST------------------------------------------------------------------------ \\
        	

        	try {
				//Add the Price
				WebElement partPrice = driver.findElement(By.xpath("//*[@id=\"part_cost\"]"));
				partPrice.sendKeys(glassPartList.getCost());
			} 
        	catch (Exception e) 
        	{
        		System.out.println(glassPartList.getPartNo() + " - failed to add part cost");
			}

       
        	//-----------------------------------------------------------------Obscure Glass------------------------------------------------------------------------ \\
        	
        	try {
				//Add the Price
        		if (glassPartList.getObscure().toLowerCase().equals("yes")) {
        			WebElement obscureGlass = driver.findElement(By.id("part_is_obscure_glass"));
        			obscureGlass.click();
        		}
			} 
        	catch (Exception e) 
        	{
				System.out.println("Failed to add glass obscurity...");
			}
        	
        	
			//----------------------------------------------------------------------------------------------------------------------------------------- \\
        	
        	
        	
        	try {
				WebElement submitPart = wait.until(ExpectedConditions.elementToBeClickable(By.className("part_dialog_submit")));
				
				//Comment out on first test 
				submitPart.click();

				if (Integer.valueOf(qtyText) >= 17) {
					Thread.sleep(1000);
					WebElement toTop = wait
							.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"scroll_to_top_div\"]")));
					toTop.click();
				}
				
			} catch (Exception e) {
				System.out.println("Failed to submit or scroll to top");
				e.printStackTrace();
			}
        	

        	wait2.until(D -> {
                try {
                    // Wait for 1.5 seconds
                    Thread.sleep(1500);
                    actions.sendKeys(Keys.HOME).build().perform(); //scroll to top
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Return true to satisfy the condition
                actions.sendKeys(Keys.HOME).build().perform(); //scroll to top
                return true;
            });



    	}
        
        System.out.println("Import Finished");

        }
}