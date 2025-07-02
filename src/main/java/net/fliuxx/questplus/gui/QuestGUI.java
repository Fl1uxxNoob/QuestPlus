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
 * Main quest GUI for viewing and managing quests
 */
public class QuestGUI implements Listener {
    
    private final QuestPlus plugin;
    private final Map<Player, QuestGUIState> playerStates;
    
    public QuestGUI(QuestPlus plugin) {
        this.plugin = plugin;
        this.playerStates = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void openQuestGUI(Player player) {
        QuestGUIState state = new QuestGUIState();
        playerStates.put(player, state);
        
        updateGUI(player, state);
    }
    
    private void updateGUI(Player player, QuestGUIState state) {
        String title = plugin.getMessages().getRawMessage("gui.quest-menu.title");
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text(title));
        
        // Navigation items
        setupNavigationItems(inventory, state);
        
        // Content based on current page
        switch (state.getCurrentPage()) {
            case AVAILABLE -> showAvailableQuests(player, inventory, state);
            case ACTIVE -> showActiveQuests(player, inventory, state);
            case COMPLETED -> showCompletedQuests(player, inventory, state);
            case STATISTICS -> showStatistics(player, inventory, state);
        }
        
        player.openInventory(inventory);
    }
    
    private void setupNavigationItems(Inventory inventory, QuestGUIState state) {
        // Available Quests
        ItemStack availableItem = new ItemStack(Material.COMPASS);
        ItemMeta availableMeta = availableItem.getItemMeta();
        availableMeta.displayName(Component.text("Available Quests")
                .color(state.getCurrentPage() == QuestGUIState.PageType.AVAILABLE ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        availableMeta.lore(List.of(
                Component.text("Click to view available quests").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        availableItem.setItemMeta(availableMeta);
        inventory.setItem(45, availableItem);
        
        // Active Quests
        ItemStack activeItem = new ItemStack(Material.CLOCK);
        ItemMeta activeMeta = activeItem.getItemMeta();
        activeMeta.displayName(Component.text("Active Quests")
                .color(state.getCurrentPage() == QuestGUIState.PageType.ACTIVE ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        activeMeta.lore(List.of(
                Component.text("Click to view active quests").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        activeItem.setItemMeta(activeMeta);
        inventory.setItem(46, activeItem);
        
        // Completed Quests
        ItemStack completedItem = new ItemStack(Material.EMERALD);
        ItemMeta completedMeta = completedItem.getItemMeta();
        completedMeta.displayName(Component.text("Completed Quests")
                .color(state.getCurrentPage() == QuestGUIState.PageType.COMPLETED ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        completedMeta.lore(List.of(
                Component.text("Click to view completed quests").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        completedItem.setItemMeta(completedMeta);
        inventory.setItem(47, completedItem);
        
        // Statistics
        ItemStack statsItem = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.displayName(Component.text("Statistics")
                .color(state.getCurrentPage() == QuestGUIState.PageType.STATISTICS ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        statsMeta.lore(List.of(
                Component.text("Click to view your quest statistics").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        statsItem.setItemMeta(statsMeta);
        inventory.setItem(48, statsItem);
        
        // Close button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.displayName(Component.text("Close").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(53, closeItem);
        
        // Page navigation
        if (state.getPage() > 0) {
            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevItem.getItemMeta();
            prevMeta.displayName(Component.text("Previous Page").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            prevItem.setItemMeta(prevMeta);
            inventory.setItem(51, prevItem);
        }
        
        if (state.hasNextPage()) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextItem.getItemMeta();
            nextMeta.displayName(Component.text("Next Page").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            nextItem.setItemMeta(nextMeta);
            inventory.setItem(52, nextItem);
        }
    }
    
    private void showAvailableQuests(Player player, Inventory inventory, QuestGUIState state) {
        List<Quest> availableQuests = plugin.getQuestManager().getAvailableQuests(player);
        int startIndex = state.getPage() * 36;
        int endIndex = Math.min(startIndex + 36, availableQuests.size());
        
        state.setHasNextPage(endIndex < availableQuests.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Quest quest = availableQuests.get(i);
            ItemStack item = createQuestItem(quest, player, QuestItemType.AVAILABLE);
            inventory.setItem(i - startIndex, item);
        }
    }
    
    private void showActiveQuests(Player player, Inventory inventory, QuestGUIState state) {
        List<QuestProgress> activeQuests = plugin.getQuestManager().getPlayerProgress(player)
                .stream()
                .filter(progress -> !progress.isCompleted())
                .toList();
        
        int startIndex = state.getPage() * 36;
        int endIndex = Math.min(startIndex + 36, activeQuests.size());
        
        state.setHasNextPage(endIndex < activeQuests.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            QuestProgress progress = activeQuests.get(i);
            Quest quest = plugin.getQuestManager().getQuest(progress.getQuestId());
            if (quest != null) {
                ItemStack item = createQuestProgressItem(quest, progress, QuestItemType.ACTIVE);
                inventory.setItem(i - startIndex, item);
            }
        }
    }
    
    private void showCompletedQuests(Player player, Inventory inventory, QuestGUIState state) {
        List<QuestProgress> completedQuests = plugin.getQuestManager().getPlayerProgress(player)
                .stream()
                .filter(QuestProgress::isCompleted)
                .toList();
        
        int startIndex = state.getPage() * 36;
        int endIndex = Math.min(startIndex + 36, completedQuests.size());
        
        state.setHasNextPage(endIndex < completedQuests.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            QuestProgress progress = completedQuests.get(i);
            Quest quest = plugin.getQuestManager().getQuest(progress.getQuestId());
            if (quest != null) {
                ItemStack item = createQuestProgressItem(quest, progress, 
                        progress.isClaimed() ? QuestItemType.CLAIMED : QuestItemType.COMPLETED);
                inventory.setItem(i - startIndex, item);
            }
        }
    }
    
    private void showStatistics(Player player, Inventory inventory, QuestGUIState state) {
        int activeCount = plugin.getQuestManager().getActiveQuestCount(player);
        int completedCount = plugin.getQuestManager().getCompletedQuestCount(player);
        int availableCount = plugin.getQuestManager().getAvailableQuests(player).size();
        
        // Active quests stat
        ItemStack activeStatItem = new ItemStack(Material.CLOCK);
        ItemMeta activeStatMeta = activeStatItem.getItemMeta();
        activeStatMeta.displayName(Component.text("Active Quests").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        activeStatMeta.lore(List.of(
                Component.text("Currently active: " + activeCount).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        activeStatItem.setItemMeta(activeStatMeta);
        inventory.setItem(10, activeStatItem);
        
        // Completed quests stat
        ItemStack completedStatItem = new ItemStack(Material.EMERALD);
        ItemMeta completedStatMeta = completedStatItem.getItemMeta();
        completedStatMeta.displayName(Component.text("Completed Quests").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        completedStatMeta.lore(List.of(
                Component.text("Total completed: " + completedCount).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        completedStatItem.setItemMeta(completedStatMeta);
        inventory.setItem(12, completedStatItem);
        
        // Available quests stat
        ItemStack availableStatItem = new ItemStack(Material.COMPASS);
        ItemMeta availableStatMeta = availableStatItem.getItemMeta();
        availableStatMeta.displayName(Component.text("Available Quests").color(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false));
        availableStatMeta.lore(List.of(
                Component.text("Available to accept: " + availableCount).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        availableStatItem.setItemMeta(availableStatMeta);
        inventory.setItem(14, availableStatItem);
        
        // Quest limit info
        int questLimit = getQuestLimitForPlayer(player);
        ItemStack limitItem = new ItemStack(Material.BARRIER);
        ItemMeta limitMeta = limitItem.getItemMeta();
        limitMeta.displayName(Component.text("Quest Limit").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        limitMeta.lore(List.of(
                Component.text("Active: " + activeCount + "/" + questLimit).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        limitItem.setItemMeta(limitMeta);
        inventory.setItem(16, limitItem);
    }
    
    private ItemStack createQuestItem(Quest quest, Player player, QuestItemType type) {
        ItemStack item = new ItemStack(quest.getDisplayItem());
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(quest.getName())
                .color(type == QuestItemType.AVAILABLE ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(quest.getDescription()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        // Add quest-specific lore
        quest.getLore().forEach(line -> 
                lore.add(Component.text(line).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
        
        lore.add(Component.empty());
        lore.add(Component.text("Type: " + quest.getType().getDisplayName()).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Target: " + quest.getTarget()).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        
        if (quest.hasTimeLimit()) {
            lore.add(Component.text("Time Limit: " + QuestUtils.formatTime(quest.getTimeLimit() / 1000))
                    .color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }
        
        if (type == QuestItemType.AVAILABLE) {
            lore.add(Component.empty());
            lore.add(Component.text("Click to accept this quest!").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createQuestProgressItem(Quest quest, QuestProgress progress, QuestItemType type) {
        ItemStack item = new ItemStack(quest.getDisplayItem());
        ItemMeta meta = item.getItemMeta();
        
        NamedTextColor nameColor = switch (type) {
            case ACTIVE -> NamedTextColor.YELLOW;
            case COMPLETED -> NamedTextColor.GREEN;
            case CLAIMED -> NamedTextColor.GRAY;
            default -> NamedTextColor.WHITE;
        };
        
        meta.displayName(Component.text(quest.getName()).color(nameColor).decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(quest.getDescription()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        // Progress bar
        String progressBar = QuestUtils.createProgressBar(progress.getProgress(), progress.getTarget(), 20);
        lore.add(Component.text("Progress: " + progressBar).color(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(progress.getProgress() + "/" + progress.getTarget() + 
                " (" + String.format("%.1f", progress.getProgressPercentage()) + "%)")
                .color(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false));
        
        // Time information
        if (progress.getExpiresAt() != null) {
            long timeLeft = progress.getTimeRemaining();
            if (timeLeft > 0) {
                lore.add(Component.text("Time Left: " + QuestUtils.formatTime(timeLeft / 1000))
                        .color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("EXPIRED").color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
            }
        }
        
        // Action text
        if (type == QuestItemType.COMPLETED) {
            lore.add(Component.empty());
            lore.add(Component.text("Click to claim reward!").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        } else if (type == QuestItemType.CLAIMED) {
            lore.add(Component.empty());
            lore.add(Component.text("Reward claimed!").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        QuestGUIState state = playerStates.get(player);
        if (state == null) return;
        
        event.setCancelled(true);
        
        int slot = event.getSlot();
        
        // Navigation clicks
        if (slot == 45) { // Available Quests
            state.setCurrentPage(QuestGUIState.PageType.AVAILABLE);
            state.setPage(0);
            updateGUI(player, state);
        } else if (slot == 46) { // Active Quests
            state.setCurrentPage(QuestGUIState.PageType.ACTIVE);
            state.setPage(0);
            updateGUI(player, state);
        } else if (slot == 47) { // Completed Quests
            state.setCurrentPage(QuestGUIState.PageType.COMPLETED);
            state.setPage(0);
            updateGUI(player, state);
        } else if (slot == 48) { // Statistics
            state.setCurrentPage(QuestGUIState.PageType.STATISTICS);
            state.setPage(0);
            updateGUI(player, state);
        } else if (slot == 51) { // Previous Page
            if (state.getPage() > 0) {
                state.setPage(state.getPage() - 1);
                updateGUI(player, state);
            }
        } else if (slot == 52) { // Next Page
            if (state.hasNextPage()) {
                state.setPage(state.getPage() + 1);
                updateGUI(player, state);
            }
        } else if (slot == 53) { // Close
            player.closeInventory();
        } else if (slot >= 0 && slot < 36) { // Quest item clicks
            handleQuestClick(player, state, slot);
        }
    }
    
    private void handleQuestClick(Player player, QuestGUIState state, int slot) {
        switch (state.getCurrentPage()) {
            case AVAILABLE -> {
                List<Quest> availableQuests = plugin.getQuestManager().getAvailableQuests(player);
                int questIndex = state.getPage() * 36 + slot;
                if (questIndex < availableQuests.size()) {
                    Quest quest = availableQuests.get(questIndex);
                    if (plugin.getQuestManager().acceptQuest(player, quest.getId())) {
                        updateGUI(player, state);
                    }
                }
            }
            case COMPLETED -> {
                List<QuestProgress> completedQuests = plugin.getQuestManager().getPlayerProgress(player)
                        .stream()
                        .filter(QuestProgress::isCompleted)
                        .toList();
                int questIndex = state.getPage() * 36 + slot;
                if (questIndex < completedQuests.size()) {
                    QuestProgress progress = completedQuests.get(questIndex);
                    if (!progress.isClaimed()) {
                        if (plugin.getQuestManager().claimReward(player, progress.getQuestId())) {
                            updateGUI(player, state);
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            playerStates.remove(player);
        }
    }
    
    private int getQuestLimitForPlayer(Player player) {
        if (player.hasPermission("questplus.bypass.limit")) {
            return Integer.MAX_VALUE;
        }
        
        var luckPerms = plugin.getLuckPermsIntegration();
        if (luckPerms != null) {
            String primaryGroup = luckPerms.getPrimaryGroup(player.getUniqueId());
            if (primaryGroup != null) {
                return plugin.getConfigManager().getQuestLimitForGroup(primaryGroup);
            }
        }
        
        return plugin.getConfigManager().getDefaultQuestLimit();
    }
    
    // Helper classes
    private static class QuestGUIState {
        public enum PageType {
            AVAILABLE, ACTIVE, COMPLETED, STATISTICS
        }
        
        private PageType currentPage = PageType.AVAILABLE;
        private int page = 0;
        private boolean hasNextPage = false;
        
        public PageType getCurrentPage() { return currentPage; }
        public void setCurrentPage(PageType currentPage) { this.currentPage = currentPage; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public boolean hasNextPage() { return hasNextPage; }
        public void setHasNextPage(boolean hasNextPage) { this.hasNextPage = hasNextPage; }
    }
    
    private enum QuestItemType {
        AVAILABLE, ACTIVE, COMPLETED, CLAIMED
    }
}
