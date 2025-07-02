package net.fliuxx.questplus.listeners;

import net.fliuxx.questplus.QuestPlus;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player connection events for quest data management
 */
public class PlayerListener implements Listener {
    
    private final QuestPlus plugin;
    
    public PlayerListener(QuestPlus plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player quest progress asynchronously to avoid blocking the main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getQuestManager().loadPlayerProgress(event.getPlayer());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load quest progress for " + event.getPlayer().getName() + ": " + e.getMessage());
            }
        });
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save and unload player quest progress asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getQuestManager().unloadPlayerProgress(event.getPlayer());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save quest progress for " + event.getPlayer().getName() + ": " + e.getMessage());
            }
        });
    }
}
