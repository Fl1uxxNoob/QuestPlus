package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.quest.Quest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Quest type for interacting with villagers
 */
public class VillagerInteractQuest extends AbstractQuestType {
    
    public VillagerInteractQuest(Quest quest) {
        super(quest);
    }
    
    @Override
    public int checkProgress(Player player, Event event) {
        if (!(event instanceof PlayerInteractEntityEvent interactEvent)) {
            return 0;
        }
        
        if (interactEvent.getPlayer() != player) {
            return 0;
        }
        
        if (interactEvent.getRightClicked().getType() == EntityType.VILLAGER) {
            return 1;
        }
        
        return 0;
    }
    
    @Override
    public String getProgressDescription() {
        return "Interact with " + quest.getTarget() + " villagers";
    }
}
