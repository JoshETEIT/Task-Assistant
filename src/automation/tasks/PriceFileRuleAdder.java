package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import automation.TestSuite;
import automation.helpers.CsvReader;
import automation.helpers.ElementHelper;
import automation.helpers.ElementHelper.LocatorType;
import automation.helpers.ElementHelper.Screenshot;
import automation.ui.ProgressUI;

import java.time.Duration;
import java.util.*;

public class PriceFileRuleAdder extends TaskBase {
    
    @Override
    public String getName() {
        return "Price File Rule Adder";
    }
    
    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        try {
            progressUI.updateStatus("Starting price file rule import");
            progressUI.setMainProgressMax(1);
            progressUI.setStepProgressMax(100);
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            
            // Get CSV files
            String csvPath = getCsvFile(progressUI, "Price File Rules");
            if (csvPath == null) return;

            String variablesCsvPath = getCsvFile(progressUI, "(Optional) Price File Variables", false);
            
            // Read CSV data
            progressUI.updateStepProgress(10, "Reading rules CSV...");
            List<RuleInfo> rules = CsvReader.read(csvPath, this::createRule);
            
            List<VariableInfo> variables = new ArrayList<>();
            if (variablesCsvPath != null && !variablesCsvPath.trim().isEmpty()) {
                progressUI.updateStepProgress(20, "Reading variables CSV...");
                variables = CsvReader.read(variablesCsvPath, this::createVariable);
            }
            
            // Navigate and setup
            progressUI.updateStepProgress(30, "Navigating to Price File Editor");
            navigateToPriceFileEditor(driver, baseUrl, wait);
            
            progressUI.updateStepProgress(40, "Closing panels");
            closeAllPanels(driver);
            
            // Process groups - open once, check and create, then close
            progressUI.updateStepProgress(45, "Processing groups");
            Set<String> existingGroups = checkAndCreateGroups(driver, wait, rules, progressUI);
            progressUI.updateStatus("Existing groups: " + existingGroups.size());
            
            // Process variables - open once, check and create, then close  
            progressUI.updateStepProgress(60, "Processing variables");
            if (!variables.isEmpty()) {
                checkAndCreateVariables(driver, wait, variables, progressUI);
            }
            
            // Import rules
            progressUI.updateStepProgress(70, "Importing rules");
            progressUI.setMainProgressMax(rules.size());
            importRules(driver, wait, rules, progressUI);
            
            completeAndHide(progressUI, "Successfully imported " + rules.size() + " price file rules");
            
        } catch (Exception e) {
            handleError(progressUI, e);
        }
    }
    
    private Set<String> checkAndCreateGroups(WebDriver driver, WebDriverWait wait, List<RuleInfo> rules, ProgressUI progressUI) {
        Set<String> existingGroups = new HashSet<>();
        
        try {
            // Open groups panel once
            openPanel(driver, "pricing_groups_resize_toggle");
            
            // Check existing groups
            progressUI.updateStatus("Checking existing groups");
            findElementsBySelectors(driver, 
                new String[]{"samp.read_only_div", "input.pricing_group_edit"}, 
                existingGroups);
            
            // Create new groups if needed
            Set<String> uniqueGroups = getUniqueGroups(rules);
            Set<String> groupsToCreate = new HashSet<>(uniqueGroups);
            groupsToCreate.removeAll(existingGroups);
            
            if (!groupsToCreate.isEmpty()) {
                progressUI.updateStatus("Creating " + groupsToCreate.size() + " new groups");
                createGroupsInOpenPanel(driver, groupsToCreate, progressUI);
            } else {
                progressUI.updateStatus("All groups already exist - skipping group creation");
            }
            
            // Close groups panel once
            closePanel(driver, "img.pricing_groups_close_button");
            
        } catch (Exception e) {
            System.out.println("Error processing groups: " + e.getMessage());
        }
        
        return existingGroups;
    }
    
    private void checkAndCreateVariables(WebDriver driver, WebDriverWait wait, List<VariableInfo> variables, ProgressUI progressUI) {
        Set<String> existingVariables = new HashSet<>();
        
        try {
            // Open variables panel once
            openPanel(driver, "pricing_variables_resize_toggle");
            
            // Check existing variables
            progressUI.updateStatus("Checking existing variables");
            findElementsBySelectors(driver,
                new String[]{"samp.read_only_div", "input.pricing_variable_edit"},
                existingVariables);
            
            // Create new variables if needed
            List<VariableInfo> newVariables = filterExistingVariables(variables, existingVariables);
            
            if (!newVariables.isEmpty()) {
                progressUI.updateStatus("Creating " + newVariables.size() + " new variables");
                createVariablesInOpenPanel(driver, newVariables, progressUI);
            } else {
                progressUI.updateStatus("All variables already exist - skipping variable creation");
            }
            
            // Close variables panel once
            closePanel(driver, "img.pricing_variables_close_button");
            
        } catch (Exception e) {
            System.out.println("Error processing variables: " + e.getMessage());
        }
    }
    
    private void createGroupsInOpenPanel(WebDriver driver, Set<String> groups, ProgressUI progressUI) {
        int groupSortOrderCount = 10;
        
        for (String group : groups) {
            checkCancellation();
            createSingleGroup(driver, group, groupSortOrderCount);
            groupSortOrderCount += 10;
            progressUI.updateStatus("Created group: " + group);
        }
    }
    
    private void createVariablesInOpenPanel(WebDriver driver, List<VariableInfo> variables, ProgressUI progressUI) {
        for (VariableInfo variable : variables) {
            checkCancellation();
            createSingleVariable(driver, variable);
            progressUI.updateStatus("Created variable: " + variable.variable);
        }
    }
    
    private RuleInfo createRule(String[] fields) {
        RuleInfo rule = new RuleInfo();
        if (fields.length >= 8) {
            rule.sortOrder = fields[0];
            rule.name = fields[1];
            rule.group = fields[2];
            rule.loop = fields[3];
            rule.condition = fields[4];
            rule.quantity = fields[5];
            rule.value = fields[6];
            rule.markup = fields[7];
        }
        return rule;
    }
    
    private VariableInfo createVariable(String[] fields) {
        VariableInfo variable = new VariableInfo();
        if (fields.length >= 2) {
            variable.variable = fields[0];
            variable.value = fields[1];
        }
        return variable;
    }
    
    private void navigateToPriceFileEditor(WebDriver driver, String baseUrl, WebDriverWait wait) {
        if (!driver.getCurrentUrl().contains("/Home")) {
            driver.get(baseUrl + "/Home");
        }
        
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//a[contains(@href,'PricingAndConfig')]")));
        
        ElementHelper.clickButton(driver, LocatorType.XPATH, 
            "//a[contains(@href,'PriceFileEditor')]", Screenshot.ON, 3);
        
        wait.until(ExpectedConditions.urlContains("PriceFileEditor"));
    }
    
    private void closeAllPanels(WebDriver driver) {
        closePanelIfOpen(driver, "img.pricefile_list_close_button");
        closePanelIfOpen(driver, "img.pricing_variables_close_button");
        closePanelIfOpen(driver, "img.pricing_groups_close_button");
    }
    
    private void closePanelIfOpen(WebDriver driver, String selector) {
        ElementHelper.clickButton(driver, LocatorType.CSS, selector, Screenshot.OFF, 1);
    }
    
    private void openPanel(WebDriver driver, String toggleId) throws InterruptedException {
        ElementHelper.clickButton(driver, LocatorType.ID, toggleId, Screenshot.OFF, 2);
        Thread.sleep(500);
    }
    
    private void closePanel(WebDriver driver, String closeSelector) throws InterruptedException {
        ElementHelper.clickButton(driver, LocatorType.CSS, closeSelector, Screenshot.OFF, 2);
        Thread.sleep(500);
    }
    
    private void findElementsBySelectors(WebDriver driver, String[] selectors, Set<String> resultSet) {
        for (String selector : selectors) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                for (WebElement element : elements) {
                    String text = selector.contains("input") ? 
                        element.getDomAttribute("value") : element.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        resultSet.add(text.trim());
                    }
                }
            } catch (Exception e) {
                // Continue with next selector
            }
        }
    }
    
    private Set<String> getUniqueGroups(List<RuleInfo> rules) {
        Set<String> groups = new HashSet<>();
        for (RuleInfo rule : rules) {
            if (rule.group != null && !rule.group.trim().isEmpty()) {
                groups.add(rule.group.trim());
            }
        }
        return groups;
    }
    
    private List<VariableInfo> filterExistingVariables(List<VariableInfo> variables, Set<String> existingVariables) {
        List<VariableInfo> newVariables = new ArrayList<>();
        for (VariableInfo variable : variables) {
            if (!existingVariables.contains(variable.variable.trim())) {
                newVariables.add(variable);
            }
        }
        return newVariables;
    }
    
    private void createSingleGroup(WebDriver driver, String groupName, int sortOrder) {
        try {
            WebElement nameInput = driver.findElement(By.id("pricing_group_name__new_"));
            WebElement sortInput = driver.findElement(By.id("pricing_group_sortorder__new_"));
            
            nameInput.clear();
            nameInput.sendKeys(groupName);
            
            sortInput.clear();
            sortInput.sendKeys(Integer.toString(sortOrder));
            
            ElementHelper.clickButton(driver, LocatorType.ID, "pricing_group_submit__new_", Screenshot.OFF, 2);
            Thread.sleep(2000);
            
        } catch (Exception e) {
            System.out.println("Error creating group '" + groupName + "': " + e.getMessage());
        }
    }
    
    private void createSingleVariable(WebDriver driver, VariableInfo variable) {
        try {
            WebElement nameInput = driver.findElement(By.id("pricing_variable_name_0"));
            WebElement valueInput = driver.findElement(By.id("pricing_variable_value_0"));
            
            nameInput.clear();
            nameInput.sendKeys(variable.variable);
            
            valueInput.clear();
            valueInput.sendKeys(variable.value);
            
            ElementHelper.clickButton(driver, LocatorType.ID, "pricing_variable_submit_0", Screenshot.OFF, 2);
            Thread.sleep(2000);
            
        } catch (Exception e) {
            System.out.println("Error creating variable '" + variable.variable + "': " + e.getMessage());
        }
    }
    
    private void importRules(WebDriver driver, WebDriverWait wait, List<RuleInfo> rules, ProgressUI progressUI) {
        int importedCount = 0;
        
        for (int i = 0; i < rules.size() && !TestSuite.isTaskCancelled(); i++) {
            RuleInfo rule = rules.get(i);
            progressUI.updateMainProgress(i);
            progressUI.updateStepProgress(70 + (i * 30 / rules.size()), 
                "Importing rule " + (i + 1) + "/" + rules.size() + " (" + importedCount + " imported)");
            
            try {
                checkCancellation();
                createSingleRule(driver, wait, rule);
                importedCount++;
                progressUI.updateStatus("Imported rule: " + rule.name);
                
            } catch (Exception e) {
                System.out.println("Error importing rule '" + rule.name + "': " + e.getMessage());
                closeOpenDialog(driver);
            }
        }
        
        progressUI.updateStatus("Import complete: " + importedCount + " rules added");
    }
    
    private void createSingleRule(WebDriver driver, WebDriverWait wait, RuleInfo rule) throws InterruptedException {
        ElementHelper.clickButton(driver, LocatorType.ID, "add_new_pricing_rule", Screenshot.ON, 3);
        Thread.sleep(500);
        
        fillRuleForm(driver, wait, rule);
        
        // Add cancellation check before final submission
        checkCancellation();
        ElementHelper.clickButton(driver, LocatorType.XPATH, 
            "//button[span[contains(text(), 'Create New Rule')]]", Screenshot.ON, 3);
        
        scrollToTop(driver);
    }
    
    private void closeOpenDialog(WebDriver driver) {
        ElementHelper.clickButton(driver, LocatorType.CSS, ".ui-dialog-titlebar-close", Screenshot.OFF, 1);
    }
    
    private void fillRuleForm(WebDriver driver, WebDriverWait wait, RuleInfo rule) {
        // Fill all fields directly without tabbing between them
        ElementHelper.enterCodeMirrorByLabel(driver, "Name", rule.name);
        ElementHelper.selectDropdownByLabel(driver, "Group", rule.group);
        
        String loopValue = "n/a".equals(rule.loop) ? "--- n/a ---" : rule.loop;
        ElementHelper.selectDropdownByLabel(driver, "Loop", loopValue);
        
        fillSortOrder(driver, wait, rule.sortOrder);
        
        // Fill CodeMirror fields directly - no tabbing needed!
        ElementHelper.enterCodeMirrorByLabel(driver, "Condition", rule.condition);
        ElementHelper.enterCodeMirrorByLabel(driver, "Quantity", rule.quantity);
        ElementHelper.enterCodeMirrorByLabel(driver, "Value", rule.value);
        ElementHelper.enterCodeMirrorByLabel(driver, "Markup", rule.markup);
    }
    
    private void fillSortOrder(WebDriver driver, WebDriverWait wait, String sortOrder) {
        try {
            WebElement sortOrderInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[.//div[@class='tiny1 field_tiny_label' and normalize-space(text())='Sort Order']]//input[@type='number']")));
            sortOrderInput.clear();
            sortOrderInput.sendKeys(sortOrder);
        } catch (Exception e) {
            System.out.println("Could not set sort order: " + e.getMessage());
        }
    }
    
    // Simplified data classes
    private static class RuleInfo {
        String name, group, loop, sortOrder, condition, quantity, value, markup;
    }
    
    private static class VariableInfo {
        String variable, value;
    }
}