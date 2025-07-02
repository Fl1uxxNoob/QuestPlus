package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.quest.Quest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;

/**
 * Quest type for killing mobs
 */
public class KillMobQuest extends AbstractQuestType {
    
    private final List<EntityType> allowedMobs;
    private final boolean anyMob;
    
    public KillMobQuest(Quest quest) {
        super(quest);
        
        ConfigurationSection config = quest.getTypeConfig();
        if (config != null) {
            List<String> mobNames = config.getStringList("mobs");
            this.allowedMobs = mobNames.stream()
                    .map(name -> {
                        try {
                            return EntityType.valueOf(name.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(type -> type != null)
                    .toList();
            
            this.anyMob = config.getBoolean("any-mob", false);
        } else {
            this.allowedMobs = List.of();
            this.anyMob = true;
        }
    }
    
    @Override
    public int checkProgress(Player player, Event event) {
        if (!(event instanceof EntityDeathEvent deathEvent)) {
            return 0;
        }
        
        if (deathEvent.getEntity().getKiller() != player) {
            return 0;
        }
        
        EntityType entityType = deathEvent.getEntity().getType();
        
        // Don't count player kills for mob quests
        if (entityType == EntityType.PLAYER) {
            return 0;
        }
        
        // Check if this mob type counts
        if (anyMob || allowedMobs.isEmpty() || allowedMobs.contains(entityType)) {
            return 1;
        }
        
        return 0;
    }
    
    @Override
    public String getProgressDescription() {
        if (anyMob || allowedMobs.isEmpty()) {
            return "Kill " + quest.getTarget() + " mobs";
        } else if (allowedMobs.size() == 1) {
            return "Kill " + quest.getTarget() + " " + allowedMobs.get(0).name().toLowerCase().replace("_", " ");
        } else {
            return "Kill " + quest.getTarget() + " of: " + 
                   allowedMobs.stream()
                           .map(m -> m.name().toLowerCase().replace("_", " "))
                           .reduce((a, b) -> a + ", " + b)
                           .orElse("mobs");
        }
    }
}
