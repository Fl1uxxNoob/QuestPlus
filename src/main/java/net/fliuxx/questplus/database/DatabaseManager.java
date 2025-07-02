package net.fliuxx.questplus.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.fliuxx.questplus.QuestPlus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Manages database connections and initialization
 */
public class DatabaseManager {
    
    private final QuestPlus plugin;
    private HikariDataSource dataSource;
    private QuestDatabase questDatabase;
    
    public DatabaseManager(QuestPlus plugin) {
        this.plugin = plugin;
    }
    
    public void initialize() throws SQLException {
        String databaseType = plugin.getConfigManager().getDatabaseType().toLowerCase();
        
        HikariConfig config = new HikariConfig();
        
        switch (databaseType) {
            case "mysql" -> setupMySQL(config);
            case "sqlite" -> setupSQLite(config);
            default -> {
                plugin.getLogger().warning("Unknown database type: " + databaseType + ". Using SQLite.");
                setupSQLite(config);
            }
        }
        
        // Common settings
        config.setMaximumPoolSize(plugin.getConfigManager().getConnectionPoolSize());
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        dataSource = new HikariDataSource(config);
        
        // Initialize quest database
        questDatabase = new QuestDatabase(this);
        questDatabase.createTables();
        
        plugin.getLogger().info("Database connection established using " + databaseType.toUpperCase());
    }
    
    private void setupMySQL(HikariConfig config) {
        config.setJdbcUrl("jdbc:mysql://" + 
            plugin.getConfigManager().getDatabaseHost() + ":" + 
            plugin.getConfigManager().getDatabasePort() + "/" + 
            plugin.getConfigManager().getDatabaseName() + 
            "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        config.setUsername(plugin.getConfigManager().getDatabaseUsername());
        config.setPassword(plugin.getConfigManager().getDatabasePassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // MySQL specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
    }
    
    private void setupSQLite(HikariConfig config) {
        String dbPath = plugin.getDataFolder().getAbsolutePath() + "/questplus.db";
        config.setJdbcUrl("jdbc:sqlite:" + dbPath);
        config.setDriverClassName("org.sqlite.JDBC");
        
        // SQLite specific settings
        config.addDataSourceProperty("foreign_keys", "true");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.setMaximumPoolSize(1); // SQLite doesn't support multiple writers
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public QuestDatabase getQuestDatabase() {
        return questDatabase;
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection closed.");
        }
    }
    
    public boolean isConnected() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Database connection test failed", e);
            return false;
        }
    }
}
