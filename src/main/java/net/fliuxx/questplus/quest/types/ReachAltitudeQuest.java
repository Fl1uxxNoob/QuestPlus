package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.quest.Quest;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Quest type for reaching a specific altitude
 */
public class ReachAltitudeQuest extends AbstractQuestType {
    
    private final int targetAltitude;
    
    public ReachAltitudeQuest(Quest quest) {
        super(quest);
        
        ConfigurationSection config = quest.getTypeConfig();
        if (config != null) {
            this.targetAltitude = config.getInt("altitude", 200);
        } else {
            this.targetAltitude = 200;
        }
    }
    
    @Override
    public int checkProgress(Player player, Event event) {
        if (!(event instanceof PlayerMoveEvent moveEvent)) {
            return 0;
        }
        
        Location to = moveEvent.getTo();
        if (to == null) {
            return 0;
        }
        
        if (to.getY() >= targetAltitude) {
            return 1; // Quest completed
        }
        
        return 0;
    }
    
    @Override
    public String getProgressDescription() {
        return "Reach altitude Y=" + targetAltitude;
    }
}
