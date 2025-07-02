package net.fliuxx.questplus.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a player's progress on a specific quest
 */
public class QuestProgress {
    
    private static final Gson GSON = new GsonBuilder().create();
    
    private final UUID playerUuid;
    private final String questId;
    private final int target;
    private int progress;
    private boolean completed;
    private boolean claimed;
    private long startedAt;
    private Long completedAt;
    private Long expiresAt;
    private Map<String, Object> data;
    
    public QuestProgress(UUID playerUuid, String questId, int target) {
        this.playerUuid = playerUuid;
        this.questId = questId;
        this.target = target;
        this.progress = 0;
        this.completed = false;
        this.claimed = false;
        this.startedAt = System.currentTimeMillis();
        this.data = new HashMap<>();
    }
    
    public void addProgress(int amount) {
        this.progress = Math.min(this.progress + amount, this.target);
    }
    
    public void setProgress(int progress) {
        this.progress = Math.min(progress, this.target);
    }
    
    public boolean isExpired() {
        return expiresAt != null && System.currentTimeMillis() > expiresAt;
    }
    
    public long getTimeRemaining() {
        if (expiresAt == null) return -1;
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }
    
    public double getProgressPercentage() {
        return target > 0 ? (double) progress / target * 100.0 : 0.0;
    }
    
    // Data management methods
    public void setData(String key, Object value) {
        data.put(key, value);
    }
    
    public Object getData(String key) {
        return data.get(key);
    }
    
    public <T> T getData(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        }
        return null;
    }
    
    public String getDataAsJson() {
        return data.isEmpty() ? null : GSON.toJson(data);
    }
    
    public void setDataFromJson(String json) {
        if (json != null && !json.isEmpty()) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            this.data = GSON.fromJson(json, type);
        } else {
            this.data = new HashMap<>();
        }
    }
    
    // Getters and setters
    public UUID getPlayerUuid() { return playerUuid; }
    public String getQuestId() { return questId; }
    public int getTarget() { return target; }
    public int getProgress() { return progress; }
    public boolean isCompleted() { return completed; }
    public boolean isClaimed() { return claimed; }
    public long getStartedAt() { return startedAt; }
    public Long getCompletedAt() { return completedAt; }
    public Long getExpiresAt() { return expiresAt; }
    public Map<String, Object> getAllData() { return new HashMap<>(data); }
    
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setClaimed(boolean claimed) { this.claimed = claimed; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }
}
