package net.fliuxx.questplus.commands;

import net.fliuxx.questplus.QuestPlus;
import net.fliuxx.questplus.gui.QuestGUI;
import net.fliuxx.questplus.gui.QuestProgressGUI;
import net.fliuxx.questplus.quest.Quest;
import net.fliuxx.questplus.quest.QuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main command handler for QuestPlus player commands
 */
public class QuestPlusCommand implements CommandExecutor, TabCompleter {
    
    private final QuestPlus plugin;
    private final QuestGUI questGUI;
    private final QuestProgressGUI progressGUI;
    
    public QuestPlusCommand(QuestPlus plugin) {
        this.plugin = plugin;
        this.questGUI = new QuestGUI(plugin);
        this.progressGUI = new QuestProgressGUI(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().getMessage("player-only"));
            return true;
        }
        
        if (!player.hasPermission("questplus.use")) {
            plugin.getMessages().sendMessage(player, "no-permission");
            return true;
        }
        
        if (args.length == 0) {
            questGUI.openQuestGUI(player);
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "quests", "gui", "menu" -> {
                questGUI.openQuestGUI(player);
            }
            case "accept" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /questplus accept <quest-id>").color(NamedTextColor.RED));
                    return true;
                }
                handleAcceptQuest(player, args[1]);
            }
            case "abandon", "cancel" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /questplus abandon <quest-id>").color(NamedTextColor.RED));
                    return true;
                }
                handleAbandonQuest(player, args[1]);
            }
            case "progress", "info" -> {
                if (args.length < 2) {
                    showActiveQuests(player);
                } else {
                    progressGUI.openProgressGUI(player, args[1]);
                }
            }
            case "claim" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /questplus claim <quest-id>").color(NamedTextColor.RED));
                    return true;
                }
                handleClaimReward(player, args[1]);
            }
            case "list" -> {
                if (args.length > 1) {
                    String listType = args[1].toLowerCase();
                    switch (listType) {
                        case "active" -> showActiveQuests(player);
                        case "available" -> showAvailableQuests(player);
                        case "completed" -> showCompletedQuests(player);
                        default -> showAllQuests(player);
                    }
                } else {
                    showAllQuests(player);
                }
            }
            case "stats", "statistics" -> {
                showPlayerStatistics(player);
            }
            case "help" -> {
                showHelp(player);
            }
            default -> {
                player.sendMessage(Component.text("Unknown subcommand. Use /questplus help for help.").color(NamedTextColor.RED));
            }
        }
        
        return true;
    }
    
    private void handleAcceptQuest(Player player, String questId) {
        Quest quest = plugin.getQuestManager().getQuest(questId);
        if (quest == null) {
            plugin.getMessages().sendMessage(player, "quest-not-found");
            return;
        }
        
        plugin.getQuestManager().acceptQuest(player, questId);
    }
    
    private void handleAbandonQuest(Player player, String questId) {
        List<QuestProgress> playerProgress = plugin.getQuestManager().getPlayerProgress(player);
        QuestProgress progress = playerProgress.stream()
                .filter(p -> p.getQuestId().equals(questId) && !p.isCompleted())
                .findFirst()
                .orElse(null);
        
        if (progress == null) {
            plugin.getMessages().sendMessage(player, "quest-not-active");
            return;
        }
        
        // Remove from player progress and database
        playerProgress.remove(progress);
        plugin.getDatabaseManager().getQuestDatabase().deleteQuestProgress(player.getUniqueId(), questId);
        
        Quest quest = plugin.getQuestManager().getQuest(questId);
        String questName = quest != null ? quest.getName() : questId;
        
        player.sendMessage(Component.text("Abandoned quest: " + questName).color(NamedTextColor.YELLOW));
    }
    
    private void handleClaimReward(Player player, String questId) {
        plugin.getQuestManager().claimReward(player, questId);
    }
    
    private void showActiveQuests(Player player) {
        List<QuestProgress> activeQuests = plugin.getQuestManager().getPlayerProgress(player)
                .stream()
                .filter(p -> !p.isCompleted())
                .toList();
        
        if (activeQuests.isEmpty()) {
            player.sendMessage(Component.text("You have no active quests.").color(NamedTextColor.YELLOW));
            return;
        }
        
        player.sendMessage(Component.text("=== Active Quests ===").color(NamedTextColor.GREEN));
        for (QuestProgress progress : activeQuests) {
            Quest quest = plugin.getQuestManager().getQuest(progress.getQuestId());
            if (quest != null) {
                Component message = Component.text("• " + quest.getName() + " - ")
                        .color(NamedTextColor.WHITE)
                        .append(Component.text(progress.getProgress() + "/" + progress.getTarget())
                                .color(NamedTextColor.BLUE))
                        .append(Component.text(" (" + String.format("%.1f", progress.getProgressPercentage()) + "%)")
                                .color(NamedTextColor.GRAY));
                player.sendMessage(message);
            }
        }
    }
    
    private void showAvailableQuests(Player player) {
        List<Quest> availableQuests = plugin.getQuestManager().getAvailableQuests(player);
        
        if (availableQuests.isEmpty()) {
            player.sendMessage(Component.text("No quests available for you.").color(NamedTextColor.YELLOW));
            return;
        }
        
        player.sendMessage(Component.text("=== Available Quests ===").color(NamedTextColor.GREEN));
        for (Quest quest : availableQuests) {
            Component message = Component.text("• " + quest.getName() + " - " + quest.getDescription())
                    .color(NamedTextColor.WHITE);
            player.sendMessage(message);
        }
    }
    
    private void showCompletedQuests(Player player) {
        List<QuestProgress> completedQuests = plugin.getQuestManager().getPlayerProgress(player)
                .stream()
                .filter(QuestProgress::isCompleted)
                .toList();
        
        if (completedQuests.isEmpty()) {
            player.sendMessage(Component.text("You have no completed quests.").color(NamedTextColor.YELLOW));
            return;
        }
        
        player.sendMessage(Component.text("=== Completed Quests ===").color(NamedTextColor.GREEN));
        for (QuestProgress progress : completedQuests) {
            Quest quest = plugin.getQuestManager().getQuest(progress.getQuestId());
            if (quest != null) {
                Component message = Component.text("• " + quest.getName())
                        .color(progress.isClaimed() ? NamedTextColor.GRAY : NamedTextColor.GREEN);
                if (!progress.isClaimed()) {
                    message = message.append(Component.text(" (Reward Available)").color(NamedTextColor.GOLD));
                }
                player.sendMessage(message);
            }
        }
    }
    
    private void showAllQuests(Player player) {
        player.sendMessage(Component.text("=== Quest Summary ===").color(NamedTextColor.GOLD));
        
        int activeCount = plugin.getQuestManager().getActiveQuestCount(player);
        int completedCount = plugin.getQuestManager().getCompletedQuestCount(player);
        int availableCount = plugin.getQuestManager().getAvailableQuests(player).size();
        
        player.sendMessage(Component.text("Active Quests: " + activeCount).color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Completed Quests: " + completedCount).color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Available Quests: " + availableCount).color(NamedTextColor.BLUE));
        
        player.sendMessage(Component.text("Use /questplus gui to open the quest menu!").color(NamedTextColor.AQUA));
    }
    
    private void showPlayerStatistics(Player player) {
        int activeCount = plugin.getQuestManager().getActiveQuestCount(player);
        int completedCount = plugin.getQuestManager().getCompletedQuestCount(player);
        int availableCount = plugin.getQuestManager().getAvailableQuests(player).size();
        
        player.sendMessage(Component.text("=== Quest Statistics ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Active Quests: " + activeCount).color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Completed Quests: " + completedCount).color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Available Quests: " + availableCount).color(NamedTextColor.BLUE));
        
        // Calculate completion rate
        if (completedCount > 0) {
            double completionRate = (double) completedCount / (completedCount + activeCount) * 100;
            player.sendMessage(Component.text("Completion Rate: " + String.format("%.1f", completionRate) + "%")
                    .color(NamedTextColor.AQUA));
        }
    }
    
    private void showHelp(Player player) {
        player.sendMessage(Component.text("=== QuestPlus Commands ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/questplus - Open quest GUI").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/questplus accept <quest-id> - Accept a quest").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/questplus abandon <quest-id> - Abandon a quest").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/questplus progress [quest-id] - View progress").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/questplus claim <quest-id> - Claim quest reward").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/questplus list [active|available|completed] - List quests").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/questplus stats - View your statistics").color(NamedTextColor.WHITE));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("quests", "accept", "abandon", "progress", "claim", "list", "stats", "help")
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            
            switch (subcommand) {
                case "accept" -> {
                    return plugin.getQuestManager().getAvailableQuests(player)
                            .stream()
                            .map(Quest::getId)
                            .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                case "abandon", "progress", "claim" -> {
                    return plugin.getQuestManager().getPlayerProgress(player)
                            .stream()
                            .filter(p -> subcommand.equals("abandon") ? !p.isCompleted() : 
                                        subcommand.equals("claim") ? p.isCompleted() && !p.isClaimed() : true)
                            .map(QuestProgress::getQuestId)
                            .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                case "list" -> {
                    return Arrays.asList("active", "available", "completed")
                            .stream()
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }
        
        return new ArrayList<>();
    }
}
