package net.fliuxx.questplus.database;

import net.fliuxx.questplus.quest.Quest;
import net.fliuxx.questplus.quest.QuestProgress;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Handles all database operations for quests and player progress
 */
public class QuestDatabase {
    
    private final DatabaseManager databaseManager;
    
    public QuestDatabase(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    public void createTables() throws SQLException {
        try (Connection connection = databaseManager.getConnection()) {
            // Players table
            String createPlayersTable = """
                CREATE TABLE IF NOT EXISTS quest_players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    quests_completed INTEGER DEFAULT 0,
                    quests_active INTEGER DEFAULT 0,
                    last_seen BIGINT NOT NULL,
                    created_at BIGINT NOT NULL
                )""";
            
            // Quest progress table
            String createProgressTable = """
                CREATE TABLE IF NOT EXISTS quest_progress (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid VARCHAR(36) NOT NULL,
                    quest_id VARCHAR(100) NOT NULL,
                    progress INTEGER DEFAULT 0,
                    target INTEGER NOT NULL,
                    completed BOOLEAN DEFAULT FALSE,
                    claimed BOOLEAN DEFAULT FALSE,
                    started_at BIGINT NOT NULL,
                    completed_at BIGINT DEFAULT NULL,
                    expires_at BIGINT DEFAULT NULL,
                    data TEXT DEFAULT NULL,
                    CONSTRAINT unique_quest_progress UNIQUE (player_uuid, quest_id),
                    FOREIGN KEY (player_uuid) REFERENCES quest_players(uuid) ON DELETE CASCADE
                )
                """;
            
            // Quest statistics table
            String createStatsTable = """
                CREATE TABLE IF NOT EXISTS quest_statistics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid VARCHAR(36) NOT NULL,
                    quest_id VARCHAR(100) NOT NULL,
                    completions INTEGER DEFAULT 1,
                    best_time BIGINT DEFAULT NULL,
                    first_completion BIGINT NOT NULL,
                    last_completion BIGINT NOT NULL,
                    CONSTRAINT unique_quest_stats UNIQUE (player_uuid, quest_id),
                    FOREIGN KEY (player_uuid) REFERENCES quest_players(uuid) ON DELETE CASCADE
                )
                """;
            
            // Adjust for SQLite
            if (databaseManager.getConnection().getMetaData().getDriverName().contains("SQLite")) {
                createProgressTable = createProgressTable.replace("AUTO_INCREMENT", "AUTOINCREMENT");
                createStatsTable = createStatsTable.replace("AUTO_INCREMENT", "AUTOINCREMENT");
                createProgressTable = createProgressTable.replace("UNIQUE KEY", "CONSTRAINT");
                createStatsTable = createStatsTable.replace("UNIQUE KEY", "CONSTRAINT");
            }
            
            try (Statement statement = connection.createStatement()) {
                statement.execute(createPlayersTable);
                statement.execute(createProgressTable);
                statement.execute(createStatsTable);
            }
        }
    }
    
    // Player operations
    public void createOrUpdatePlayer(UUID playerUuid, String username) {
        String sql = """
            INSERT INTO quest_players (uuid, username, last_seen, created_at) 
            VALUES (?, ?, ?, ?) 
            ON DUPLICATE KEY UPDATE username = ?, last_seen = ?""";
            
        // SQLite version
        String sqliteSql = """
            INSERT OR REPLACE INTO quest_players (uuid, username, last_seen, created_at) 
            VALUES (?, ?, ?, COALESCE((SELECT created_at FROM quest_players WHERE uuid = ?), ?))""";
        
        try (Connection connection = databaseManager.getConnection()) {
            boolean isSQLite = connection.getMetaData().getDriverName().contains("SQLite");
            long currentTime = System.currentTimeMillis();
            
            try (PreparedStatement statement = connection.prepareStatement(isSQLite ? sqliteSql : sql)) {
                if (isSQLite) {
                    statement.setString(1, playerUuid.toString());
                    statement.setString(2, username);
                    statement.setLong(3, currentTime);
                    statement.setLong(4, currentTime);
                    statement.setString(5, playerUuid.toString());
                    statement.setLong(6, currentTime);
                } else {
                    statement.setString(1, playerUuid.toString());
                    statement.setString(2, username);
                    statement.setLong(3, currentTime);
                    statement.setLong(4, currentTime);
                    statement.setString(5, username);
                    statement.setLong(6, currentTime);
                }
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to create/update player: " + playerUuid, e);
        }
    }
    
    // Quest progress operations
    public void saveQuestProgress(QuestProgress progress) {
        String sql = """
            INSERT INTO quest_progress (player_uuid, quest_id, progress, target, completed, claimed, 
                                      started_at, completed_at, expires_at, data) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
            ON DUPLICATE KEY UPDATE 
                progress = ?, completed = ?, claimed = ?, completed_at = ?, data = ?""";
        
        String sqliteSql = """
            INSERT OR REPLACE INTO quest_progress (player_uuid, quest_id, progress, target, completed, claimed, 
                                                 started_at, completed_at, expires_at, data) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        
        try (Connection connection = databaseManager.getConnection()) {
            boolean isSQLite = connection.getMetaData().getDriverName().contains("SQLite");
            
            try (PreparedStatement statement = connection.prepareStatement(isSQLite ? sqliteSql : sql)) {
                statement.setString(1, progress.getPlayerUuid().toString());
                statement.setString(2, progress.getQuestId());
                statement.setInt(3, progress.getProgress());
                statement.setInt(4, progress.getTarget());
                statement.setBoolean(5, progress.isCompleted());
                statement.setBoolean(6, progress.isClaimed());
                statement.setLong(7, progress.getStartedAt());
                statement.setObject(8, progress.getCompletedAt());
                statement.setObject(9, progress.getExpiresAt());
                statement.setString(10, progress.getDataAsJson());
                
                if (!isSQLite) {
                    statement.setInt(11, progress.getProgress());
                    statement.setBoolean(12, progress.isCompleted());
                    statement.setBoolean(13, progress.isClaimed());
                    statement.setObject(14, progress.getCompletedAt());
                    statement.setString(15, progress.getDataAsJson());
                }
                
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save quest progress: " + progress.getQuestId() + " for " + progress.getPlayerUuid(), e);
        }
    }
    
    public List<QuestProgress> getPlayerProgress(UUID playerUuid) {
        List<QuestProgress> progressList = new ArrayList<>();
        String sql = "SELECT * FROM quest_progress WHERE player_uuid = ?";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, playerUuid.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    QuestProgress progress = new QuestProgress(
                        UUID.fromString(resultSet.getString("player_uuid")),
                        resultSet.getString("quest_id"),
                        resultSet.getInt("target")
                    );
                    
                    progress.setProgress(resultSet.getInt("progress"));
                    progress.setCompleted(resultSet.getBoolean("completed"));
                    progress.setClaimed(resultSet.getBoolean("claimed"));
                    progress.setStartedAt(resultSet.getLong("started_at"));
                    
                    Long completedAt = resultSet.getObject("completed_at", Long.class);
                    if (completedAt != null) {
                        progress.setCompletedAt(completedAt);
                    }
                    
                    Long expiresAt = resultSet.getObject("expires_at", Long.class);
                    if (expiresAt != null) {
                        progress.setExpiresAt(expiresAt);
                    }
                    
                    String data = resultSet.getString("data");
                    if (data != null) {
                        progress.setDataFromJson(data);
                    }
                    
                    progressList.add(progress);
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to load player progress: " + playerUuid, e);
        }
        
        return progressList;
    }
    
    public QuestProgress getQuestProgress(UUID playerUuid, String questId) {
        String sql = "SELECT * FROM quest_progress WHERE player_uuid = ? AND quest_id = ?";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, playerUuid.toString());
            statement.setString(2, questId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    QuestProgress progress = new QuestProgress(
                        UUID.fromString(resultSet.getString("player_uuid")),
                        resultSet.getString("quest_id"),
                        resultSet.getInt("target")
                    );
                    
                    progress.setProgress(resultSet.getInt("progress"));
                    progress.setCompleted(resultSet.getBoolean("completed"));
                    progress.setClaimed(resultSet.getBoolean("claimed"));
                    progress.setStartedAt(resultSet.getLong("started_at"));
                    
                    Long completedAt = resultSet.getObject("completed_at", Long.class);
                    if (completedAt != null) {
                        progress.setCompletedAt(completedAt);
                    }
                    
                    Long expiresAt = resultSet.getObject("expires_at", Long.class);
                    if (expiresAt != null) {
                        progress.setExpiresAt(expiresAt);
                    }
                    
                    String data = resultSet.getString("data");
                    if (data != null) {
                        progress.setDataFromJson(data);
                    }
                    
                    return progress;
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to load quest progress: " + questId + " for " + playerUuid, e);
        }
        
        return null;
    }
    
    public void deleteQuestProgress(UUID playerUuid, String questId) {
        String sql = "DELETE FROM quest_progress WHERE player_uuid = ? AND quest_id = ?";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, playerUuid.toString());
            statement.setString(2, questId);
            statement.executeUpdate();
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to delete quest progress: " + questId + " for " + playerUuid, e);
        }
    }
    
    // Statistics operations
    public void saveQuestStatistics(UUID playerUuid, String questId, long completionTime) {
        String sql = """
            INSERT INTO quest_statistics (player_uuid, quest_id, completions, best_time, first_completion, last_completion) 
            VALUES (?, ?, 1, ?, ?, ?) 
            ON DUPLICATE KEY UPDATE 
                completions = completions + 1, 
                best_time = CASE WHEN best_time IS NULL OR ? < best_time THEN ? ELSE best_time END,
                last_completion = ?""";
        
        String sqliteSql = """
            INSERT OR REPLACE INTO quest_statistics (player_uuid, quest_id, completions, best_time, first_completion, last_completion) 
            VALUES (?, ?, 
                    COALESCE((SELECT completions FROM quest_statistics WHERE player_uuid = ? AND quest_id = ?), 0) + 1,
                    CASE WHEN (SELECT best_time FROM quest_statistics WHERE player_uuid = ? AND quest_id = ?) IS NULL 
                         OR ? < (SELECT best_time FROM quest_statistics WHERE player_uuid = ? AND quest_id = ?) 
                    THEN ? ELSE (SELECT best_time FROM quest_statistics WHERE player_uuid = ? AND quest_id = ?) END,
                    COALESCE((SELECT first_completion FROM quest_statistics WHERE player_uuid = ? AND quest_id = ?), ?),
                    ?)""";
        
        try (Connection connection = databaseManager.getConnection()) {
            boolean isSQLite = connection.getMetaData().getDriverName().contains("SQLite");
            long currentTime = System.currentTimeMillis();
            
            try (PreparedStatement statement = connection.prepareStatement(isSQLite ? sqliteSql : sql)) {
                if (isSQLite) {
                    statement.setString(1, playerUuid.toString());
                    statement.setString(2, questId);
                    statement.setString(3, playerUuid.toString());
                    statement.setString(4, questId);
                    statement.setString(5, playerUuid.toString());
                    statement.setString(6, questId);
                    statement.setLong(7, completionTime);
                    statement.setString(8, playerUuid.toString());
                    statement.setString(9, questId);
                    statement.setLong(10, completionTime);
                    statement.setString(11, playerUuid.toString());
                    statement.setString(12, questId);
                    statement.setString(13, playerUuid.toString());
                    statement.setString(14, questId);
                    statement.setLong(15, currentTime);
                    statement.setLong(16, currentTime);
                } else {
                    statement.setString(1, playerUuid.toString());
                    statement.setString(2, questId);
                    statement.setLong(3, completionTime);
                    statement.setLong(4, currentTime);
                    statement.setLong(5, currentTime);
                    statement.setLong(6, completionTime);
                    statement.setLong(7, completionTime);
                    statement.setLong(8, currentTime);
                }
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save quest statistics: " + questId + " for " + playerUuid, e);
        }
    }
    
    public int getActiveQuestCount(UUID playerUuid) {
        String sql = "SELECT COUNT(*) FROM quest_progress WHERE player_uuid = ? AND completed = FALSE";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, playerUuid.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to get active quest count for " + playerUuid, e);
        }
        
        return 0;
    }
    
    public int getCompletedQuestCount(UUID playerUuid) {
        String sql = "SELECT COALESCE(SUM(completions), 0) FROM quest_statistics WHERE player_uuid = ?";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, playerUuid.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to get completed quest count for " + playerUuid, e);
        }
        
        return 0;
    }
}
