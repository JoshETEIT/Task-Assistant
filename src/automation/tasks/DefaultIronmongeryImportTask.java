package automation.tasks;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import automation.TestSuite;
import automation.helpers.CsvReader;
import automation.helpers.ElementHelper.LocatorType;
import automation.helpers.ElementHelper.Screenshot;
import automation.ui.ProgressUI;

import java.time.Duration;
import java.util.*;

import static automation.helpers.ElementHelper.*;

public class DefaultIronmongeryImportTask extends TaskBase {

    @Override
    public String getName() {
        return "Default Ironmongery Import";
    }

    @Override
    public void execute(WebDriver driver, String baseUrl, ProgressUI progressUI) {
        try {
            progressUI.updateStatus("Starting default ironmongery import");
            progressUI.setMainProgressMax(1);
            progressUI.setStepProgressMax(100);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // --- Load CSVs ---
            String csvPath = getCsvFile(progressUI, "Default Ironmongery Rules");
            if (csvPath == null) return;

            String groupsCsvPath = getCsvFile(progressUI, "Default Ironmongery Groups", false);
            String variablesCsvPath = getCsvFile(progressUI, "Default Ironmongery Variables", false);

            progressUI.updateStepProgress(10, "Reading rules CSV...");
            List<IronmongeryRuleInfo> rules = CsvReader.read(csvPath, this::createRule);

            List<GroupInfo> groups = groupsCsvPath != null && !groupsCsvPath.trim().isEmpty()
                    ? CsvReader.read(groupsCsvPath, this::createGroup)
                    : new ArrayList<>();

            List<VariableInfo> variables = variablesCsvPath != null && !variablesCsvPath.trim().isEmpty()
                    ? CsvReader.read(variablesCsvPath, this::createVariable)
                    : new ArrayList<>();

            // --- Navigate ---
            progressUI.updateStepProgress(30, "Navigating to Default Ironmongery");
            navigateToDefaultIronmongery(driver, baseUrl, wait);

            // --- Groups ---
            progressUI.updateStepProgress(40, "Processing groups");
            if (!groups.isEmpty()) checkAndCreateGroups(driver, wait, groups, progressUI);

            // --- Variables ---
            progressUI.updateStepProgress(50, "Processing variables");
            if (!variables.isEmpty()) checkAndCreateVariables(driver, wait, variables, progressUI);

            // --- Rules ---
            progressUI.updateStepProgress(60, "Importing rules");
            progressUI.setMainProgressMax(rules.size());
            importRules(driver, wait, rules, progressUI);

            completeAndHide(progressUI, "Successfully imported " + rules.size() + " default ironmongery rules");

        } catch (Exception e) {
            handleError(progressUI, e);
        }
    }

    // --- CSV Object Creators ---
    private IronmongeryRuleInfo createRule(String[] f) {
        IronmongeryRuleInfo r = new IronmongeryRuleInfo();
        if (f.length >= 8) {
            r.sortOrder = f[0];
            r.name = f[1];
            r.group = f[2];
            r.loop = f[3];
            r.active = f[4];
            r.condition = f[5];
            r.quantity = f[6];
            r.ironmongery = f[7];
            r.comment = f.length > 8 ? f[8] : "";
        }
        return r;
    }

    private GroupInfo createGroup(String[] f) {
        GroupInfo g = new GroupInfo();
        if (f.length >= 2) {
            g.name = f[0];
            g.sortOrder = f[1];
        }
        return g;
    }

    private VariableInfo createVariable(String[] f) {
        VariableInfo v = new VariableInfo();
        if (f.length >= 2) {
            v.name = f[0];
            v.value = f[1];
        }
        return v;
    }

    // --- Navigation ---
    private void navigateToDefaultIronmongery(WebDriver driver, String baseUrl, WebDriverWait wait) {
        if (!driver.getCurrentUrl().contains("/Home")) driver.get(baseUrl + "/Home");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href,'PricingAndConfig')]")));

        clickButton(driver, LocatorType.XPATH,
                "//a[contains(@href,'DrawingBoardConfig')]", Screenshot.ON, 3);
        wait.until(ExpectedConditions.urlContains("DrawingBoardConfig"));

        clickButton(driver, LocatorType.XPATH,
                "//a[contains(@class,'tab_inactive') and contains(@href,'DefaultIronmongery')]",
                Screenshot.ON, 3);
        wait.until(ExpectedConditions.urlContains("DefaultIronmongery"));
    }

    // --- Groups & Variables ---
    private void checkAndCreateGroups(WebDriver driver, WebDriverWait wait, List<GroupInfo> groups, ProgressUI progressUI) {
        try {
            openExpandingPanel(driver, "Groups");
            progressUI.updateStatus("Checking existing groups");

            Set<String> existing = new HashSet<>();
            findExistingGroups(driver, existing);

            List<GroupInfo> newGroups = filterExistingGroups(groups, existing);
            if (!newGroups.isEmpty()) {
                progressUI.updateStatus("Creating " + newGroups.size() + " new groups");
                createGroupsInOpenPanel(driver, newGroups, progressUI);
            } else progressUI.updateStatus("All groups already exist - skipping group creation");

            closeExpandingPanel(driver, "Groups");
        } catch (Exception e) {
            System.out.println("Error processing groups: " + e.getMessage());
        }
    }

    private void checkAndCreateVariables(WebDriver driver, WebDriverWait wait, List<VariableInfo> vars, ProgressUI progressUI) {
        try {
            openExpandingPanel(driver, "Variables");
            progressUI.updateStatus("Checking existing variables");

            Set<String> existing = new HashSet<>();
            findExistingVariables(driver, existing);

            List<VariableInfo> newVars = filterExistingVariables(vars, existing);
            if (!newVars.isEmpty()) {
                progressUI.updateStatus("Creating " + newVars.size() + " new variables");
                createVariablesInOpenPanel(driver, newVars, progressUI);
            } else progressUI.updateStatus("All variables already exist - skipping variable creation");

            closeExpandingPanel(driver, "Variables");
        } catch (Exception e) {
            System.out.println("Error processing variables: " + e.getMessage());
        }
    }

    // --- Expanding Panels ---
    private void openExpandingPanel(WebDriver driver, String panel) throws InterruptedException {
        String title = String.format("//div[contains(@class,'expanding-panel')]//div[contains(@class,'ep-title') and contains(., '%s')]", panel);
        clickButton(driver, LocatorType.XPATH, title, Screenshot.OFF, 2);
        Thread.sleep(1000);
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format(
                        "//div[contains(@class,'expanding-panel')][.//span[contains(.,'%s')]]//div[contains(@class,'ep-close')]", panel))));
    }

    private void closeExpandingPanel(WebDriver driver, String panel) throws InterruptedException {
        String close = String.format(
                "//div[contains(@class,'expanding-panel')][.//span[contains(.,'%s')]]//div[contains(@class,'ep-close')]/img",
                panel);
        clickButton(driver, LocatorType.XPATH, close, Screenshot.OFF, 2);
        Thread.sleep(500);
    }

    // --- Group & Variable Creation ---
    private void findExistingGroups(WebDriver driver, Set<String> existing) {
        try {
            List<WebElement> elems = driver.findElements(By.xpath(
                    "//div[contains(@class,'expanding-panel')][.//span[contains(.,'Groups')]]//tr/td[1]/span"));
            for (WebElement e : elems) {
                String text = e.getText().trim();
                if (!text.isEmpty() && !text.matches("\\d+") && !text.equals("Groups") && !text.equals("Quote Level"))
                    existing.add(text);
            }
        } catch (Exception e) {
            System.out.println("Error finding existing groups: " + e.getMessage());
        }
    }

    private void findExistingVariables(WebDriver driver, Set<String> existing) {
        try {
            List<WebElement> spans = driver.findElements(By.xpath(
                    "//div[contains(@class,'expanding-panel')][.//span[contains(.,'Variables')]]//td[1]/span"));
            for (WebElement s : spans) {
                String text = s.getText().trim();
                if (!text.isEmpty() && !text.equals("Variables")) existing.add(text);
            }
        } catch (Exception e) {
            System.out.println("Error finding existing variables: " + e.getMessage());
        }
    }

    private void createGroupsInOpenPanel(WebDriver driver, List<GroupInfo> groups, ProgressUI ui) {
        for (GroupInfo g : groups) {
            checkCancellation();
            createSingleGroup(driver, g);
            ui.updateStatus("Created group: " + g.name);
        }
    }

    private void createVariablesInOpenPanel(WebDriver driver, List<VariableInfo> vars, ProgressUI ui) {
        for (VariableInfo v : vars) {
            checkCancellation();
            createSingleVariable(driver, v);
            ui.updateStatus("Created variable: " + v.name);
        }
    }

    private void createSingleGroup(WebDriver driver, GroupInfo g) {
        try {
            WebElement name = driver.findElement(By.xpath(
                    "//div[contains(@class,'expanding-panel')][.//span[contains(.,'Groups')]]//input[@placeholder='name of new group']"));
            WebElement sort = driver.findElement(By.xpath(
                    "//div[contains(@class,'expanding-panel')][.//span[contains(.,'Groups')]]//input[@type='number']"));

            name.clear();
            name.sendKeys(g.name);
            sort.clear();
            sort.sendKeys(g.sortOrder);

            clickButton(driver, LocatorType.XPATH,
                    "//div[contains(@class,'expanding-panel')][.//span[contains(.,'Groups')]]//button[contains(text(),'Add new')]",
                    Screenshot.OFF, 2);
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Error creating group '" + g.name + "': " + e.getMessage());
        }
    }

    private void createSingleVariable(WebDriver driver, VariableInfo v) {
        try {
            WebElement name = driver.findElement(By.xpath(
                    "//div[contains(@class,'expanding-panel')][.//span[contains(.,'Variables')]]//input[@placeholder='name of new variable']"));
            WebElement value = driver.findElement(By.xpath(
                    "//div[contains(@class,'expanding-panel')][.//span[contains(.,'Variables')]]//input[@title='value can be either numeric or string or coma separated list' and not(contains(@style,'display: none'))]"));

            name.clear();
            name.sendKeys(v.name);
            value.clear();
            value.sendKeys(v.value);

            clickButton(driver, LocatorType.XPATH,
                    "//div[contains(@class,'expanding-panel')][.//span[contains(.,'Variables')]]//button[contains(text(),'Add new')]",
                    Screenshot.OFF, 2);
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Error creating variable '" + v.name + "': " + e.getMessage());
        }
    }

    // --- Rules ---
    private void importRules(WebDriver driver, WebDriverWait wait, List<IronmongeryRuleInfo> rules, ProgressUI ui) {
        int imported = 0;
        for (int i = 0; i < rules.size() && !TestSuite.isTaskCancelled(); i++) {
            IronmongeryRuleInfo rule = rules.get(i);
            ui.updateMainProgress(i);
            ui.updateStepProgress(60 + (i * 40 / rules.size()),
                    "Importing rule " + (i + 1) + "/" + rules.size() + " (" + imported + " imported)");

            try {
                checkCancellation();
                createSingleRule(driver, wait, rule);
                imported++;
                ui.updateStatus("Imported rule: " + rule.name);
            } catch (Exception e) {
                System.out.println("Error importing rule '" + rule.name + "': " + e.getMessage());
                closeOpenDialog(driver);
            }
        }
        ui.updateStatus("Import complete: " + imported + " rules added");
    }

    private void createSingleRule(WebDriver driver, WebDriverWait wait, IronmongeryRuleInfo rule) throws InterruptedException {
        clickButton(driver, LocatorType.XPATH,
                "//div[contains(@class,'highlighted-button')]//button[span[contains(text(), 'Create new rule')]]",
                Screenshot.ON, 3);
        Thread.sleep(500);

        fillRuleForm(driver, wait, rule);
        checkCancellation();

        clickButton(driver, LocatorType.XPATH,
                "//button[span[contains(text(), 'Create New Rule')]]",
                Screenshot.ON, 3);
        Thread.sleep(2000);
    }


    private void fillRuleForm(WebDriver driver, WebDriverWait wait, IronmongeryRuleInfo r) {
        enterCodeMirrorByLabel(driver, "Name", r.name);
        selectDropdownByLabel(driver, "Group", r.group);

        String loop = "_item_level_".equals(r.loop) ? "--- n/a ---" : r.loop;
        selectDropdownByLabel(driver, "Loop", loop);

        String activeBtn = "yes".equalsIgnoreCase(r.active)
                ? "//div[.//span[text()='Active']]//button[span[contains(text(),'Yes')]]"
                : "//div[.//span[text()='Active']]//button[span[contains(text(),'No')]]";
        clickButton(driver, LocatorType.XPATH, activeBtn, Screenshot.OFF, 2);

        fillSortOrder(driver, wait, r.sortOrder);
        enterCodeMirrorByLabel(driver, "Condition", r.condition);
        enterCodeMirrorByLabel(driver, "Quantity", r.quantity);
        selectIronmongery(driver, r.ironmongery);
        fillComment(driver, r.comment);
    }

    private void fillSortOrder(WebDriver driver, WebDriverWait wait, String val) {
        try {
            WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
                    "//div[.//div[@class='tiny1 field_tiny_label' and normalize-space(text())='Sort Order']]//input[@type='number']")));
            input.clear();
            input.sendKeys(val);
        } catch (Exception e) {
            System.out.println("Could not set sort order: " + e.getMessage());
        }
    }

    private void selectIronmongery(WebDriver driver, String val) {
        try {
            WebElement input = driver.findElement(By.cssSelector("span.custom-combobox input.custom-combobox-input"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", input);
            input.click();
            input.sendKeys(val);
            Thread.sleep(300);
            input.sendKeys(Keys.ENTER);
        } catch (Exception e) {
            System.out.println("Could not select ironmongery: " + e.getMessage());
        }
    }

    private void fillComment(WebDriver driver, String text) {
        try {
            WebElement input = driver.findElement(By.xpath("//span[normalize-space(text())='Comment']/following::input[1]"));
            input.clear();
            input.sendKeys(text);
        } catch (Exception e) {
            System.out.println("Could not set comment: " + e.getMessage());
        }
    }

    private void closeOpenDialog(WebDriver driver) {
        clickButton(driver, LocatorType.CSS, ".ui-dialog-titlebar-close", Screenshot.OFF, 1);
    }

    private List<GroupInfo> filterExistingGroups(List<GroupInfo> all, Set<String> existing) {
        List<GroupInfo> newOnes = new ArrayList<>();
        for (GroupInfo g : all) if (!existing.contains(g.name.trim())) newOnes.add(g);
        return newOnes;
    }

    private List<VariableInfo> filterExistingVariables(List<VariableInfo> all, Set<String> existing) {
        List<VariableInfo> newOnes = new ArrayList<>();
        for (VariableInfo v : all) if (!existing.contains(v.name.trim())) newOnes.add(v);
        return newOnes;
    }

    // --- Data Classes ---
    private static class IronmongeryRuleInfo {
        String name, group, loop, active, sortOrder, condition, quantity, ironmongery, comment;
    }

    private static class GroupInfo {
        String name, sortOrder;
    }

    private static class VariableInfo {
        String name, value;
    }
}
