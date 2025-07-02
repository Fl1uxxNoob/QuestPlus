package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.quest.Quest;
import net.fliuxx.questplus.quest.QuestProgress;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Quest type for traveling a certain distance
 */
public class TravelQuest extends AbstractQuestType {
    
    public enum TravelType {
        WALK, SWIM, FLY, ANY
    }
    
    private final TravelType travelType;
    private final double targetDistance;
    
    public TravelQuest(Quest quest) {
        super(quest);
        
        ConfigurationSection config = quest.getTypeConfig();
        TravelType tempTravelType;
        double tempDistance;
        
        if (config != null) {
            String typeString = config.getString("type", "ANY").toUpperCase();
            try {
                tempTravelType = TravelType.valueOf(typeString);
            } catch (IllegalArgumentException e) {
                tempTravelType = TravelType.ANY;
            }
            tempDistance = config.getDouble("distance", quest.getTarget());
        } else {
            tempTravelType = TravelType.ANY;
            tempDistance = quest.getTarget();
        }
        
        this.travelType = tempTravelType;
        this.targetDistance = tempDistance;
    }
    
    @Override
    public int checkProgress(Player player, Event event) {
        if (!(event instanceof PlayerMoveEvent moveEvent)) {
            return 0;
        }
        
        Location from = moveEvent.getFrom();
        Location to = moveEvent.getTo();
        if (to == null) {
            return 0;
        }
        
        // Calculate distance moved
        double distance = from.distance(to);
        
        // Check travel type
        if (!isValidTravelType(player, from, to)) {
            return 0;
        }
        
        // Return distance as progress (will be accumulated)
        return (int) (distance * 100); // Convert to centimeters for precision
    }
    
    private boolean isValidTravelType(Player player, Location from, Location to) {
        return switch (travelType) {
            case WALK -> !player.isSwimming() && !player.isGliding() && !player.isFlying();
            case SWIM -> player.isSwimming();
            case FLY -> player.isGliding() || player.isFlying();
            case ANY -> true;
        };
    }
    
    @Override
    public String getProgressDescription() {
        String typeDesc = switch (travelType) {
            case WALK -> "Walk";
            case SWIM -> "Swim";
            case FLY -> "Fly";
            case ANY -> "Travel";
        };
        
        return typeDesc + " " + (int) targetDistance + " blocks";
    }
}
