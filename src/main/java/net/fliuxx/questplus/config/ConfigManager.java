package net.fliuxx.questplus.config;

import net.fliuxx.questplus.QuestPlus;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Manages configuration files for QuestPlus
 */
public class ConfigManager {
    
    private final QuestPlus plugin;
    private FileConfiguration config;
    private FileConfiguration questsConfig;
    
    public ConfigManager(QuestPlus plugin) {
        this.plugin = plugin;
        loadConfigs();
    }
    
    private void loadConfigs() {
        // Save default configs if they don't exist
        plugin.saveDefaultConfig();
        saveDefaultConfig("quests.yml");
        
        // Load configs
        config = plugin.getConfig();
        questsConfig = loadConfig("quests.yml");
    }
    
    private void saveDefaultConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }
    
    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        return YamlConfiguration.loadConfiguration(file);
    }
    
    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        questsConfig = loadConfig("quests.yml");
    }
    
    // Database settings
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }
    
    public String getDatabaseHost() {
        return config.getString("database.mysql.host", "localhost");
    }
    
    public int getDatabasePort() {
        return config.getInt("database.mysql.port", 3306);
    }
    
    public String getDatabaseName() {
        return config.getString("database.mysql.database", "questplus");
    }
    
    public String getDatabaseUsername() {
        return config.getString("database.mysql.username", "root");
    }
    
    public String getDatabasePassword() {
        return config.getString("database.mysql.password", "");
    }
    
    public int getConnectionPoolSize() {
        return config.getInt("database.pool-size", 10);
    }
    
    // Quest settings
    public int getDefaultQuestLimit() {
        return config.getInt("quest-limits.default", 3);
    }
    
    public int getQuestLimitForGroup(String group) {
        return config.getInt("quest-limits.groups." + group, getDefaultQuestLimit());
    }
    
    public boolean isQuestLimitEnabled() {
        return config.getBoolean("quest-limits.enabled", true);
    }
    
    // GUI settings
    public String getGuiTitle() {
        return config.getString("gui.title", "QuestPlus");
    }
    
    public int getGuiSize() {
        return config.getInt("gui.size", 54);
    }
    
    // Auto-save settings
    public boolean isAutoSaveEnabled() {
        return config.getBoolean("auto-save.enabled", true);
    }
    
    public int getAutoSaveInterval() {
        return config.getInt("auto-save.interval", 300); // 5 minutes default
    }
    
    // Quest configuration access
    public FileConfiguration getQuestsConfig() {
        return questsConfig;
    }
    
    public FileConfiguration getMainConfig() {
        return config;
    }
    
    // Utility methods
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }
    
    public void saveConfig() {
        try {
            plugin.saveConfig();
            
            File questsFile = new File(plugin.getDataFolder(), "quests.yml");
            questsConfig.save(questsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save configs: " + e.getMessage());
        }
    }
}
