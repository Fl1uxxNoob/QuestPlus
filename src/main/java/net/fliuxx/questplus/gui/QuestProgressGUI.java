package net.fliuxx.questplus.gui;

import net.fliuxx.questplus.QuestPlus;
import net.fliuxx.questplus.quest.Quest;
import net.fliuxx.questplus.quest.QuestProgress;
import net.fliuxx.questplus.utils.QuestUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI for viewing detailed quest progress
 */
public class QuestProgressGUI implements Listener {
    
    private final QuestPlus plugin;
    private final Map<Player, String> playerQuestViews;
    
    public QuestProgressGUI(QuestPlus plugin) {
        this.plugin = plugin;
        this.playerQuestViews = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void openProgressGUI(Player player, String questId) {
        QuestProgress progress = plugin.getQuestManager().getPlayerProgress(player)
                .stream()
                .filter(p -> p.getQuestId().equals(questId))
                .findFirst()
                .orElse(null);
        
        if (progress == null) {
            plugin.getMessages().sendMessage(player, "quest-not-found");
            return;
        }
        
        Quest quest = plugin.getQuestManager().getQuest(questId);
        if (quest == null) {
            plugin.getMessages().sendMessage(player, "quest-not-found");
            return;
        }
        
        playerQuestViews.put(player, questId);
        
        String title = plugin.getMessages().getRawMessage("gui.quest-progress.title")
                .replace("{quest}", quest.getName());
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text(title));
        
        setupProgressGUI(player, inventory, quest, progress);
        player.openInventory(inventory);
    }
    
    private void setupProgressGUI(Player player, Inventory inventory, Quest quest, QuestProgress progress) {
        // Quest information item
        ItemStack questItem = new ItemStack(quest.getDisplayItem());
        ItemMeta questMeta = questItem.getItemMeta();
        questMeta.displayName(Component.text(quest.getName())
                .color(progress.isCompleted() ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        
        List<Component> questLore = new ArrayList<>();
        questLore.add(Component.text(quest.getDescription()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        questLore.add(Component.empty());
        questLore.add(Component.text("Type: " + quest.getType().getDisplayName()).color(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false));
        questLore.add(Component.text("Target: " + quest.getTarget()).color(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false));
        
        if (quest.hasTimeLimit()) {
            questLore.add(Component.text("Time Limit: " + QuestUtils.formatTime(quest.getTimeLimit() / 1000))
                    .color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }
        
        questMeta.lore(questLore);
        questItem.setItemMeta(questMeta);
        inventory.setItem(4, questItem);
        
        // Progress bar visualization
        createProgressVisualization(inventory, progress);
        
        // Progress statistics
        ItemStack statsItem = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.displayName(Component.text("Progress Statistics").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        
        List<Component> statsLore = new ArrayList<>();
        statsLore.add(Component.text("Current Progress: " + progress.getProgress() + "/" + progress.getTarget())
                .color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        statsLore.add(Component.text("Percentage: " + String.format("%.1f", progress.getProgressPercentage()) + "%")
                .color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        
        long startTime = progress.getStartedAt();
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        statsLore.add(Component.text("Time Elapsed: " + QuestUtils.formatTime(elapsedTime / 1000))
                .color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        
        if (progress.getExpiresAt() != null) {
            long timeLeft = progress.getTimeRemaining();
            if (timeLeft > 0) {
                statsLore.add(Component.text("Time Remaining: " + QuestUtils.formatTime(timeLeft / 1000))
                        .color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            } else {
                statsLore.add(Component.text("STATUS: EXPIRED").color(NamedTextColor.DARK_RED)
                        .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            }
        }
        
        if (progress.isCompleted()) {
            if (progress.isClaimed()) {
                statsLore.add(Component.text("STATUS: REWARD CLAIMED").color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            } else {
                statsLore.add(Component.text("STATUS: READY TO CLAIM").color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            }
        } else {
            statsLore.add(Component.text("STATUS: IN PROGRESS").color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
        }
        
        statsMeta.lore(statsLore);
        statsItem.setItemMeta(statsMeta);
        inventory.setItem(12, statsItem);
        
        // Reward information
        if (!quest.getReward().isEmpty()) {
            ItemStack rewardItem = new ItemStack(Material.CHEST);
            ItemMeta rewardMeta = rewardItem.getItemMeta();
            rewardMeta.displayName(Component.text("Quest Reward").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            
            List<Component> rewardLore = new ArrayList<>();
            rewardLore.add(Component.text("Reward Commands:").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            for (String command : quest.getReward().getCommands()) {
                rewardLore.add(Component.text("• " + command.replace("<player>", player.getName()))
                        .color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            }
            
            if (quest.getReward().getMessage() != null) {
                rewardLore.add(Component.empty());
                rewardLore.add(Component.text("Reward Message:").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                rewardLore.add(Component.text(quest.getReward().getMessage().replace("<player>", player.getName()))
                        .color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            }
            
            if (progress.isCompleted() && !progress.isClaimed()) {
                rewardLore.add(Component.empty());
                rewardLore.add(Component.text("Click to claim reward!").color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            }
            
            rewardMeta.lore(rewardLore);
            rewardItem.setItemMeta(rewardMeta);
            inventory.setItem(14, rewardItem);
        }
        
        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(Component.text("Back to Quest Menu").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        backItem.setItemMeta(backMeta);
        inventory.setItem(18, backItem);
        
        // Close button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.displayName(Component.text("Close").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(26, closeItem);
    }
    
    private void createProgressVisualization(Inventory inventory, QuestProgress progress) {
        int totalSlots = 7; // Slots 10-16 for progress bar
        int filledSlots = (int) ((double) progress.getProgress() / progress.getTarget() * totalSlots);
        
        for (int i = 0; i < totalSlots; i++) {
            ItemStack barItem;
            ItemMeta barMeta;
            
            if (i < filledSlots) {
                // Filled progress
                barItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                barMeta = barItem.getItemMeta();
                barMeta.displayName(Component.text("Progress: " + (i + 1) + "/" + totalSlots)
                        .color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            } else {
                // Empty progress
                barItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                barMeta = barItem.getItemMeta();
                barMeta.displayName(Component.text("Not Completed")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            
            barItem.setItemMeta(barMeta);
            inventory.setItem(10 + i, barItem);
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String questId = playerQuestViews.get(player);
        if (questId == null) return;
        
        event.setCancelled(true);
        
        int slot = event.getSlot();
        
        if (slot == 14) { // Reward item
            QuestProgress progress = plugin.getQuestManager().getPlayerProgress(player)
                    .stream()
                    .filter(p -> p.getQuestId().equals(questId))
                    .findFirst()
                    .orElse(null);
            
            if (progress != null && progress.isCompleted() && !progress.isClaimed()) {
                if (plugin.getQuestManager().claimReward(player, questId)) {
                    // Refresh the GUI
                    openProgressGUI(player, questId);
                }
            }
        } else if (slot == 18) { // Back button
            player.closeInventory();
            new QuestGUI(plugin).openQuestGUI(player);
        } else if (slot == 26) { // Close button
            player.closeInventory();
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            playerQuestViews.remove(player);
        }
    }
}
