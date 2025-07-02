package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.quest.Quest;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Quest type for fishing specific items
 */
public class FishQuest extends AbstractQuestType {
    
    private final List<Material> allowedItems;
    private final boolean anyFish;
    
    public FishQuest(Quest quest) {
        super(quest);
        
        ConfigurationSection config = quest.getTypeConfig();
        if (config != null) {
            List<String> itemNames = config.getStringList("items");
            this.allowedItems = itemNames.stream()
                    .map(name -> {
                        try {
                            return Material.valueOf(name.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(material -> material != null)
                    .toList();
            
            this.anyFish = config.getBoolean("any-fish", false);
        } else {
            this.allowedItems = List.of();
            this.anyFish = true;
        }
    }
    
    @Override
    public int checkProgress(Player player, Event event) {
        if (!(event instanceof PlayerFishEvent fishEvent)) {
            return 0;
        }
        
        if (fishEvent.getPlayer() != player || fishEvent.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return 0;
        }
        
        if (fishEvent.getCaught() instanceof org.bukkit.entity.Item item) {
            ItemStack caught = item.getItemStack();
            
            // Check if this item counts
            if (anyFish || allowedItems.isEmpty() || allowedItems.contains(caught.getType())) {
                return caught.getAmount();
            }
        }
        
        return 0;
    }
    
    @Override
    public String getProgressDescription() {
        if (anyFish || allowedItems.isEmpty()) {
            return "Fish " + quest.getTarget() + " items";
        } else if (allowedItems.size() == 1) {
            return "Fish " + quest.getTarget() + " " + allowedItems.get(0).name().toLowerCase().replace("_", " ");
        } else {
            return "Fish " + quest.getTarget() + " of: " + 
                   allowedItems.stream()
                           .map(m -> m.name().toLowerCase().replace("_", " "))
                           .reduce((a, b) -> a + ", " + b)
                           .orElse("items");
        }
    }
}
