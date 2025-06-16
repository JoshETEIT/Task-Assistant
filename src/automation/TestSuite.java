package automation;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import automation.helpers.ProgressBarHelper;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.bonigarcia.wdm.WebDriverManager;

public class TestSuite {

    private static final String CSV_PATH = "servers.csv";

    private static JDialog progressDialog;
    private static JProgressBar progressBar;
    private static JLabel progressLabel;

    static class Server {
        String name, url, username, password;

        Server(String name, String url, String username, String password) {
            this.name = name;
            this.url = url;
            this.username = username;
            this.password = password;
        }

        String[] toRow() {
            return new String[]{name, url, username, "Select", "Edit", "Delete"};
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
        	String[] options = {
        		    "Add Lead", 
        		    "Save Drawing Settings", 
        		    "Inject Drawing Settings",
        		    "Upload Part Image"  // New option
        		};
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "What action would you like to perform?",
                    "Select Test Type",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice == JOptionPane.CLOSED_OPTION) System.exit(0);
            boolean runAddLead = (choice == 0);
            boolean runInjectSettings = (choice == 2);
            boolean runUploadImage = (choice == 3);
            showServerTable(loadServersFromCSV(), runAddLead, runInjectSettings, runUploadImage);
        });
    }

    private static void showServerTable(
    	    List<TestSuite.Server> servers, 
    	    boolean runAddLead, 
    	    boolean runInjectSettings,
    	    boolean runUploadImage  // New parameter
    	) {
        JFrame frame = new JFrame("Server Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        frame.setLocationRelativeTo(null);

        String[] columns = {"Name", "URL", "Username", "Select", "Edit", "Delete"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return column >= 3;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setFillsViewportHeight(true);
        table.setGridColor(new Color(220, 220, 220));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.setIntercellSpacing(new Dimension(10, 5));
        table.setSelectionBackground(new Color(220, 240, 255));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 3; i < columns.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        for (Server s : servers) {
            model.addRow(s.toRow());
        }

        model.addRow(new Object[]{"", "", "", "", "Add", ""});

        addButtonColumn(table, 3, "Select", row -> {
            if (row >= servers.size()) return;
            frame.dispose();

            new Thread(() -> runSeleniumTest(servers.get(row), runAddLead, runInjectSettings, runUploadImage)).start();
        }, servers.size());

        addButtonColumn(table, 4, "Edit", row -> {
            frame.dispose();

            Server existing = row < servers.size() ? servers.get(row) : new Server("", "", "", "");

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(new JLabel("Server Name:"), gbc);
            gbc.gridy++;
            panel.add(new JLabel("Server URL:"), gbc);
            gbc.gridy++;
            panel.add(new JLabel("Username:"), gbc);
            gbc.gridy++;
            panel.add(new JLabel("Password:"), gbc);

            gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
            JTextField nameField = new JTextField(existing.name, 20);
            panel.add(nameField, gbc);
            gbc.gridy++;
            JTextField urlField = new JTextField(existing.url, 20);
            panel.add(urlField, gbc);
            gbc.gridy++;
            JTextField userField = new JTextField(existing.username, 20);
            panel.add(userField, gbc);
            gbc.gridy++;
            JPasswordField passField = new JPasswordField(existing.password, 20);
            panel.add(passField, gbc);

            int result = JOptionPane.showConfirmDialog(null, panel,
                    (row < servers.size() ? "Edit Server" : "Add New Server"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String name = nameField.getText().trim();
                String url = urlField.getText().trim();
                String user = userField.getText().trim();
                String pass = new String(passField.getPassword()).trim();

                if (!name.isEmpty() && !url.isEmpty() && !user.isEmpty()) {
                    if (row >= servers.size()) {
                        servers.add(new Server(name, url, user, pass));
                    } else {
                        servers.set(row, new Server(name, url, user, pass));
                    }
                    saveServersToCSV(servers);
                }
            }

            showServerTable(loadServersFromCSV(), false, false, false);
        }, -1);

        addButtonColumn(table, 5, "Delete", row -> {
            if (row >= servers.size()) return;
            int confirm = JOptionPane.showConfirmDialog(null, "Delete this server?");
            if (confirm == JOptionPane.YES_OPTION) {
                servers.remove(row);
                saveServersToCSV(servers);
                frame.dispose();
                showServerTable(loadServersFromCSV(), false, false, false);
            }
        }, servers.size());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Server Management Panel", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        frame.setLayout(new BorderLayout());
        frame.add(titleLabel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private static void addButtonColumn(JTable table, int colIndex, String label,
                                        java.util.function.IntConsumer onClick, int disableRowIndex) {
        table.getColumnModel().getColumn(colIndex).setCellRenderer((tbl, val, sel, foc, row, col) -> {
            JButton btn = new JButton(label);
            if (disableRowIndex != -1 && row >= disableRowIndex && !label.equals("Edit")) {
                btn.setEnabled(false);
            }
            return btn;
        });

        table.getColumnModel().getColumn(colIndex).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            public Component getTableCellEditorComponent(JTable tbl, Object val, boolean sel, int row, int col) {
                JButton btn = new JButton(label);
                if (disableRowIndex != -1 && row >= disableRowIndex && !label.equals("Edit")) {
                    btn.setEnabled(false);
                } else {
                    btn.addActionListener(e -> onClick.accept(row));
                }
                return btn;
            }

            public Object getCellEditorValue() {
                return label;
            }
        });
    }

    private static List<Server> loadServersFromCSV() {
        List<Server> servers = new ArrayList<>();
        File file = new File(CSV_PATH);

        if (!file.exists()) {
            System.out.println("CSV file does not exist: " + CSV_PATH);
            return servers;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] f = parseCSVLine(line);
                if (f.length >= 4) {
                    servers.add(new Server(f[0], f[1], f[2], f[3]));
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading CSV: " + e.getMessage());
        }

        return servers;
    }

    private static void saveServersToCSV(List<Server> servers) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_PATH))) {
            pw.println("Name,URL,Username,Password");
            for (Server s : servers) {
                pw.printf("%s,%s,%s,%s%n",
                        escapeCSV(s.name),
                        escapeCSV(s.url),
                        escapeCSV(s.username),
                        escapeCSV(s.password));
            }
        } catch (IOException e) {
            System.out.println("Failed to save CSV: " + e.getMessage());
        }
    }

    private static String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(sb.toString().replace("\"\"", "\""));
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }

        values.add(sb.toString().replace("\"\"", "\"")); // Add last value
        return values.toArray(new String[0]);
    }

    private static JProgressBar stepProgressBar;

    private static void showProgressDialog(String serverName) {
        SwingUtilities.invokeLater(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dispose();
            }

            progressDialog = new JDialog((Frame) null, "Running Test", false);
            progressDialog.setSize(400, 180);  // Increased height for the second progress bar
            progressDialog.setLocationRelativeTo(null);

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel serverLabel = new JLabel("Testing Server: " + serverName);
            serverLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            panel.add(serverLabel, BorderLayout.NORTH);

            progressBar = new JProgressBar(0, 100);
            progressBar.setValue(0);
            progressBar.setStringPainted(true);
         // Replace the single panel with a vertical one
            JPanel progressBarsPanel = new JPanel();
            progressBarsPanel.setLayout(new BoxLayout(progressBarsPanel, BoxLayout.Y_AXIS));
            progressBarsPanel.add(progressBar);

            stepProgressBar = new JProgressBar(0, 100);
            stepProgressBar.setValue(0);
            stepProgressBar.setStringPainted(true);
            stepProgressBar.setString("0% - Step Progress");
            progressBarsPanel.add(stepProgressBar);
            
            ProgressBarHelper.initializeProgress(progressBar, stepProgressBar);

            panel.add(progressBarsPanel, BorderLayout.CENTER);  // Add both bars in the center

            progressLabel = new JLabel("Starting...");
            progressLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            panel.add(progressLabel, BorderLayout.SOUTH);


            progressDialog.setContentPane(panel);
            progressDialog.setAlwaysOnTop(true);
            progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            progressDialog.setResizable(false);
            progressDialog.setVisible(true);
        });
    }


    private static void updateProgress(String message, int targetPercent) {
        if (progressBar == null || progressLabel == null) return;

        SwingUtilities.invokeLater(() -> progressLabel.setText(message));

        new Thread(() -> {
            int current = progressBar.getValue();
            while (current < targetPercent) {
                current++;
                final int progress = current;
                SwingUtilities.invokeLater(() -> {
                    if (progressBar != null) progressBar.setValue(progress);
                });
                try {
                    Thread.sleep(30); // smooth animation
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private static void closeProgressDialog() {
        SwingUtilities.invokeLater(() -> {
            if (progressDialog != null) {
                progressDialog.dispose();
                progressDialog = null;
                progressBar = null;
                progressLabel = null;
            }
        });
    }



    private static void runSeleniumTest(
    	    Server s, 
    	    boolean runAddLead, 
    	    boolean runInjectSettings,
    	    boolean runUploadImage  // New parameter
    	) {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        showProgressDialog(s.name);
        ProgressBarHelper.initializeProgress(progressBar, stepProgressBar);
        updateProgress("Opening browser...", 10);

        try {
            driver.get(s.url);
            driver.manage().window().maximize();
            updateProgress("Navigating to login page...", 20);

            wait.until(ExpectedConditions.elementToBeClickable(By.id("login_user_name"))).sendKeys(s.username);
            wait.until(ExpectedConditions.elementToBeClickable(By.id("login_password"))).sendKeys(s.password);
            wait.until(ExpectedConditions.elementToBeClickable(By.id("submit_button"))).click();
            updateProgress("Logged in. Running test...", 70);

            if (runAddLead) {
                for (int i = 0; i < 5; i++) {
                    boolean success = AddNewLead.testFormSubmission(driver);
                    int progress = 75 + i * 5;
                    updateProgress("Form submission " + (success ? "succeeded" : "failed"), progress);
                    System.out.println((success ? "✅" : "❌") + " Test result for " + s.name);
                    driver.get(s.url + "/Home");
                }
            } else if (runInjectSettings) {
            	DrawingSettingsInjector.injectSettingsFromCSV(driver, wait, "drawing_settings.csv");
                updateProgress("Settings injected.", 100);
                System.out.println("✅ Injected drawing settings for " + s.name);
            } else if (runUploadImage) {
                final String[] folderPath = new String[1];
                SwingUtilities.invokeAndWait(() -> {
                    folderPath[0] = JOptionPane.showInputDialog(
                        progressDialog, 
                        "Enter Images Folder Path:", 
                        "Image Folder Input", 
                        JOptionPane.QUESTION_MESSAGE);
                });
                
                if (folderPath[0] != null) {
                    updateProgress("Starting image upload...", 70);
                    new UploadPartImage(driver).uploadImagesFromFolder(folderPath[0]);
                    updateProgress("Upload completed", 100);
                } else {
                    updateProgress("Upload canceled", 100);
                }
            } else {
                DrawingSettingsCSV.saveSettings(driver, wait);
                updateProgress("Settings saved.", 100);
                System.out.println("✅ Saved drawing settings for " + s.name);
            }

        } catch (Exception e) {
            System.out.println("Login or test failed for " + s.name + ": " + e.getMessage());
        } finally {
            closeProgressDialog();
            // driver.quit();
        }
    }

}