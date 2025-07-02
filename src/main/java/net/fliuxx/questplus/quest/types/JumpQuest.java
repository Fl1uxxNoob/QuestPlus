package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.quest.Quest;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Quest type for jumping a certain number of times
 */
public class JumpQuest extends AbstractQuestType {
    
    public JumpQuest(Quest quest) {
        super(quest);
    }
    
    @Override
    public int checkProgress(Player player, Event event) {
        if (event instanceof PlayerMoveEvent moveEvent) {
            return checkJumpFromMovement(player, moveEvent);
        }
        
        return 0;
    }
    
    private int checkJumpFromMovement(Player player, PlayerMoveEvent event) {
        // Check if player jumped (Y velocity increased while on ground)
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return 0;
        }
        
        // Simple jump detection: Y coordinate increased and player was on ground
        if (to.getY() > from.getY() && player.isOnGround()) {
            // Additional check: make sure it's not just walking up stairs/blocks
            double yDiff = to.getY() - from.getY();
            if (yDiff > 0.1 && yDiff < 2.0) { // Reasonable jump height
                return 1;
            }
        }
        
        return 0;
    }
    
    @Override
    public String getProgressDescription() {
        return "Jump " + quest.getTarget() + " times";
    }
}
