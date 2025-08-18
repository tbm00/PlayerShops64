package dev.tbm00.spigot.playershops64;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.playershops64.utils.*;
import dev.tbm00.spigot.playershops64.command.*;
import dev.tbm00.spigot.playershops64.data.ConfigHandler;
import dev.tbm00.spigot.playershops64.data.MySQLConnection;
import dev.tbm00.spigot.playershops64.hook.VaultHook;
import dev.tbm00.spigot.playershops64.listener.PlayerMovement;

public class PlayerShops64 extends JavaPlugin {
    private ConfigHandler configHandler;
    private MySQLConnection mysqlConnection;
    private VaultHook vaultHook;
    private ShopHandler shopHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final PluginDescriptionFile pdf = this.getDescription();

        if (getConfig().contains("enabled") && getConfig().getBoolean("enabled")) {
            configHandler = new ConfigHandler(this);

            StaticUtils.init(this, configHandler);

            if (!setupMySQL()) disablePlugin();
            
            StaticUtils.log(ChatColor.LIGHT_PURPLE,
                    ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-",
                    pdf.getName() + " v" + pdf.getVersion() + " created by tbm00",
                    ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"
            );

            if (!setupHooks()) disablePlugin();

            if (configHandler.isFeatureEnabled()) {
                shopHandler = new ShopHandler(this, mysqlConnection, vaultHook);

                // Register Listener
                getServer().getPluginManager().registerEvents(new PlayerMovement(), this);

                // Register Commands
                getCommand("testshop").setExecutor(new ShopCmd(this, configHandler));
                getCommand("testshopadmin").setExecutor(new AdminCmd());
            }
        }
    }

    /**
     * Sets up the MySQL db connection.
     * Disables the plugin if it fails.
     */
    private boolean setupMySQL() {
        try {
            mysqlConnection = new MySQLConnection(this);
            return true;
        } catch (Exception e) {
            getLogger().severe("Failed to connect to MySQL. Disabling plugin.");
            return false;
        }
    }

    /**
     * Sets up the required hooks for plugin integration.
     * Disables the plugin if any required hook fails.
     */
    private boolean setupHooks() {
        if (!setupVault()) {
            getLogger().severe("Vault hook failed -- disabling plugin!");
            return false;
        }
        return true;
    }

    /**
     * Attempts to hook into the Vault plugin.
     *
     * @return true if the hook was successful, false otherwise.
     */
    private boolean setupVault() {
        if (!isPluginAvailable("Vault")) return false;

        vaultHook = new VaultHook(this);

        if (vaultHook==null || vaultHook.pl==null) {
            return false;
        }

        StaticUtils.log(ChatColor.GREEN, "Vault hooked.");
        return true;
    }

    /**
     * Checks if the specified plugin is available and enabled on the server.
     *
     * @param pluginName the name of the plugin to check
     * @return true if the plugin is available and enabled, false otherwise.
     */
    private boolean isPluginAvailable(String pluginName) {
		final Plugin plugin = getServer().getPluginManager().getPlugin(pluginName);
		return plugin != null && plugin.isEnabled();
	}

    /**
     * Disables the plugin.
     */
    private void disablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
    }

    /**
     * Called when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        getLogger().info("PlayerShops64 disabled..! ");
    }
}