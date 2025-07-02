package net.fliuxx.questplus.quest.types;

import net.fliuxx.questplus.QuestPlus;
import net.fliuxx.questplus.quest.Quest;
import net.fliuxx.questplus.quest.QuestProgress;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Quest type for surviving for a certain time under specific conditions
 */
public class SurviveQuest extends AbstractQuestType {
    
    private final int survivalTimeSeconds;
    private final double minHealth;
    private final int minFoodLevel;
    private final List<Biome> requiredBiomes;
    private final Location requiredLocation;
    private final double locationRadius;
    
    public SurviveQuest(Quest quest) {
        super(quest);
        
        ConfigurationSection config = quest.getTypeConfig();
        if (config != null) {
            this.survivalTimeSeconds = config.getInt("time-seconds", 300); // 5 minutes default
            this.minHealth = config.getDouble("min-health", 1.0);
            this.minFoodLevel = config.getInt("min-food-level", 0);
            
            List<String> biomeNames = config.getStringList("biomes");
            this.requiredBiomes = biomeNames.stream()
                    .map(name -> {
                        try {
                            return Biome.valueOf(name.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(biome -> biome != null)
                    .toList();
            
            if (config.contains("location")) {
                ConfigurationSection locConfig = config.getConfigurationSection("location");
                String worldName = locConfig.getString("world");
                double x = locConfig.getDouble("x");
                double y = locConfig.getDouble("y");
                double z = locConfig.getDouble("z");
                this.locationRadius = locConfig.getDouble("radius", 50.0);
                this.requiredLocation = new Location(Bukkit.getWorld(worldName), x, y, z);
            } else {
                this.requiredLocation = null;
                this.locationRadius = 0;
            }
        } else {
            this.survivalTimeSeconds = 300;
            this.minHealth = 1.0;
            this.minFoodLevel = 0;
            this.requiredBiomes = List.of();
            this.requiredLocation = null;
            this.locationRadius = 0;
        }
    }
    
    @Override
    public int checkProgress(Player player, Event event) {
        // This quest uses a timer-based approach, not event-based
        return 0;
    }
    
    @Override
    public void onQuestAccepted(Player player) {
        startSurvivalTimer(player);
    }
    
    private void startSurvivalTimer(Player player) {
        new BukkitRunnable() {
            int secondsPassed = 0;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                QuestProgress progress = QuestPlus.getInstance().getQuestManager()
                        .getPlayerProgress(player).stream()
                        .filter(p -> p.getQuestId().equals(quest.getId()) && !p.isCompleted())
                        .findFirst()
                        .orElse(null);
                
                if (progress == null) {
                    this.cancel();
                    return;
                }
                
                // Check survival conditions
                if (!checkSurvivalConditions(player)) {
                    // Player failed to meet survival conditions, reset timer
                    secondsPassed = 0;
                    progress.setData("seconds_survived", 0);
                    return;
                }
                
                secondsPassed++;
                progress.setData("seconds_survived", secondsPassed);
                
                // Update progress (progress is based on time)
                int newProgress = (secondsPassed * quest.getTarget()) / survivalTimeSeconds;
                progress.setProgress(Math.min(newProgress, quest.getTarget()));
                
                if (secondsPassed >= survivalTimeSeconds) {
                    progress.setProgress(quest.getTarget());
                    QuestPlus.getInstance().getQuestManager().updateQuestProgress(player, quest.getId(), 0);
                    this.cancel();
                }
            }
        }.runTaskTimer(QuestPlus.getInstance(), 20L, 20L); // Run every second
    }
    
    private boolean checkSurvivalConditions(Player player) {
        // Check health
        if (player.getHealth() < minHealth) {
            return false;
        }
        
        // Check food level
        if (player.getFoodLevel() < minFoodLevel) {
            return false;
        }
        
        // Check biome
        if (!requiredBiomes.isEmpty()) {
            Biome currentBiome = player.getLocation().getBlock().getBiome();
            if (!requiredBiomes.contains(currentBiome)) {
                return false;
            }
        }
        
        // Check location
        if (requiredLocation != null) {
            if (!player.getWorld().equals(requiredLocation.getWorld())) {
                return false;
            }
            
            double distance = player.getLocation().distance(requiredLocation);
            if (distance > locationRadius) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String getProgressDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("Survive for ").append(survivalTimeSeconds).append(" seconds");
        
        if (minHealth > 1.0) {
            desc.append(" with at least ").append(minHealth).append(" health");
        }
        
        if (minFoodLevel > 0) {
            desc.append(" with at least ").append(minFoodLevel).append(" food level");
        }
        
        if (!requiredBiomes.isEmpty()) {
            desc.append(" in ").append(requiredBiomes.stream()
                    .map(b -> b.name().toLowerCase().replace("_", " "))
                    .reduce((a, b) -> a + " or " + b)
                    .orElse(""));
        }
        
        if (requiredLocation != null) {
            desc.append(" near ").append(String.format("%.0f, %.0f, %.0f", 
                    requiredLocation.getX(), requiredLocation.getY(), requiredLocation.getZ()));
        }
        
        return desc.toString();
    }
}
