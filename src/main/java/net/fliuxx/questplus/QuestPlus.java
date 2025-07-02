package net.fliuxx.questplus;

import net.fliuxx.questplus.commands.QuestAdminCommand;
import net.fliuxx.questplus.commands.QuestPlusCommand;
import net.fliuxx.questplus.config.ConfigManager;
import net.fliuxx.questplus.config.Messages;
import net.fliuxx.questplus.database.DatabaseManager;
import net.fliuxx.questplus.integration.LuckPermsIntegration;
import net.fliuxx.questplus.integration.PlaceholderAPIExpansion;
import net.fliuxx.questplus.listeners.PlayerListener;
import net.fliuxx.questplus.listeners.QuestListener;
import net.fliuxx.questplus.quest.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main class for QuestPlus plugin
 * 
 * @author Fl1uxxNoob
 * @version 1.0.0
 * @since 1.0.0
 */
public class QuestPlus extends JavaPlugin {
    
    private static QuestPlus instance;
    private ConfigManager configManager;
    private Messages messages;
    private DatabaseManager databaseManager;
    private QuestManager questManager;
    private LuckPermsIntegration luckPermsIntegration;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize components
        getLogger().info("Starting QuestPlus v" + getDescription().getVersion());
        
        try {
            // Load configuration
            configManager = new ConfigManager(this);
            messages = new Messages(this);
            
            // Initialize database
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            
            // Initialize quest manager
            questManager = new QuestManager(this);
            questManager.loadQuests();
            
            // Initialize integrations
            initializeIntegrations();
            
            // Register commands
            registerCommands();
            
            // Register listeners
            registerListeners();
            
            getLogger().info("QuestPlus has been enabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable QuestPlus", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Disabling QuestPlus...");
        
        // Save all data
        if (questManager != null) {
            questManager.saveAllProgress();
        }
        
        // Close database connections
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("QuestPlus has been disabled!");
    }
    
    private void initializeIntegrations() {
        // LuckPerms integration
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsIntegration = new LuckPermsIntegration();
            if (luckPermsIntegration.initialize()) {
                getLogger().info("LuckPerms integration enabled!");
            }
        }
        
        // PlaceholderAPI integration
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIExpansion(this).register();
            getLogger().info("PlaceholderAPI integration enabled!");
        }
    }
    
    private void registerCommands() {
        getCommand("questplus").setExecutor(new QuestPlusCommand(this));
        getCommand("questadmin").setExecutor(new QuestAdminCommand(this));
    }
    
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new QuestListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
    }
    
    public void reload() {
        try {
            configManager.reload();
            messages.reload();
            questManager.loadQuests();
            getLogger().info("QuestPlus has been reloaded successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to reload QuestPlus", e);
        }
    }
    
    // Getters
    public static QuestPlus getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public Messages getMessages() {
        return messages;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public QuestManager getQuestManager() {
        return questManager;
    }
    
    public LuckPermsIntegration getLuckPermsIntegration() {
        return luckPermsIntegration;
    }
}
