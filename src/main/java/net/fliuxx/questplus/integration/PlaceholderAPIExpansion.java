package net.fliuxx.questplus.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.fliuxx.questplus.QuestPlus;
import net.fliuxx.questplus.quest.Quest;
import net.fliuxx.questplus.quest.QuestProgress;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * PlaceholderAPI expansion for QuestPlus
 * 
 * Available placeholders:
 * - %questplus_active% - Number of active quests
 * - %questplus_completed% - Number of completed quests
 * - %questplus_available% - Number of available quests
 * - %questplus_progress_<quest-id>% - Progress of specific quest
 * - %questplus_percentage_<quest-id>% - Percentage of specific quest
 * - %questplus_status_<quest-id>% - Status of specific quest
 * - %questplus_time_left_<quest-id>% - Time left for quest
 */
public class PlaceholderAPIExpansion extends PlaceholderExpansion {
    
    private final QuestPlus plugin;
    
    public PlaceholderAPIExpansion(QuestPlus plugin) {
        this.plugin = plugin;
    }
    
    @Override
    @NotNull
    public String getIdentifier() {
        return "questplus";
    }
    
    @Override
    @NotNull
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    @Nullable
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.hasPlayedBefore()) {
            return "0";
        }
        
        switch (params.toLowerCase()) {
            case "active" -> {
                if (player.isOnline()) {
                    return String.valueOf(plugin.getQuestManager().getActiveQuestCount(player.getPlayer()));
                } else {
                    return String.valueOf(plugin.getDatabaseManager().getQuestDatabase().getActiveQuestCount(player.getUniqueId()));
                }
            }
            case "completed" -> {
                return String.valueOf(plugin.getDatabaseManager().getQuestDatabase().getCompletedQuestCount(player.getUniqueId()));
            }
            case "available" -> {
                if (player.isOnline()) {
                    return String.valueOf(plugin.getQuestManager().getAvailableQuests(player.getPlayer()).size());
                } else {
                    return "0"; // Can't calculate available quests for offline players
                }
            }
        }
        
        // Handle quest-specific placeholders
        if (params.startsWith("progress_")) {
            String questId = params.substring("progress_".length());
            QuestProgress progress = getQuestProgress(player, questId);
            return progress != null ? String.valueOf(progress.getProgress()) : "0";
        }
        
        if (params.startsWith("target_")) {
            String questId = params.substring("target_".length());
            QuestProgress progress = getQuestProgress(player, questId);
            return progress != null ? String.valueOf(progress.getTarget()) : "0";
        }
        
        if (params.startsWith("percentage_")) {
            String questId = params.substring("percentage_".length());
            QuestProgress progress = getQuestProgress(player, questId);
            return progress != null ? String.format("%.1f", progress.getProgressPercentage()) : "0.0";
        }
        
        if (params.startsWith("status_")) {
            String questId = params.substring("status_".length());
            QuestProgress progress = getQuestProgress(player, questId);
            if (progress == null) {
                return "Not Started";
            } else if (progress.isCompleted()) {
                return progress.isClaimed() ? "Claimed" : "Completed";
            } else if (progress.isExpired()) {
                return "Expired";
            } else {
                return "Active";
            }
        }
        
        if (params.startsWith("time_left_")) {
            String questId = params.substring("time_left_".length());
            QuestProgress progress = getQuestProgress(player, questId);
            if (progress == null || progress.getExpiresAt() == null) {
                return "No Limit";
            }
            
            long timeLeft = progress.getTimeRemaining();
            if (timeLeft <= 0) {
                return "Expired";
            }
            
            return formatTime(timeLeft / 1000);
        }
        
        if (params.startsWith("name_")) {
            String questId = params.substring("name_".length());
            Quest quest = plugin.getQuestManager().getQuest(questId);
            return quest != null ? quest.getName() : "Unknown Quest";
        }
        
        if (params.startsWith("description_")) {
            String questId = params.substring("description_".length());
            Quest quest = plugin.getQuestManager().getQuest(questId);
            return quest != null ? quest.getDescription() : "Unknown Quest";
        }
        
        if (params.startsWith("type_")) {
            String questId = params.substring("type_".length());
            Quest quest = plugin.getQuestManager().getQuest(questId);
            return quest != null ? quest.getType().getDisplayName() : "Unknown";
        }
        
        return null; // Placeholder not found
    }
    
    private QuestProgress getQuestProgress(OfflinePlayer player, String questId) {
        if (player.isOnline()) {
            List<QuestProgress> progress = plugin.getQuestManager().getPlayerProgress(player.getPlayer());
            return progress.stream()
                    .filter(p -> p.getQuestId().equals(questId))
                    .findFirst()
                    .orElse(null);
        } else {
            return plugin.getDatabaseManager().getQuestDatabase().getQuestProgress(player.getUniqueId(), questId);
        }
    }
    
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        } else {
            long days = seconds / 86400;
            long hours = (seconds % 86400) / 3600;
            return days + "d " + hours + "h";
        }
    }
}
