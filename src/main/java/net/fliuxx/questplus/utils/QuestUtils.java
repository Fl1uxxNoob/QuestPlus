package net.fliuxx.questplus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Utility methods for quest-related operations
 */
public class QuestUtils {
    
    /**
     * Format time in seconds to a human-readable string
     * @param seconds Time in seconds
     * @return Formatted time string
     */
    public static String formatTime(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        StringBuilder timeString = new StringBuilder();
        
        if (days > 0) {
            timeString.append(days).append("d ");
        }
        if (hours > 0) {
            timeString.append(hours).append("h ");
        }
        if (minutes > 0) {
            timeString.append(minutes).append("m ");
        }
        if (secs > 0 || timeString.length() == 0) {
            timeString.append(secs).append("s");
        }
        
        return timeString.toString().trim();
    }
    
    /**
     * Format time in milliseconds to a human-readable string
     * @param milliseconds Time in milliseconds
     * @return Formatted time string
     */
    public static String formatTimeMillis(long milliseconds) {
        return formatTime(milliseconds / 1000);
    }
    
    /**
     * Create a progress bar string
     * @param current Current progress
     * @param max Maximum progress
     * @param length Length of the progress bar
     * @return Progress bar string
     */
    public static String createProgressBar(int current, int max, int length) {
        if (max <= 0) return "█".repeat(length);
        
        int filled = (int) ((double) current / max * length);
        filled = Math.min(filled, length);
        
        String filledBar = "█".repeat(filled);
        String emptyBar = "░".repeat(length - filled);
        
        return filledBar + emptyBar;
    }
    
    /**
     * Create a colored progress bar component
     * @param current Current progress
     * @param max Maximum progress
     * @param length Length of the progress bar
     * @return Progress bar component
     */
    public static Component createColoredProgressBar(int current, int max, int length) {
        if (max <= 0) {
            return Component.text("█".repeat(length)).color(NamedTextColor.GREEN);
        }
        
        int filled = (int) ((double) current / max * length);
        filled = Math.min(filled, length);
        
        Component filledPart = Component.text("█".repeat(filled)).color(NamedTextColor.GREEN);
        Component emptyPart = Component.text("░".repeat(length - filled)).color(NamedTextColor.GRAY);
        
        return filledPart.append(emptyPart);
    }
    
    /**
     * Get progress color based on percentage
     * @param percentage Progress percentage (0-100)
     * @return Text color
     */
    public static TextColor getProgressColor(double percentage) {
        if (percentage >= 100.0) {
            return NamedTextColor.GREEN;
        } else if (percentage >= 75.0) {
            return NamedTextColor.YELLOW;
        } else if (percentage >= 50.0) {
            return NamedTextColor.GOLD;
        } else if (percentage >= 25.0) {
            return NamedTextColor.RED;
        } else {
            return NamedTextColor.DARK_RED;
        }
    }
    
    /**
     * Format a number with appropriate suffixes (K, M, B)
     * @param number The number to format
     * @return Formatted number string
     */
    public static String formatNumber(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 1000000) {
            return String.format("%.1fK", number / 1000.0);
        } else if (number < 1000000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else {
            return String.format("%.1fB", number / 1000000000.0);
        }
    }
    
    /**
     * Convert seconds to ticks (20 ticks per second)
     * @param seconds Seconds
     * @return Ticks
     */
    public static long secondsToTicks(long seconds) {
        return seconds * 20L;
    }
    
    /**
     * Convert ticks to seconds (20 ticks per second)
     * @param ticks Ticks
     * @return Seconds
     */
    public static long ticksToSeconds(long ticks) {
        return ticks / 20L;
    }
    
    /**
     * Check if a string represents a valid integer
     * @param str The string to check
     * @return true if valid integer, false otherwise
     */
    public static boolean isValidInteger(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Check if a string represents a valid long
     * @param str The string to check
     * @return true if valid long, false otherwise
     */
    public static boolean isValidLong(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Safely parse an integer with a default value
     * @param str The string to parse
     * @param defaultValue Default value if parsing fails
     * @return Parsed integer or default value
     */
    public static int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Safely parse a long with a default value
     * @param str The string to parse
     * @param defaultValue Default value if parsing fails
     * @return Parsed long or default value
     */
    public static long parseLong(String str, long defaultValue) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Truncate a string to a maximum length with ellipsis
     * @param str The string to truncate
     * @param maxLength Maximum length
     * @return Truncated string
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        
        return str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Calculate percentage with proper rounding
     * @param current Current value
     * @param max Maximum value
     * @return Percentage (0-100)
     */
    public static double calculatePercentage(int current, int max) {
        if (max <= 0) return 0.0;
        return Math.min(100.0, (double) current / max * 100.0);
    }
}
