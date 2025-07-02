package net.fliuxx.questplus.listeners;

import net.fliuxx.questplus.QuestPlus;
import net.fliuxx.questplus.quest.Quest;
import net.fliuxx.questplus.quest.QuestProgress;
import net.fliuxx.questplus.quest.types.AbstractQuestType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

import java.util.List;

/**
 * Listens for events that can progress quests
 */
public class QuestListener implements Listener {
    
    private final QuestPlus plugin;
    
    public QuestListener(QuestPlus plugin) {
        this.plugin = plugin;
    }
    
    // Item Collection Events
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        processQuestEvent(player, event);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        processQuestEvent(player, event);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        processQuestEvent(event.getPlayer(), event);
    }
    
    // Combat Events
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player) {
            Player player = (Player) event.getEntity().getKiller();
            processQuestEvent(player, event);
        }
    }
    
    // Block Events
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        processQuestEvent(event.getPlayer(), event);
    }
    
    // Player Movement Events
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        processQuestEvent(event.getPlayer(), event);
    }
    
    // Fishing Events
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        processQuestEvent(event.getPlayer(), event);
    }
    
    // Villager Interaction Events
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        processQuestEvent(event.getPlayer(), event);
    }
    
    // Trading Events
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        processQuestEvent(player, event);
    }
    
    // Flight Events (for flight quests)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        processQuestEvent(event.getPlayer(), event);
    }
    
    // Food Level Change (for survival quests)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFoodLevelChange(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            processQuestEvent(player, event);
        }
    }
    
    // Damage Events (for survival quests)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            processQuestEvent(player, event);
        }
    }
    
    /**
     * Process quest events for a player
     */
    private void processQuestEvent(Player player, org.bukkit.event.Event event) {
        if (player == null) return;
        
        List<QuestProgress> activeQuests = plugin.getQuestManager().getPlayerProgress(player)
                .stream()
                .filter(progress -> !progress.isCompleted() && !progress.isExpired())
                .toList();
        
        for (QuestProgress progress : activeQuests) {
            Quest quest = plugin.getQuestManager().getQuest(progress.getQuestId());
            if (quest == null) continue;
            
            try {
                AbstractQuestType questType = quest.createQuestTypeHandler();
                int progressAmount = questType.checkProgress(player, event);
                
                if (progressAmount > 0) {
                    plugin.getQuestManager().updateQuestProgress(player, progress.getQuestId(), progressAmount);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error processing quest event for quest " + quest.getId() + ": " + e.getMessage());
            }
        }
    }
}
