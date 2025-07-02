package net.fliuxx.questplus.quest;

/**
 * Enum representing different types of quests
 */
public enum QuestType {
    COLLECT("Collect Items"),
    KILL_MOB("Kill Mobs"),
    VISIT_LOCATION("Visit Location"),
    FISH("Fish Items"),
    BREAK_BLOCK("Break Blocks"),
    KILL_PLAYER("Kill Players"),
    REACH_ALTITUDE("Reach Altitude"),
    FIND_STRUCTURE("Find Structure"),
    VILLAGER_INTERACT("Interact with Villager"),
    VILLAGER_TRADE("Trade with Villager"),
    SURVIVE("Survive"),
    TRAVEL("Travel Distance"),
    JUMP("Jump");
    
    private final String displayName;
    
    QuestType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
