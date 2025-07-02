package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.quest.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Abstract base class for all quest type handlers
 */
public abstract class AbstractQuestType {
    
    protected final Quest quest;
    
    public AbstractQuestType(Quest quest) {
        this.quest = quest;
    }
    
    /**
     * Checks if the given event should count towards quest progress
     * @param player The player involved in the event
     * @param event The event that occurred
     * @return The amount of progress to add (0 if event doesn't count)
     */
    public abstract int checkProgress(Player player, Event event);
    
    /**
     * Gets a human-readable description of what the player needs to do
     * @return Progress description
     */
    public abstract String getProgressDescription();
    
    /**
     * Called when a player accepts this quest (for setup if needed)
     * @param player The player who accepted the quest
     */
    public void onQuestAccepted(Player player) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a player completes this quest
     * @param player The player who completed the quest
     */
    public void onQuestCompleted(Player player) {
        // Default implementation does nothing
    }
    
    /**
     * Called when this quest expires for a player
     * @param player The player whose quest expired
     */
    public void onQuestExpired(Player player) {
        // Default implementation does nothing
    }
    
    protected Quest getQuest() {
        return quest;
    }
}
