package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.quest.Quest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Quest type for killing players
 */
public class KillPlayerQuest extends AbstractQuestType {
    
    public KillPlayerQuest(Quest quest) {
        super(quest);
    }
    
    @Override
    public int checkProgress(Player player, Event event) {
        if (!(event instanceof EntityDeathEvent deathEvent)) {
            return 0;
        }
        
        if (deathEvent.getEntity().getType() != EntityType.PLAYER) {
            return 0;
        }
        
        if (deathEvent.getEntity().getKiller() != player) {
            return 0;
        }
        
        // Don't allow self-kills
        if (deathEvent.getEntity().equals(player)) {
            return 0;
        }
        
        return 1;
    }
    
    @Override
    public String getProgressDescription() {
        return "Kill " + quest.getTarget() + " players";
    }
}
