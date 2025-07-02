package net.fliuxx.questplus.quest;

import net.fliuxx.questplus.quest.types.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * Represents a quest that players can complete
 */
public class Quest {
    
    private final String id;
    private final String name;
    private final String description;
    private final List<String> lore;
    private final QuestType type;
    private final int target;
    private final Material displayItem;
    private final QuestReward reward;
    private final String permission;
    private final long timeLimit; // in milliseconds, 0 = no limit
    private final boolean repeatable;
    private final int cooldown; // in seconds
    private final ConfigurationSection typeConfig;
    
    public Quest(String id, String name, String description, List<String> lore, 
                 QuestType type, int target, Material displayItem, QuestReward reward,
                 String permission, long timeLimit, boolean repeatable, int cooldown,
                 ConfigurationSection typeConfig) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.lore = lore;
        this.type = type;
        this.target = target;
        this.displayItem = displayItem;
        this.reward = reward;
        this.permission = permission;
        this.timeLimit = timeLimit;
        this.repeatable = repeatable;
        this.cooldown = cooldown;
        this.typeConfig = typeConfig;
    }
    
    public static Quest fromConfig(String id, ConfigurationSection config) {
        String name = config.getString("name", id);
        String description = config.getString("description", "");
        List<String> lore = config.getStringList("lore");
        
        String typeString = config.getString("type", "COLLECT");
        QuestType type = QuestType.valueOf(typeString.toUpperCase());
        
        int target = config.getInt("target", 1);
        
        String materialString = config.getString("display-item", "PAPER");
        Material displayItem = Material.valueOf(materialString.toUpperCase());
        
        QuestReward reward = QuestReward.fromConfig(config.getConfigurationSection("reward"));
        
        String permission = config.getString("permission");
        long timeLimit = config.getLong("time-limit", 0) * 1000; // Convert seconds to milliseconds
        boolean repeatable = config.getBoolean("repeatable", true);
        int cooldown = config.getInt("cooldown", 0);
        
        ConfigurationSection typeConfig = config.getConfigurationSection("type-config");
        
        return new Quest(id, name, description, lore, type, target, displayItem, 
                        reward, permission, timeLimit, repeatable, cooldown, typeConfig);
    }
    
    public boolean canPlayerAccept(org.bukkit.entity.Player player) {
        if (permission != null && !player.hasPermission(permission)) {
            return false;
        }
        
        return true;
    }
    
    public AbstractQuestType createQuestTypeHandler() {
        return switch (type) {
            case COLLECT -> new CollectQuest(this);
            case KILL_MOB -> new KillMobQuest(this);
            case VISIT_LOCATION -> new VisitLocationQuest(this);
            case FISH -> new FishQuest(this);
            case BREAK_BLOCK -> new BreakBlockQuest(this);
            case KILL_PLAYER -> new KillPlayerQuest(this);
            case REACH_ALTITUDE -> new ReachAltitudeQuest(this);
            case FIND_STRUCTURE -> new FindStructureQuest(this);
            case VILLAGER_INTERACT -> new VillagerInteractQuest(this);
            case VILLAGER_TRADE -> new VillagerTradeQuest(this);
            case SURVIVE -> new SurviveQuest(this);
            case TRAVEL -> new TravelQuest(this);
            case JUMP -> new JumpQuest(this);
        };
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getLore() { return lore; }
    public QuestType getType() { return type; }
    public int getTarget() { return target; }
    public Material getDisplayItem() { return displayItem; }
    public QuestReward getReward() { return reward; }
    public String getPermission() { return permission; }
    public long getTimeLimit() { return timeLimit; }
    public boolean isRepeatable() { return repeatable; }
    public int getCooldown() { return cooldown; }
    public ConfigurationSection getTypeConfig() { return typeConfig; }
    
    public boolean hasTimeLimit() {
        return timeLimit > 0;
    }
}
