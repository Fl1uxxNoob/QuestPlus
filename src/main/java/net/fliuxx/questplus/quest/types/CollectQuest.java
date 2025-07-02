package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.quest.Quest;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Quest type for collecting items
 */
public class CollectQuest extends AbstractQuestType {
    
    private final List<Material> allowedMaterials;
    private final boolean anyItem;
    
    public CollectQuest(Quest quest) {
        super(quest);
        
        ConfigurationSection config = quest.getTypeConfig();
        if (config != null) {
            List<String> materialNames = config.getStringList("materials");
            this.allowedMaterials = materialNames.stream()
                    .map(name -> {
                        try {
                            return Material.valueOf(name.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(material -> material != null)
                    .toList();
            
            this.anyItem = config.getBoolean("any-item", false);
        } else {
            this.allowedMaterials = List.of();
            this.anyItem = true;
        }
    }
    
    @Override
    public int checkProgress(Player player, Event event) {
        ItemStack item = null;
        int amount = 0;
        
        if (event instanceof EntityPickupItemEvent pickupEvent) {
            if (pickupEvent.getEntity().equals(player)) {
                item = pickupEvent.getItem().getItemStack();
                amount = item.getAmount();
            }
        } else if (event instanceof CraftItemEvent craftEvent) {
            if (craftEvent.getWhoClicked().equals(player)) {
                item = craftEvent.getCurrentItem();
                amount = item != null ? item.getAmount() : 0;
            }
        } else if (event instanceof FurnaceExtractEvent extractEvent) {
            if (extractEvent.getPlayer().equals(player)) {
                item = new ItemStack(extractEvent.getItemType(), extractEvent.getItemAmount());
                amount = extractEvent.getItemAmount();
            }
        }
        
        if (item == null || amount == 0) {
            return 0;
        }
        
        // Check if this item counts
        if (anyItem || allowedMaterials.isEmpty() || allowedMaterials.contains(item.getType())) {
            return amount;
        }
        
        return 0;
    }
    
    @Override
    public String getProgressDescription() {
        if (anyItem || allowedMaterials.isEmpty()) {
            return "Collect " + quest.getTarget() + " of any items";
        } else if (allowedMaterials.size() == 1) {
            return "Collect " + quest.getTarget() + " " + allowedMaterials.get(0).name().toLowerCase().replace("_", " ");
        } else {
            return "Collect " + quest.getTarget() + " of: " + 
                   allowedMaterials.stream()
                           .map(m -> m.name().toLowerCase().replace("_", " "))
                           .reduce((a, b) -> a + ", " + b)
                           .orElse("items");
        }
    }
}
