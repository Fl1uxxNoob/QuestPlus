package net.fliuxx.questplus.integration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Integration with LuckPerms for permission group support
 */
public class LuckPermsIntegration {
    
    private LuckPerms luckPerms;
    private boolean enabled;
    
    public boolean initialize() {
        try {
            luckPerms = LuckPermsProvider.get();
            enabled = true;
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to initialize LuckPerms integration", e);
            enabled = false;
            return false;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get the primary group of a player
     * @param playerUuid The player's UUID
     * @return The primary group name, or null if not found
     */
    public String getPrimaryGroup(UUID playerUuid) {
        if (!enabled) return null;
        
        try {
            User user = luckPerms.getUserManager().getUser(playerUuid);
            if (user == null) {
                // Try to load the user
                user = luckPerms.getUserManager().loadUser(playerUuid).join();
            }
            
            if (user != null) {
                return user.getPrimaryGroup();
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error getting primary group for " + playerUuid, e);
        }
        
        return null;
    }
    
    /**
     * Check if a player is in a specific group
     * @param playerUuid The player's UUID
     * @param groupName The group name to check
     * @return true if the player is in the group, false otherwise
     */
    public boolean isInGroup(UUID playerUuid, String groupName) {
        if (!enabled) return false;
        
        try {
            User user = luckPerms.getUserManager().getUser(playerUuid);
            if (user == null) {
                user = luckPerms.getUserManager().loadUser(playerUuid).join();
            }
            
            if (user != null) {
                return user.getInheritedGroups(user.getQueryOptions()).stream()
                        .anyMatch(group -> group.getName().equalsIgnoreCase(groupName));
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error checking group membership for " + playerUuid, e);
        }
        
        return false;
    }
    
    /**
     * Get all groups a player is in
     * @param playerUuid The player's UUID
     * @return Array of group names, or empty array if none found
     */
    public String[] getPlayerGroups(UUID playerUuid) {
        if (!enabled) return new String[0];
        
        try {
            User user = luckPerms.getUserManager().getUser(playerUuid);
            if (user == null) {
                user = luckPerms.getUserManager().loadUser(playerUuid).join();
            }
            
            if (user != null) {
                return user.getInheritedGroups(user.getQueryOptions()).stream()
                        .map(group -> group.getName())
                        .toArray(String[]::new);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error getting player groups for " + playerUuid, e);
        }
        
        return new String[0];
    }
    
    /**
     * Get the highest priority group of a player based on weight
     * @param playerUuid The player's UUID
     * @return The highest priority group name, or null if not found
     */
    public String getHighestPriorityGroup(UUID playerUuid) {
        if (!enabled) return null;
        
        try {
            User user = luckPerms.getUserManager().getUser(playerUuid);
            if (user == null) {
                user = luckPerms.getUserManager().loadUser(playerUuid).join();
            }
            
            if (user != null) {
                return user.getInheritedGroups(user.getQueryOptions()).stream()
                        .max((g1, g2) -> Integer.compare(
                                g1.getWeight().orElse(0),
                                g2.getWeight().orElse(0)
                        ))
                        .map(group -> group.getName())
                        .orElse(null);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error getting highest priority group for " + playerUuid, e);
        }
        
        return null;
    }
}
