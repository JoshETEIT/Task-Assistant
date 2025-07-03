//Created by @AlexLovick 24/10/2024

//Java imports
import java.time.Duration;
import java.awt.SystemColor;
import java.io.*;  
import java.util.Scanner;  
import java.util.ArrayList;

//WebDriverManager import
import io.github.bonigarcia.wdm.WebDriverManager;

//Selenium imports
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.locators.RelativeLocator;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Things to update between uses:
 * 1. URL for client
 * 2. Login details
 * 3. CSV path
 * 4. xPaths for elements in the add part box
 * 5. Add the UOMs from the import csv to the live/test systems
 * 		use I2001 and I2201
 * 6. Remove I2001 and I2201 from the CSV while it runs
 * 7. Add them back after
 */
public class AddingIronmongery {
	
	public static final String localURL = "";
	public static final String STLURL = "";
	public static final String twentyOneDegURL = "";
	public static final String LDSURL = "";
	public static final String ironmongeryURL = "";
	
	public static final String userJosh = "";
	public static final String passwordDemo = "";
	public static final String userLocalTest = "";
	public static final String passwordLocalTest = "";
	public static final String passwordLDN = "";
	public static final String passwordSTL = "";
	public static final String passwordTwentyOneDeg = "";
	public static final String passwordLDS = "";
	
	
	//Main Java class to run application
    public static void main(String[] args) throws Exception {
    	
        // Setting up WebDriverManager to manage the ChromeDriver binary
        WebDriverManager.chromedriver().setup();
    	
    	// Read CSV and get IronmongeryLists
    	ArrayList<IronmongeryList> IronmongeryLists = CSVReader(GetCSV());
    	
    	System.out.println("Reading CSV...");
    	
    	System.out.println(IronmongeryLists);
    	
    	// Pass the IronmongeryLists list to login
    	IronmongeryPartAdder(IronmongeryLists,getSystem());
    }
    
    public static String getSystem() {

      	Scanner input = new Scanner(System.in);
      	
      	String system;
      	
      	System.out.print("\nPlease pick a system:\n1) Local System\n2) Live System\n");
      	system = (input.nextLine());
      	
      	//input.close();
      	
      	return system;
    }
    
    //Gets the csv file path from the user input
    public static String GetCSV() {
    	String CSV;
    	CSV = "C:\\Users\\JoshHolden\\OneDrive - EndToEnd IT Limited\\Documents\\Hardware_to_import_clean.csv";
    	
    	return CSV;
    	
    }
    
    public static void hold() {
    	
    	System.out.println("Pick a DB, and then press the 'Enter' key to continue...");
        
        Scanner scanner = new Scanner(System.in);
        
        while (!scanner.nextLine().trim().equals("")) {
            System.out.println("Waiting for the Enter key...");
        }

        // Proceed with the next steps
        System.out.println("Resuming script...\n");
    	
    }
    
    
    //Data Class for holding the parts
    public static class IronmongeryList{

    	//CSV headers
    	String PartNo,Name,Cost,Unit,Type,Notes;

		public String getPartNo() {
			return PartNo;
		}

		public void setPartNo(String partNo) {
			PartNo = partNo;
		}

		public String getName() {
			return Name;
		}

		public void setName(String name) {
			Name = name;
		}

		public String getCost() {
			return Cost;
		}

		public void setCost(String cost) {
			Cost = cost;
		}

		public String getUnit() {
			return Unit;
		}

		public void setUnit(String unit) {
			Unit = unit;
		}

		public String getNotes() {
			return Notes;
		}

		public void setNotes(String notes) {
			Notes = notes;
		}
		
		public String getType() {
			return Type;
		}
		
		public void setType(String type) {
			Type = type;
		}
    }
    
    //Reads the CSV data and map it to the data class
    public static ArrayList<IronmongeryList> CSVReader(String CSV) throws Exception {

        // Create a list to hold multiple IronmongeryLists
        ArrayList<IronmongeryList> userList = new ArrayList<>();
        
        // Define a scanner object to read the CSV file
        //Scanner sc = new Scanner(new File("C:\\Users\\LiamSimkin\\Downloads\\Hardware_to_import.csv"));
        Scanner sc = new Scanner(new File(CSV));
        
        System.out.println(sc);
        System.out.println(sc.hasNextLine());
        
        // Skip the header (assuming the first line has headers)
		
		if (sc.hasNextLine()) { sc.nextLine();}

        // Read the CSV data
        while (sc.hasNextLine()) {
        	        	
            String[] IronmongeryListDetails = sc.nextLine().split(",");  // Split line by commas
            
            IronmongeryList IronmongeryList = new IronmongeryList();
            
            //Part Number
            IronmongeryList.setPartNo(IronmongeryListDetails[0]);
            
            //Part Name
            IronmongeryList.setName(IronmongeryListDetails[1]);
            
            //Cost
            IronmongeryList.setCost(IronmongeryListDetails[2]);
            
            //Set the units
            IronmongeryList.setUnit(IronmongeryListDetails[3]);
            
            //Type
            IronmongeryList.setType(IronmongeryListDetails[4]);
            
            //Notes
            IronmongeryList.setNotes(IronmongeryListDetails[5]);
            
            userList.add(IronmongeryList);
        }
        
        // Close the scanner
        sc.close();
        
        //Return the user list
        return userList;
    }
    
    //Main Selenium code for add the wizard selenium things  
    public static void IronmongeryPartAdder(ArrayList<IronmongeryList> IronmongeryLists, String system) {

        WebDriver driver = new ChromeDriver();
        String URL = "";
        String username = "";
        String password = "";
        
        if (system.equals("1")) {
        	URL = localURL;
        	username = userLocalTest;
        	password = passwordLocalTest;

        }
        else {
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
        driver.get(URL + ironmongeryURL);
        Actions actions = new Actions(driver);
        
        for (IronmongeryList IronmongeryList : IronmongeryLists) {
        	
        	//Locate and click the add new part button
        	
        	WebElement qty = driver.findElement(By.xpath("/html/body/div[7]/div[2]"));
			String qtyText = qty.getText();
			qtyText = qtyText.substring(5);
        	
        	WebElement addPart = wait.until(ExpectedConditions.elementToBeClickable(By.id("add_part_button")));
        	addPart.click();
     
        	
        	//Add the part number 
        	try {
        		
				WebElement partNumber = driver.findElement(By.id("part_no"));
				partNumber.sendKeys(IronmongeryList.getPartNo()); 
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
				partName.sendKeys(IronmongeryList.getName());
			} 
        	catch (Exception e) 
        	{
				System.out.println(IronmongeryList.getPartNo() + " - failed to add part name");
			}
        	
        	//-----------------------------------------------------------------Purchase UOM------------------------------------------------------------------------ \\
        	try {
				WebElement PurchaseUOF = wait.until(
						ExpectedConditions.elementToBeClickable(By.id("part_unit_name"))
				    );
				
				WebElement UOM = driver.findElement(By.xpath("//*[@id=\"part_unit_name\"]"));
				UOM.click();
				
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"part_unit_name\"]")));
				

				String IPL = "//*[@id=\"part_unit_name\"]/option[contains(text(), '" + IronmongeryList.getUnit().toLowerCase() + "')]";
							
				WebElement dropdownOption = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(IPL)));
				
				dropdownOption.click();
			} catch (Exception e) {
				System.out.println(IronmongeryList.getPartNo() + " - failed to add part UOF");
			}
        	
        	//-----------------------------------------------------------------Allocated UOM------------------------------------------------------------------------ \\
        	try {
				WebElement PurchaseUOF = wait.until(
						ExpectedConditions.elementToBeClickable(By.id("part_allocated_unit_name"))
				    );
				
				WebElement UOM = driver.findElement(By.xpath("//*[@id=\"part_allocated_unit_name\"]"));
				UOM.click();
				
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"part_allocated_unit_name\"]")));
				

				String IPL = "//*[@id=\"part_allocated_unit_name\"]/option[contains(text(), '" + "each" + "')]";
							
				WebElement dropdownOption = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(IPL)));
				
				dropdownOption.click();
			} catch (Exception e) {
				System.out.println(IronmongeryList.getPartNo() + " - failed to add part UOF");
			}
        	
        	//-----------------------------------------------------------------Purchase Pair UOM------------------------------------------------------------------ \\
        	
        	
        	if(IronmongeryList.getUnit().trim().toLowerCase().equals("pair"))
        	{
        	       		
        		WebElement pairPrice = driver.findElement(By.id("part_allocated_amount_in_purchase_unit"));
				pairPrice.sendKeys("1");

        	}
        	        	
        	if(IronmongeryList.getUnit().trim().toLowerCase().equals("set"))
        	{
        	       		
        		WebElement pairPrice = driver.findElement(By.id("part_allocated_amount_in_purchase_unit"));
				pairPrice.sendKeys("1");

        	}
        	        	
        	if(IronmongeryList.getUnit().trim().toLowerCase().equals("roll"))
        	{
        	       		
        		WebElement pairPrice = driver.findElement(By.id("part_allocated_amount_in_purchase_unit"));
				pairPrice.sendKeys("1");

        	}
        	
        	//----------------------------------------------------------------------------------------------------------------------------------------- \\
        	

        	try {
				//Add the Price
				WebElement partPrice = driver.findElement(By.xpath("//*[@id=\"part_cost\"]"));
				partPrice.sendKeys(IronmongeryList.getCost());
			} 
        	catch (Exception e) 
        	{
				System.out.println(IronmongeryList.getPartNo() + " - failed to add part cost");
			}

        	//------------------------------------------------------------------- IPL ------------------------------------------------------------------- \\
        	try {
				WebElement partIpl = wait.until(
						ExpectedConditions.elementToBeClickable(By.id("part_ipl"))
				    );   	
				WebElement newLeadTitle = driver.findElement(By.xpath("//*[@id=\"part_ipl\"]"));
				newLeadTitle.click();
				
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"part_ipl\"]")));
				

				if (IronmongeryList.getType().equals("Casement")) {
					IronmongeryList.setType("Case");
				}
				String IPL = "//*[@id=\"part_ipl\"]/option[contains(text(), '" + IronmongeryList.getType() + "')]";
							
				WebElement dropdownOption = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(IPL)));
				
				dropdownOption.click();
			} catch (Exception e) {
				System.out.println(IronmongeryList.getPartNo() + " - failed to add part IPL");
			}
			
        	
        	//------------------------------------------------------------------- Notes ------------------------------------------------------------------- \\
        	try {
				//Add the Price
				
        		if (!IronmongeryList.getNotes().equals("N/A")) { 
        			WebElement partPrice = driver.findElement(By.id("part_notes"));
        			partPrice.sendKeys(IronmongeryList.getNotes());
        		}
        	} 
        	catch (Exception e) 
        	{
				System.out.println(IronmongeryList.getPartNo() + " - failed to add part notes");
			}
        	
        	
			//----------------------------------------------------------------------------------------------------------------------------------------- \\
        	
        	try {
				WebElement submitPart = wait.until(ExpectedConditions.elementToBeClickable(By.className("part_dialog_submit")));
				
				//Comment out on first test 
				submitPart.click();
				System.out.println(IronmongeryList.getPartNo() + " added successfully");

				if (Integer.valueOf(qtyText) >= 17) {
					Thread.sleep(1500);
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
        
        System.out.println("Import finished");
        }
}
