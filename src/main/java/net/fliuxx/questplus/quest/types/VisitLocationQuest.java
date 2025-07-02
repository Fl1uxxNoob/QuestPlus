package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.quest.Quest;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

/**
 * Quest type for visiting specific locations or biomes
 */
public class VisitLocationQuest extends AbstractQuestType {
    
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final String targetWorld;
    private final double radius;
    private final List<Biome> targetBiomes;
    private final boolean isBiomeQuest;
    
    public VisitLocationQuest(Quest quest) {
        super(quest);
        
        ConfigurationSection config = quest.getTypeConfig();
        if (config != null) {
            this.targetX = config.getDouble("x", 0);
            this.targetY = config.getDouble("y", -1);
            this.targetZ = config.getDouble("z", 0);
            this.targetWorld = config.getString("world");
            this.radius = config.getDouble("radius", 10.0);
            
            List<String> biomeNames = config.getStringList("biomes");
            this.targetBiomes = biomeNames.stream()
                    .map(name -> {
                        try {
                            return Biome.valueOf(name.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(biome -> biome != null)
                    .toList();
            
            this.isBiomeQuest = !targetBiomes.isEmpty();
        } else {
            this.targetX = 0;
            this.targetY = -1;
            this.targetZ = 0;
            this.targetWorld = null;
            this.radius = 10.0;
            this.targetBiomes = List.of();
            this.isBiomeQuest = false;
        }
    }
    
    @Override
    public int checkProgress(Player player, Event event) {
        if (!(event instanceof PlayerMoveEvent moveEvent)) {
            return 0;
        }
        
        // Only check if player actually moved to a different block
        Location from = moveEvent.getFrom();
        Location to = moveEvent.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX() && 
                          from.getBlockY() == to.getBlockY() && 
                          from.getBlockZ() == to.getBlockZ())) {
            return 0;
        }
        
        if (isBiomeQuest) {
            return checkBiomeVisit(player, to);
        } else {
            return checkLocationVisit(player, to);
        }
    }
    
    private int checkBiomeVisit(Player player, Location location) {
        Biome currentBiome = location.getBlock().getBiome();
        
        if (targetBiomes.contains(currentBiome)) {
            return 1; // Quest completed
        }
        
        return 0;
    }
    
    private int checkLocationVisit(Player player, Location location) {
        // Check world
        if (targetWorld != null && !targetWorld.equals(location.getWorld().getName())) {
            return 0;
        }
        
        // Check coordinates
        double distance = location.distance(new Location(
                location.getWorld(), targetX, targetY == -1 ? location.getY() : targetY, targetZ));
        
        if (distance <= radius) {
            return 1; // Quest completed
        }
        
        return 0;
    }
    
    @Override
    public String getProgressDescription() {
        if (isBiomeQuest) {
            if (targetBiomes.size() == 1) {
                return "Visit the " + targetBiomes.get(0).name().toLowerCase().replace("_", " ") + " biome";
            } else {
                return "Visit one of these biomes: " + 
                       targetBiomes.stream()
                               .map(b -> b.name().toLowerCase().replace("_", " "))
                               .reduce((a, b) -> a + ", " + b)
                               .orElse("biomes");
            }
        } else {
            String worldName = targetWorld != null ? " in " + targetWorld : "";
            if (targetY == -1) {
                return String.format("Visit coordinates %.0f, %.0f%s (within %.0f blocks)", 
                                    targetX, targetZ, worldName, radius);
            } else {
                return String.format("Visit coordinates %.0f, %.0f, %.0f%s (within %.0f blocks)", 
                                    targetX, targetY, targetZ, worldName, radius);
            }
        }
    }
}
