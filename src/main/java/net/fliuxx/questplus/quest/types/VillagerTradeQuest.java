package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.quest.Quest;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.MerchantInventory;

/**
 * Quest type for trading with villagers
 */
public class VillagerTradeQuest extends AbstractQuestType {
    
    public VillagerTradeQuest(Quest quest) {
        super(quest);
    }
    
    @Override
    public int checkProgress(Player player, Event event) {
        if (!(event instanceof InventoryClickEvent clickEvent)) {
            return 0;
        }
        
        if (clickEvent.getWhoClicked() != player) {
            return 0;
        }
        
        if (!(clickEvent.getInventory() instanceof MerchantInventory merchantInventory)) {
            return 0;
        }
        
        // Check if it's a villager trade
        if (merchantInventory.getMerchant() instanceof Villager) {
            // Check if player is taking the result item (completing a trade)
            if (clickEvent.getSlot() == 2 && clickEvent.getCurrentItem() != null) {
                return 1;
            }
        }
        
        return 0;
    }
    
    @Override
    public String getProgressDescription() {
        return "Complete " + quest.getTarget() + " trades with villagers";
    }
}
