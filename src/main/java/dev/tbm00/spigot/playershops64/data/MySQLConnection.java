package dev.tbm00.spigot.playershops64.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.bukkit.plugin.java.JavaPlugin;

public class MySQLConnection {
    private HikariDataSource dataSource;
    private JavaPlugin javaPlugin;
    private HikariConfig config;

    public MySQLConnection(JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
        
        loadSQLConfig();
        setupConnectionPool();
        //clearOldTables();
        initializeDatabase();
    }

    private void loadSQLConfig() {
        config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + javaPlugin.getConfig().getString("mysql.host") + 
                        ":" + javaPlugin.getConfig().getInt("mysql.port") + 
                        "/" + javaPlugin.getConfig().getString("mysql.database") +
                        "?useSSL=" + javaPlugin.getConfig().getBoolean("mysql.useSSL", false));
        config.setUsername(javaPlugin.getConfig().getString("mysql.username"));
        config.setPassword(javaPlugin.getConfig().getString("mysql.password"));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "100");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(javaPlugin.getConfig().getInt("mysql.hikari.maximumPoolSize"));
        config.setMinimumIdle(javaPlugin.getConfig().getInt("mysql.hikari.minimumPoolSize"));
        config.setIdleTimeout(javaPlugin.getConfig().getInt("mysql.hikari.idleTimeout")*1000);
        config.setConnectionTimeout(javaPlugin.getConfig().getInt("mysql.hikari.connectionTimeout")*1000);
        config.setMaxLifetime(javaPlugin.getConfig().getInt("mysql.hikari.maxLifetime")*1000);
        if (javaPlugin.getConfig().getBoolean("mysql.hikari.leakDetection.enabled"))
            config.setLeakDetectionThreshold(javaPlugin.getConfig().getInt("mysql.hikari.leakDetection.threshold")*1000);
    }

    private void setupConnectionPool() {
        dataSource = new HikariDataSource(config);
        javaPlugin.getLogger().info("Initialized Hikari connection pool.");

        try (Connection connection = getConnection()) {
            if (connection.isValid(2))
                javaPlugin.getLogger().info("MySQL database connection is valid!");
        } catch (SQLException e) {
            javaPlugin.getLogger().severe("Failed to establish connection to MySQL database: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed())
            dataSource.close();
    }

    private void initializeDatabase() {
        final String shopTable = "CREATE TABLE IF NOT EXISTS playershops64_shops ("
          + "  `uuid` CHAR(36) NOT NULL,"
          + "  `owner_uuid` CHAR(36) NOT NULL,"
          + "  `owner_name` VARCHAR(32) NOT NULL,"
          + "  `world` VARCHAR(64) NOT NULL,"
          + "  `location` VARCHAR(64) NOT NULL,"
          + "  `itemstack_b64` LONGTEXT NULL,"
          + "  `stack_size` INT NOT NULL DEFAULT 1,"
          + "  `item_stock` INT NOT NULL DEFAULT 0,"
          + "  `money_stock` DECIMAL(18,2) NOT NULL DEFAULT 0.00,"
          + "  `buy_price` DECIMAL(18,2) NULL,"
          + "  `sell_price` DECIMAL(18,2) NULL,"
          + "  `last_tx` DATETIME NULL,"
          + "  PRIMARY KEY (`uuid`),"
          + "  KEY `idx_owner_uuid` (`owner_uuid`),"
          + "  KEY `idx_world` (`world`),"
          + "  KEY `idx_location` (`location`)"
          + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(shopTable);
        } catch (SQLException e) {
            javaPlugin.getLogger().severe("Error initializing database: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private void clearOldTables() {
        String[] oldTables = {
            "playershops64_shops"
        };
    
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
    
            for (String tbl : oldTables) {
                stmt.executeUpdate("DROP TABLE IF EXISTS `" + tbl + "`;");
                javaPlugin.getLogger().info("Dropped old table '" + tbl + "' (if it existed).");
            }
    
        } catch (SQLException e) {
            javaPlugin.getLogger().severe("Failed to drop old tables: " + e.getMessage());
        }
    }
}