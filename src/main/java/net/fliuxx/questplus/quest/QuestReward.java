package net.fliuxx.questplus.quest;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents rewards that can be given to players for completing quests
 */
public class QuestReward {
    
    private final List<String> commands;
    private final String message;
    
    public QuestReward(List<String> commands, String message) {
        this.commands = commands != null ? commands : new ArrayList<>();
        this.message = message;
    }
    
    public static QuestReward fromConfig(ConfigurationSection config) {
        if (config == null) {
            return new QuestReward(new ArrayList<>(), null);
        }
        
        List<String> commands = config.getStringList("commands");
        String message = config.getString("message");
        
        return new QuestReward(commands, message);
    }
    
    public void give(Player player) {
        // Execute reward commands
        for (String command : commands) {
            String processedCommand = command.replace("<player>", player.getName())
                                           .replace("{player}", player.getName())
                                           .replace("%player%", player.getName());
            
            // Execute command from console
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        }
        
        // Send reward message if configured
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message.replace("<player>", player.getName())
                                    .replace("{player}", player.getName())
                                    .replace("%player%", player.getName()));
        }
    }
    
    public List<String> getCommands() {
        return commands;
    }
    
    public String getMessage() {
        return message;
    }
    
    public boolean isEmpty() {
        return commands.isEmpty() && (message == null || message.isEmpty());
    }
}
