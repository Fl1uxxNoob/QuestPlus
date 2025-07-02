package net.fliuxx.questplus.config;

import net.fliuxx.questplus.QuestPlus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all plugin messages with MiniMessage support
 */
public class Messages {
    
    private final QuestPlus plugin;
    private FileConfiguration messagesConfig;
    private final MiniMessage miniMessage;
    private final Map<String, String> messageCache;
    
    public Messages(QuestPlus plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.messageCache = new HashMap<>();
        loadMessages();
    }
    
    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        cacheMessages();
    }
    
    private void cacheMessages() {
        messageCache.clear();
        for (String key : messagesConfig.getKeys(true)) {
            if (messagesConfig.isString(key)) {
                messageCache.put(key, messagesConfig.getString(key));
            }
        }
    }
    
    public void reload() {
        loadMessages();
    }
    
    public String getRawMessage(String key) {
        return messageCache.getOrDefault(key, "<red>Missing message: " + key);
    }
    
    public Component getMessage(String key) {
        return miniMessage.deserialize(getRawMessage(key));
    }
    
    public Component getMessage(String key, Map<String, String> placeholders) {
        String message = getRawMessage(key);
        
        // Replace placeholders
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return miniMessage.deserialize(message);
    }
    
    public void sendMessage(Player player, String key) {
        player.sendMessage(getMessage(key));
    }
    
    public void sendMessage(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(getMessage(key, placeholders));
    }
    
    // Common message methods
    public Component getPrefix() {
        return getMessage("prefix");
    }
    
    public Component getNoPermission() {
        return getMessage("no-permission");
    }
    
    public Component getPlayerOnly() {
        return getMessage("player-only");
    }
    
    public Component getQuestNotFound() {
        return getMessage("quest-not-found");
    }
    
    public Component getQuestAlreadyActive() {
        return getMessage("quest-already-active");
    }
    
    public Component getQuestLimitReached() {
        return getMessage("quest-limit-reached");
    }
    
    public Component getQuestAccepted(String questName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("quest", questName);
        return getMessage("quest-accepted", placeholders);
    }
    
    public Component getQuestCompleted(String questName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("quest", questName);
        return getMessage("quest-completed", placeholders);
    }
    
    public Component getQuestProgress(String questName, int current, int target) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("quest", questName);
        placeholders.put("current", String.valueOf(current));
        placeholders.put("target", String.valueOf(target));
        return getMessage("quest-progress", placeholders);
    }
    
    public Component getQuestTimeLeft(String timeLeft) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("time", timeLeft);
        return getMessage("quest-time-left", placeholders);
    }
    
    public Component getQuestExpired(String questName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("quest", questName);
        return getMessage("quest-expired", placeholders);
    }
    
    public Component getRewardClaimed(String questName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("quest", questName);
        return getMessage("reward-claimed", placeholders);
    }
}
