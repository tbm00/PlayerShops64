package dev.tbm00.papermc.playershops64.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.bukkit.ChatColor;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class MySQLConnection {
    private HikariDataSource dataSource;
    private PlayerShops64 javaPlugin;
    private HikariConfig config;
    private static String[] TABLES = {
            StaticUtils.TBL_SHOPS
    };
    
    public MySQLConnection(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
        
        loadSQLConfig();
        setupConnectionPool();
        //clearTables();
        initializeDatabase();
        updateTables();
        StaticUtils.log(ChatColor.GREEN, "MySQLConnection initialized.");
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
        StaticUtils.log(ChatColor.YELLOW, "Initialized Hikari connection pool.");

        try (Connection connection = getConnection()) {
            if (connection.isValid(2))
                StaticUtils.log(ChatColor.YELLOW, "MySQL database connection is valid!");
        } catch (SQLException e) {
            StaticUtils.log(ChatColor.RED, "Failed to establish connection to MySQL database: " + e.getMessage());
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
        final String shopTable = "CREATE TABLE IF NOT EXISTS "+StaticUtils.TBL_SHOPS+" ("
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
          + "  `inf_money` TINYINT(1) NOT NULL DEFAULT 0,"
          + "  `inf_stock` TINYINT(1) NOT NULL DEFAULT 0,"
          + "  `description` VARCHAR(256) NULL,"
          + "  `display_height` INT NULL DEFAULT 0,"
          + "  `base_material` VARCHAR(256) NULL,"
          + "  `assistants` LONGTEXT NULL,"
          + "  PRIMARY KEY (`uuid`),"
          + "  KEY `idx_owner_uuid` (`owner_uuid`),"
          + "  KEY `idx_world` (`world`),"
          + "  KEY `idx_location` (`location`)"
          + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(shopTable);
        } catch (SQLException e) {
            StaticUtils.log(ChatColor.RED, "Error initializing database: " + e.getMessage());
        }
    }

    //@SuppressWarnings("unused")
    private void updateTables() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
    
            for (String tbl : TABLES) {
                switch (tbl) {
                    case StaticUtils.TBL_SHOPS: {
                        stmt.executeUpdate(
                                "ALTER TABLE `"+StaticUtils.TBL_SHOPS+"` " +
                                "ADD COLUMN IF NOT EXISTS `description` VARCHAR(256) NULL AFTER `inf_stock`, " +
                                "ADD COLUMN IF NOT EXISTS `display_height` INT NOT NULL DEFAULT 0 AFTER `description`, " +
                                "ADD COLUMN IF NOT EXISTS `base_material` VARCHAR(256) NULL AFTER `display_height`, " +
                                "ADD COLUMN IF NOT EXISTS `assistants` LONGTEXT NULL AFTER `base_material`"
                            );
                        StaticUtils.log(ChatColor.YELLOW, "Updated table '"+tbl+"'.");
                        break;
                    }
                    default:
                        break;
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1060) return;
            else StaticUtils.log(ChatColor.RED, "Failed to update tables: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private void clearTables() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
    
            for (String tbl : TABLES) {
                stmt.executeUpdate("DROP TABLE IF EXISTS `" + tbl + "`;");
                StaticUtils.log(ChatColor.YELLOW, "Dropped table '" + tbl + "' if it existed.");
            }
        } catch (SQLException e) {
            StaticUtils.log(ChatColor.RED, "Failed to drop tables: " + e.getMessage());
        }
    }
}