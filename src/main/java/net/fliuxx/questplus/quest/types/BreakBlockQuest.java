package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.quest.Quest;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.List;

/**
 * Quest type for breaking blocks
 */
public class BreakBlockQuest extends AbstractQuestType {
    
    private final List<Material> allowedBlocks;
    private final boolean anyBlock;
    
    public BreakBlockQuest(Quest quest) {
        super(quest);
        
        ConfigurationSection config = quest.getTypeConfig();
        if (config != null) {
            List<String> blockNames = config.getStringList("blocks");
            this.allowedBlocks = blockNames.stream()
                    .map(name -> {
                        try {
                            return Material.valueOf(name.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(material -> material != null && material.isBlock())
                    .toList();
            
            this.anyBlock = config.getBoolean("any-block", false);
        } else {
            this.allowedBlocks = List.of();
            this.anyBlock = true;
        }
    }
    
    @Override
    public int checkProgress(Player player, Event event) {
        if (!(event instanceof BlockBreakEvent breakEvent)) {
            return 0;
        }
        
        if (breakEvent.getPlayer() != player) {
            return 0;
        }
        
        Material blockType = breakEvent.getBlock().getType();
        
        // Check if this block type counts
        if (anyBlock || allowedBlocks.isEmpty() || allowedBlocks.contains(blockType)) {
            return 1;
        }
        
        return 0;
    }
    
    @Override
    public String getProgressDescription() {
        if (anyBlock || allowedBlocks.isEmpty()) {
            return "Break " + quest.getTarget() + " blocks";
        } else if (allowedBlocks.size() == 1) {
            return "Break " + quest.getTarget() + " " + allowedBlocks.get(0).name().toLowerCase().replace("_", " ");
        } else {
            return "Break " + quest.getTarget() + " of: " + 
                   allowedBlocks.stream()
                           .map(m -> m.name().toLowerCase().replace("_", " "))
                           .reduce((a, b) -> a + ", " + b)
                           .orElse("blocks");
        }
    }
}
