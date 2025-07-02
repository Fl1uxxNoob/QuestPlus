package net.fliuxx.questplus.quest;

import net.fliuxx.questplus.QuestPlus;
import net.fliuxx.questplus.integration.LuckPermsIntegration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all quests and player progress
 */
public class QuestManager {
    
    private final QuestPlus plugin;
    private final Map<String, Quest> quests;
    private final Map<UUID, List<QuestProgress>> playerProgress;
    private final Map<UUID, Map<String, Long>> questCooldowns;
    
    public QuestManager(QuestPlus plugin) {
        this.plugin = plugin;
        this.quests = new ConcurrentHashMap<>();
        this.playerProgress = new ConcurrentHashMap<>();
        this.questCooldowns = new ConcurrentHashMap<>();
        
        startAutoSaveTask();
        startExpirationTask();
    }
    
    public void loadQuests() {
        quests.clear();
        
        ConfigurationSection questsSection = plugin.getConfigManager().getQuestsConfig().getConfigurationSection("quests");
        if (questsSection == null) {
            plugin.getLogger().warning("No quests found in quests.yml");
            return;
        }
        
        for (String questId : questsSection.getKeys(false)) {
            try {
                ConfigurationSection questConfig = questsSection.getConfigurationSection(questId);
                Quest quest = Quest.fromConfig(questId, questConfig);
                quests.put(questId, quest);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load quest: " + questId, e);
            }
        }
        
        plugin.getLogger().info("Loaded " + quests.size() + " quests");
    }
    
    public void loadPlayerProgress(Player player) {
        UUID uuid = player.getUniqueId();
        List<QuestProgress> progress = plugin.getDatabaseManager().getQuestDatabase().getPlayerProgress(uuid);
        playerProgress.put(uuid, progress);
        
        // Update player in database
        plugin.getDatabaseManager().getQuestDatabase().createOrUpdatePlayer(uuid, player.getName());
    }
    
    public void unloadPlayerProgress(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Save progress before unloading
        List<QuestProgress> progress = playerProgress.get(uuid);
        if (progress != null) {
            for (QuestProgress questProgress : progress) {
                plugin.getDatabaseManager().getQuestDatabase().saveQuestProgress(questProgress);
            }
        }
        
        playerProgress.remove(uuid);
        questCooldowns.remove(uuid);
    }
    
    public boolean acceptQuest(Player player, String questId) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return false;
        }
        
        UUID uuid = player.getUniqueId();
        
        // Check if player can accept the quest
        if (!quest.canPlayerAccept(player)) {
            plugin.getMessages().sendMessage(player, "no-permission");
            return false;
        }
        
        // Check if already active
        if (hasActiveQuest(player, questId)) {
            plugin.getMessages().sendMessage(player, "quest-already-active");
            return false;
        }
        
        // Check quest limits
        if (!canAcceptMoreQuests(player)) {
            plugin.getMessages().sendMessage(player, "quest-limit-reached");
            return false;
        }
        
        // Check cooldown
        if (isOnCooldown(player, questId)) {
            plugin.getMessages().sendMessage(player, "quest-on-cooldown");
            return false;
        }
        
        // Create quest progress
        QuestProgress progress = new QuestProgress(uuid, questId, quest.getTarget());
        if (quest.hasTimeLimit()) {
            progress.setExpiresAt(System.currentTimeMillis() + quest.getTimeLimit());
        }
        
        // Add to player progress
        List<QuestProgress> playerQuests = playerProgress.computeIfAbsent(uuid, k -> new ArrayList<>());
        playerQuests.add(progress);
        
        // Save to database
        plugin.getDatabaseManager().getQuestDatabase().saveQuestProgress(progress);
        
        // Send confirmation message
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("quest", quest.getName());
        plugin.getMessages().sendMessage(player, "quest-accepted", placeholders);
        
        return true;
    }
    
    public boolean hasActiveQuest(Player player, String questId) {
        List<QuestProgress> progress = playerProgress.get(player.getUniqueId());
        if (progress == null) return false;
        
        return progress.stream()
                .anyMatch(p -> p.getQuestId().equals(questId) && !p.isCompleted());
    }
    
    public boolean canAcceptMoreQuests(Player player) {
        if (player.hasPermission("questplus.bypass.limit")) {
            return true;
        }
        
        UUID uuid = player.getUniqueId();
        int activeQuests = getActiveQuestCount(player);
        int limit = getQuestLimitForPlayer(player);
        
        return activeQuests < limit;
    }
    
    private int getQuestLimitForPlayer(Player player) {
        LuckPermsIntegration luckPerms = plugin.getLuckPermsIntegration();
        if (luckPerms != null) {
            String primaryGroup = luckPerms.getPrimaryGroup(player.getUniqueId());
            if (primaryGroup != null) {
                return plugin.getConfigManager().getQuestLimitForGroup(primaryGroup);
            }
        }
        
        return plugin.getConfigManager().getDefaultQuestLimit();
    }
    
    private boolean isOnCooldown(Player player, String questId) {
        Map<String, Long> cooldowns = questCooldowns.get(player.getUniqueId());
        if (cooldowns == null) return false;
        
        Long cooldownEnd = cooldowns.get(questId);
        if (cooldownEnd == null) return false;
        
        return System.currentTimeMillis() < cooldownEnd;
    }
    
    public void updateQuestProgress(Player player, String questId, int amount) {
        List<QuestProgress> progress = playerProgress.get(player.getUniqueId());
        if (progress == null) return;
        
        QuestProgress questProgress = progress.stream()
                .filter(p -> p.getQuestId().equals(questId) && !p.isCompleted())
                .findFirst()
                .orElse(null);
        
        if (questProgress == null) return;
        
        questProgress.addProgress(amount);
        
        // Check if completed
        if (questProgress.getProgress() >= questProgress.getTarget()) {
            completeQuest(player, questProgress);
        } else {
            // Send progress message
            Quest quest = quests.get(questId);
            if (quest != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("quest", quest.getName());
                placeholders.put("current", String.valueOf(questProgress.getProgress()));
                placeholders.put("target", String.valueOf(questProgress.getTarget()));
                plugin.getMessages().sendMessage(player, "quest-progress", placeholders);
            }
        }
    }
    
    private void completeQuest(Player player, QuestProgress progress) {
        Quest quest = quests.get(progress.getQuestId());
        if (quest == null) return;
        
        progress.setCompleted(true);
        progress.setCompletedAt(System.currentTimeMillis());
        
        // Save completion time for statistics
        long completionTime = progress.getCompletedAt() - progress.getStartedAt();
        plugin.getDatabaseManager().getQuestDatabase().saveQuestStatistics(
                player.getUniqueId(), progress.getQuestId(), completionTime);
        
        // Set cooldown if applicable
        if (quest.getCooldown() > 0) {
            Map<String, Long> cooldowns = questCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            cooldowns.put(progress.getQuestId(), System.currentTimeMillis() + (quest.getCooldown() * 1000L));
        }
        
        // Send completion message
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("quest", quest.getName());
        plugin.getMessages().sendMessage(player, "quest-completed", placeholders);
        
        // Save to database
        plugin.getDatabaseManager().getQuestDatabase().saveQuestProgress(progress);
    }
    
    public boolean claimReward(Player player, String questId) {
        List<QuestProgress> progress = playerProgress.get(player.getUniqueId());
        if (progress == null) return false;
        
        QuestProgress questProgress = progress.stream()
                .filter(p -> p.getQuestId().equals(questId) && p.isCompleted() && !p.isClaimed())
                .findFirst()
                .orElse(null);
        
        if (questProgress == null) return false;
        
        Quest quest = quests.get(questId);
        if (quest == null) return false;
        
        // Give reward
        quest.getReward().give(player);
        questProgress.setClaimed(true);
        
        // Remove from active quests if not repeatable
        if (!quest.isRepeatable()) {
            progress.remove(questProgress);
            plugin.getDatabaseManager().getQuestDatabase().deleteQuestProgress(player.getUniqueId(), questId);
        } else {
            plugin.getDatabaseManager().getQuestDatabase().saveQuestProgress(questProgress);
        }
        
        // Send reward claimed message
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("quest", quest.getName());
        plugin.getMessages().sendMessage(player, "reward-claimed", placeholders);
        
        return true;
    }
    
    public int getActiveQuestCount(Player player) {
        List<QuestProgress> progress = playerProgress.get(player.getUniqueId());
        if (progress == null) return 0;
        
        return (int) progress.stream()
                .filter(p -> !p.isCompleted())
                .count();
    }
    
    public int getCompletedQuestCount(Player player) {
        return plugin.getDatabaseManager().getQuestDatabase().getCompletedQuestCount(player.getUniqueId());
    }
    
    public List<QuestProgress> getPlayerProgress(Player player) {
        return playerProgress.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }
    
    public List<Quest> getAvailableQuests(Player player) {
        return quests.values().stream()
                .filter(quest -> quest.canPlayerAccept(player))
                .filter(quest -> !hasActiveQuest(player, quest.getId()))
                .filter(quest -> !isOnCooldown(player, quest.getId()))
                .toList();
    }
    
    public Quest getQuest(String questId) {
        return quests.get(questId);
    }
    
    public Collection<Quest> getAllQuests() {
        return quests.values();
    }
    
    public void saveAllProgress() {
        for (List<QuestProgress> progressList : playerProgress.values()) {
            for (QuestProgress progress : progressList) {
                plugin.getDatabaseManager().getQuestDatabase().saveQuestProgress(progress);
            }
        }
    }
    
    private void startAutoSaveTask() {
        if (!plugin.getConfigManager().isAutoSaveEnabled()) {
            return;
        }
        
        int interval = plugin.getConfigManager().getAutoSaveInterval() * 20; // Convert to ticks
        
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAllProgress();
            }
        }.runTaskTimerAsynchronously(plugin, interval, interval);
    }
    
    private void startExpirationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                
                for (Map.Entry<UUID, List<QuestProgress>> entry : playerProgress.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null) continue;
                    
                    List<QuestProgress> progressList = entry.getValue();
                    List<QuestProgress> toRemove = new ArrayList<>();
                    
                    for (QuestProgress progress : progressList) {
                        if (progress.getExpiresAt() != null && progress.getExpiresAt() <= currentTime) {
                            Quest quest = quests.get(progress.getQuestId());
                            if (quest != null) {
                                Map<String, String> placeholders = new HashMap<>();
                                placeholders.put("quest", quest.getName());
                                plugin.getMessages().sendMessage(player, "quest-expired", placeholders);
                            }
                            
                            toRemove.add(progress);
                            plugin.getDatabaseManager().getQuestDatabase().deleteQuestProgress(entry.getKey(), progress.getQuestId());
                        }
                    }
                    
                    progressList.removeAll(toRemove);
                }
            }
        }.runTaskTimer(plugin, 20 * 60, 20 * 60); // Check every minute
    }
}
