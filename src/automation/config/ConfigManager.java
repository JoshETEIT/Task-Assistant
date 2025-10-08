// ConfigManager.java
package automation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
	private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.json";
    private static ConfigManager instance;
    private Config config;
    private final ObjectMapper mapper;
    
    
    
    private ConfigManager() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ensureConfigDirectory();
        loadConfig();
    }
    
    private void ensureConfigDirectory() {
        try {
            Path configPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                System.out.println("📁 Created config directory: " + configPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("❌ Could not create config directory: " + e.getMessage());
        }
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    public Config getConfig() {
        return config;
    }
    
    private void loadConfig() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                config = mapper.readValue(configFile, Config.class);
                System.out.println("✅ Configuration loaded from: " + configFile.getAbsolutePath());
            } else {
                createDefaultConfig();
            }
        } catch (IOException e) {
            System.err.println("❌ Error loading configuration, using defaults: " + e.getMessage());
            createDefaultConfig();
        }
    }
    
    private void createDefaultConfig() {
        config = new Config();
        
        // Set up default UI config
        Config.UiConfig uiConfig = new Config.UiConfig();
        Config.FontConfig fontConfig = new Config.FontConfig();
        uiConfig.setFont(fontConfig);
        config.setUi(uiConfig);
        
        // Set up default automation config
        Config.AutomationConfig automationConfig = new Config.AutomationConfig();
        config.setAutomation(automationConfig);
        
        // Set up default selenium config
        Config.SeleniumConfig seleniumConfig = new Config.SeleniumConfig();
        Config.WindowConfig windowConfig = new Config.WindowConfig();
        seleniumConfig.setWindow(windowConfig);
        config.setSelenium(seleniumConfig);
        
        saveConfig();
    }
    
    public void saveConfig() {
        try {
            mapper.writeValue(new File(CONFIG_FILE), config);
            System.out.println("✅ Configuration saved to: " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("❌ Error saving configuration: " + e.getMessage());
        }
    }
    
    public void reloadConfig() {
        loadConfig();
    }
    
    // Helper methods for quick access to common settings
    public boolean isVisualDebugEnabled() {
        return config.getAutomation().isVisualDebug();
    }
    
    public int getDefaultRetryAttempts() {
        return config.getAutomation().getDefaultRetryAttempts();
    }
    
    public int getTimeoutSeconds() {
        return config.getAutomation().getTimeoutSeconds();
    }
    
    public String getScreenshotDirectory() {
        return config.getAutomation().getScreenshotDirectory();
    }
}