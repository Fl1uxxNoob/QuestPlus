package net.fliuxx.questplus.commands;

import net.fliuxx.questplus.QuestPlus;
import net.fliuxx.questplus.quest.Quest;
import net.fliuxx.questplus.quest.QuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin command handler for QuestPlus
 */
public class QuestAdminCommand implements CommandExecutor, TabCompleter {
    
    private final QuestPlus plugin;
    
    public QuestAdminCommand(QuestPlus plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("questplus.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length == 0) {
            showAdminHelp(sender);
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "reload" -> handleReload(sender);
            case "give" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /questadmin give <player> <quest-id>").color(NamedTextColor.RED));
                    return true;
                }
                handleGiveQuest(sender, args[1], args[2]);
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /questadmin remove <player> <quest-id>").color(NamedTextColor.RED));
                    return true;
                }
                handleRemoveQuest(sender, args[1], args[2]);
            }
            case "complete" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /questadmin complete <player> <quest-id>").color(NamedTextColor.RED));
                    return true;
                }
                handleCompleteQuest(sender, args[1], args[2]);
            }
            case "progress" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /questadmin progress <player> <quest-id> <amount>").color(NamedTextColor.RED));
                    return true;
                }
                handleSetProgress(sender, args[1], args[2], args[3]);
            }
            case "reset" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /questadmin reset <player> <quest-id>").color(NamedTextColor.RED));
                    return true;
                }
                handleResetQuest(sender, args[1], args[2]);
            }
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /questadmin info <quest-id>").color(NamedTextColor.RED));
                    return true;
                }
                handleQuestInfo(sender, args[1]);
            }
            case "list" -> {
                if (args.length > 1) {
                    handlePlayerQuests(sender, args[1]);
                } else {
                    handleListQuests(sender);
                }
            }
            case "stats" -> {
                if (args.length < 2) {
                    handleGlobalStats(sender);
                } else {
                    handlePlayerStats(sender, args[1]);
                }
            }
            case "purge" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /questadmin purge <player>").color(NamedTextColor.RED));
                    return true;
                }
                handlePurgePlayer(sender, args[1]);
            }
            case "help" -> showAdminHelp(sender);
            default -> sender.sendMessage(Component.text("Unknown subcommand. Use /questadmin help for help.").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private void handleReload(CommandSender sender) {
        try {
            plugin.reload();
            sender.sendMessage(Component.text("QuestPlus has been reloaded successfully!").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            sender.sendMessage(Component.text("Failed to reload QuestPlus: " + e.getMessage()).color(NamedTextColor.RED));
            plugin.getLogger().severe("Failed to reload: " + e.getMessage());
        }
    }
    
    private void handleGiveQuest(CommandSender sender, String playerName, String questId) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found or not online.").color(NamedTextColor.RED));
            return;
        }
        
        Quest quest = plugin.getQuestManager().getQuest(questId);
        if (quest == null) {
            sender.sendMessage(Component.text("Quest not found: " + questId).color(NamedTextColor.RED));
            return;
        }
        
        if (plugin.getQuestManager().acceptQuest(target, questId)) {
            sender.sendMessage(Component.text("Given quest '" + quest.getName() + "' to " + target.getName()).color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("An admin has given you the quest: " + quest.getName()).color(NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("Failed to give quest to " + target.getName()).color(NamedTextColor.RED));
        }
    }
    
    private void handleRemoveQuest(CommandSender sender, String playerName, String questId) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found or not online.").color(NamedTextColor.RED));
            return;
        }
        
        List<QuestProgress> playerProgress = plugin.getQuestManager().getPlayerProgress(target);
        QuestProgress progress = playerProgress.stream()
                .filter(p -> p.getQuestId().equals(questId))
                .findFirst()
                .orElse(null);
        
        if (progress == null) {
            sender.sendMessage(Component.text("Player doesn't have this quest.").color(NamedTextColor.RED));
            return;
        }
        
        playerProgress.remove(progress);
        plugin.getDatabaseManager().getQuestDatabase().deleteQuestProgress(target.getUniqueId(), questId);
        
        Quest quest = plugin.getQuestManager().getQuest(questId);
        String questName = quest != null ? quest.getName() : questId;
        
        sender.sendMessage(Component.text("Removed quest '" + questName + "' from " + target.getName()).color(NamedTextColor.GREEN));
        target.sendMessage(Component.text("An admin has removed your quest: " + questName).color(NamedTextColor.YELLOW));
    }
    
    private void handleCompleteQuest(CommandSender sender, String playerName, String questId) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found or not online.").color(NamedTextColor.RED));
            return;
        }
        
        List<QuestProgress> playerProgress = plugin.getQuestManager().getPlayerProgress(target);
        QuestProgress progress = playerProgress.stream()
                .filter(p -> p.getQuestId().equals(questId) && !p.isCompleted())
                .findFirst()
                .orElse(null);
        
        if (progress == null) {
            sender.sendMessage(Component.text("Player doesn't have this active quest.").color(NamedTextColor.RED));
            return;
        }
        
        progress.setProgress(progress.getTarget());
        progress.setCompleted(true);
        progress.setCompletedAt(System.currentTimeMillis());
        
        plugin.getDatabaseManager().getQuestDatabase().saveQuestProgress(progress);
        
        Quest quest = plugin.getQuestManager().getQuest(questId);
        String questName = quest != null ? quest.getName() : questId;
        
        sender.sendMessage(Component.text("Completed quest '" + questName + "' for " + target.getName()).color(NamedTextColor.GREEN));
        target.sendMessage(Component.text("An admin has completed your quest: " + questName).color(NamedTextColor.GREEN));
    }
    
    private void handleSetProgress(CommandSender sender, String playerName, String questId, String amountStr) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found or not online.").color(NamedTextColor.RED));
            return;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount: " + amountStr).color(NamedTextColor.RED));
            return;
        }
        
        List<QuestProgress> playerProgress = plugin.getQuestManager().getPlayerProgress(target);
        QuestProgress progress = playerProgress.stream()
                .filter(p -> p.getQuestId().equals(questId) && !p.isCompleted())
                .findFirst()
                .orElse(null);
        
        if (progress == null) {
            sender.sendMessage(Component.text("Player doesn't have this active quest.").color(NamedTextColor.RED));
            return;
        }
        
        progress.setProgress(Math.max(0, Math.min(amount, progress.getTarget())));
        plugin.getDatabaseManager().getQuestDatabase().saveQuestProgress(progress);
        
        Quest quest = plugin.getQuestManager().getQuest(questId);
        String questName = quest != null ? quest.getName() : questId;
        
        sender.sendMessage(Component.text("Set progress for quest '" + questName + "' to " + progress.getProgress() + "/" + progress.getTarget()).color(NamedTextColor.GREEN));
    }
    
    private void handleResetQuest(CommandSender sender, String playerName, String questId) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found or not online.").color(NamedTextColor.RED));
            return;
        }
        
        List<QuestProgress> playerProgress = plugin.getQuestManager().getPlayerProgress(target);
        QuestProgress progress = playerProgress.stream()
                .filter(p -> p.getQuestId().equals(questId))
                .findFirst()
                .orElse(null);
        
        if (progress == null) {
            sender.sendMessage(Component.text("Player doesn't have this quest.").color(NamedTextColor.RED));
            return;
        }
        
        progress.setProgress(0);
        progress.setCompleted(false);
        progress.setClaimed(false);
        progress.setCompletedAt(null);
        progress.setStartedAt(System.currentTimeMillis());
        
        plugin.getDatabaseManager().getQuestDatabase().saveQuestProgress(progress);
        
        Quest quest = plugin.getQuestManager().getQuest(questId);
        String questName = quest != null ? quest.getName() : questId;
        
        sender.sendMessage(Component.text("Reset quest '" + questName + "' for " + target.getName()).color(NamedTextColor.GREEN));
    }
    
    private void handleQuestInfo(CommandSender sender, String questId) {
        Quest quest = plugin.getQuestManager().getQuest(questId);
        if (quest == null) {
            sender.sendMessage(Component.text("Quest not found: " + questId).color(NamedTextColor.RED));
            return;
        }
        
        sender.sendMessage(Component.text("=== Quest Info: " + quest.getName() + " ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("ID: " + quest.getId()).color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Description: " + quest.getDescription()).color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Type: " + quest.getType().getDisplayName()).color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Target: " + quest.getTarget()).color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Repeatable: " + quest.isRepeatable()).color(NamedTextColor.WHITE));
        
        if (quest.hasTimeLimit()) {
            sender.sendMessage(Component.text("Time Limit: " + (quest.getTimeLimit() / 1000) + " seconds").color(NamedTextColor.WHITE));
        }
        
        if (quest.getPermission() != null) {
            sender.sendMessage(Component.text("Permission: " + quest.getPermission()).color(NamedTextColor.WHITE));
        }
        
        if (quest.getCooldown() > 0) {
            sender.sendMessage(Component.text("Cooldown: " + quest.getCooldown() + " seconds").color(NamedTextColor.WHITE));
        }
    }
    
    private void handleListQuests(CommandSender sender) {
        var allQuests = plugin.getQuestManager().getAllQuests();
        
        sender.sendMessage(Component.text("=== All Quests ===").color(NamedTextColor.GOLD));
        for (Quest quest : allQuests) {
            sender.sendMessage(Component.text("• " + quest.getId() + " - " + quest.getName()).color(NamedTextColor.WHITE));
        }
        sender.sendMessage(Component.text("Total: " + allQuests.size() + " quests").color(NamedTextColor.GRAY));
    }
    
    private void handlePlayerQuests(CommandSender sender, String playerName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player not found: " + playerName).color(NamedTextColor.RED));
            return;
        }
        
        List<QuestProgress> playerProgress = plugin.getDatabaseManager().getQuestDatabase().getPlayerProgress(target.getUniqueId());
        
        sender.sendMessage(Component.text("=== Quests for " + target.getName() + " ===").color(NamedTextColor.GOLD));
        
        List<QuestProgress> active = playerProgress.stream().filter(p -> !p.isCompleted()).toList();
        List<QuestProgress> completed = playerProgress.stream().filter(QuestProgress::isCompleted).toList();
        
        sender.sendMessage(Component.text("Active Quests:").color(NamedTextColor.YELLOW));
        for (QuestProgress progress : active) {
            Quest quest = plugin.getQuestManager().getQuest(progress.getQuestId());
            String questName = quest != null ? quest.getName() : progress.getQuestId();
            sender.sendMessage(Component.text("• " + questName + " - " + progress.getProgress() + "/" + progress.getTarget()).color(NamedTextColor.WHITE));
        }
        
        sender.sendMessage(Component.text("Completed Quests:").color(NamedTextColor.GREEN));
        for (QuestProgress progress : completed) {
            Quest quest = plugin.getQuestManager().getQuest(progress.getQuestId());
            String questName = quest != null ? quest.getName() : progress.getQuestId();
            String status = progress.isClaimed() ? " (Claimed)" : " (Unclaimed)";
            sender.sendMessage(Component.text("• " + questName + status).color(NamedTextColor.WHITE));
        }
    }
    
    private void handleGlobalStats(CommandSender sender) {
        // This would require more database queries to implement properly
        sender.sendMessage(Component.text("=== Global Quest Statistics ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Total Quests: " + plugin.getQuestManager().getAllQuests().size()).color(NamedTextColor.WHITE));
        // Add more global statistics as needed
    }
    
    private void handlePlayerStats(CommandSender sender, String playerName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player not found: " + playerName).color(NamedTextColor.RED));
            return;
        }
        
        int activeCount = plugin.getDatabaseManager().getQuestDatabase().getActiveQuestCount(target.getUniqueId());
        int completedCount = plugin.getDatabaseManager().getQuestDatabase().getCompletedQuestCount(target.getUniqueId());
        
        sender.sendMessage(Component.text("=== Quest Statistics for " + target.getName() + " ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Active Quests: " + activeCount).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Completed Quests: " + completedCount).color(NamedTextColor.GREEN));
    }
    
    private void handlePurgePlayer(CommandSender sender, String playerName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player not found: " + playerName).color(NamedTextColor.RED));
            return;
        }
        
        // Remove all player progress
        List<QuestProgress> playerProgress = plugin.getDatabaseManager().getQuestDatabase().getPlayerProgress(target.getUniqueId());
        for (QuestProgress progress : playerProgress) {
            plugin.getDatabaseManager().getQuestDatabase().deleteQuestProgress(target.getUniqueId(), progress.getQuestId());
        }
        
        sender.sendMessage(Component.text("Purged all quest data for " + target.getName()).color(NamedTextColor.GREEN));
    }
    
    private void showAdminHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== QuestPlus Admin Commands ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/questadmin reload - Reload the plugin").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/questadmin give <player> <quest-id> - Give quest to player").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/questadmin remove <player> <quest-id> - Remove quest from player").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/questadmin complete <player> <quest-id> - Complete quest for player").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/questadmin progress <player> <quest-id> <amount> - Set quest progress").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/questadmin reset <player> <quest-id> - Reset quest progress").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/questadmin info <quest-id> - View quest information").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/questadmin list [player] - List quests or player quests").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/questadmin stats [player] - View statistics").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/questadmin purge <player> - Remove all quest data for player").color(NamedTextColor.WHITE));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("questplus.admin")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("reload", "give", "remove", "complete", "progress", "reset", "info", "list", "stats", "purge", "help")
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            
            if (Arrays.asList("give", "remove", "complete", "progress", "reset", "list", "stats", "purge").contains(subcommand)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            
            if (subcommand.equals("info")) {
                return plugin.getQuestManager().getAllQuests().stream()
                        .map(Quest::getId)
                        .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3) {
            String subcommand = args[0].toLowerCase();
            
            if (Arrays.asList("give", "remove", "complete", "progress", "reset").contains(subcommand)) {
                return plugin.getQuestManager().getAllQuests().stream()
                        .map(Quest::getId)
                        .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
}
