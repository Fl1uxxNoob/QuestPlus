package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.quest.Quest;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.util.StructureSearchResult;

import java.util.List;

/**
 * Quest type for finding natural structures
 */
public class FindStructureQuest extends AbstractQuestType {
    
    private final List<StructureType> targetStructures;
    private final int searchRadius;
    
    public FindStructureQuest(Quest quest) {
        super(quest);
        
        ConfigurationSection config = quest.getTypeConfig();
        if (config != null) {
            List<String> structureNames = config.getStringList("structures");
            this.targetStructures = structureNames.stream()
                    .map(name -> {
                        try {
                            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
                            return Registry.STRUCTURE_TYPE.get(key);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(structure -> structure != null)
                    .toList();
            
            this.searchRadius = config.getInt("search-radius", 100);
        } else {
            this.targetStructures = List.of();
            this.searchRadius = 100;
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
        
        // Only check if player moved to a different chunk to avoid performance issues
        Location from = moveEvent.getFrom();
        if (from.getChunk().equals(to.getChunk())) {
            return 0;
        }
        
        // Check for structures near the player
        for (StructureType structureType : targetStructures) {
            StructureSearchResult searchResult = to.getWorld().locateNearestStructure(to, structureType, searchRadius, false);
            if (searchResult != null) {
                Location structureLocation = searchResult.getLocation();
                if (structureLocation != null && structureLocation.distance(to) <= 50) { // Close enough to the structure
                    return 1; // Quest completed
                }
            }
        }
        
        return 0;
    }
    
    @Override
    public String getProgressDescription() {
        if (targetStructures.size() == 1) {
            return "Find a " + targetStructures.get(0).getKey().getKey().replace("_", " ");
        } else {
            return "Find one of these structures: " + 
                   targetStructures.stream()
                           .map(s -> s.getKey().getKey().replace("_", " "))
                           .reduce((a, b) -> a + ", " + b)
                           .orElse("structures");
        }
    }
}
