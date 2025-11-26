package dev.tbm00.papermc.playershops64;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.papermc.playershops64.command.AdminCmd;
import dev.tbm00.papermc.playershops64.command.DepositGuiCmd;
import dev.tbm00.papermc.playershops64.command.ExchangeShopCmd;
import dev.tbm00.papermc.playershops64.command.ExchangeWandCmd;
import dev.tbm00.papermc.playershops64.command.SellGuiCmd;
import dev.tbm00.papermc.playershops64.command.ShopCmd;
import dev.tbm00.papermc.playershops64.data.ConfigHandler;
import dev.tbm00.papermc.playershops64.data.MySQLConnection;
import dev.tbm00.papermc.playershops64.hook.VaultHook;
import dev.tbm00.papermc.playershops64.listener.ChunkActivity;
import dev.tbm00.papermc.playershops64.listener.PlayerConnection;
import dev.tbm00.papermc.playershops64.listener.PlayerCoupon;
import dev.tbm00.papermc.playershops64.listener.PlayerMovement;
import dev.tbm00.papermc.playershops64.listener.PlayerWand;
//import dev.tbm00.papermc.playershops64.listener.ServerStartup;
import dev.tbm00.papermc.playershops64.listener.ShopBaseBlock;
import dev.tbm00.papermc.playershops64.utils.GuiUtils;
import dev.tbm00.papermc.playershops64.utils.Logger;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;
import xzot1k.plugins.ds.DisplayShops;

public class PlayerShops64 extends JavaPlugin {
    private ConfigHandler configHandler;
    private MySQLConnection mysqlConnection;
    private VaultHook vaultHook;
    private ShopHandler shopHandler;
    public DisplayShops dsHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final PluginDescriptionFile pdf = this.getDescription();

        if (getConfig().contains("enabled") && getConfig().getBoolean("enabled")) {
            StaticUtils.init(this);
            ShopUtils.init(this);
            GuiUtils.init(this);
            Logger.init(this);
            StaticUtils.log(ChatColor.LIGHT_PURPLE,
                    ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-",
                    pdf.getName() + " v" + pdf.getVersion() + " created by tbm00",
                    ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"
            );

            try {
                getDataFolder().mkdirs();
                File editLog = new File(getDataFolder(), "log.txt");
                if (!editLog.exists()) editLog.createNewFile();
                Logger.setEditLogFile(editLog);
            } catch (IOException e) {
                StaticUtils.log(ChatColor.RED, "Failed to initialize log.txt: " + e.getMessage());
            }

            configHandler = new ConfigHandler(this);

            if (!setupMySQL()) disablePlugin();
            if (!setupHooks()) disablePlugin();

            shopHandler = new ShopHandler(this, mysqlConnection);
            shopHandler.validateShops();

            // Register Listeners
            // getServer().getPluginManager().registerEvents(new ServerStartup(this), this);
            getServer().getPluginManager().registerEvents(new ChunkActivity(this), this);
            getServer().getPluginManager().registerEvents(new PlayerConnection(this), this);
            getServer().getPluginManager().registerEvents(new PlayerMovement(), this);
            getServer().getPluginManager().registerEvents(new PlayerWand(this), this);
            getServer().getPluginManager().registerEvents(new ShopBaseBlock(this), this);
            if (configHandler.isAdminShopCouponEnabled())
                getServer().getPluginManager().registerEvents(new PlayerCoupon(this), this);
            
            // Register Commands
            getCommand("depositgui").setExecutor(new DepositGuiCmd(this));
            getCommand("shopadmin").setExecutor(new AdminCmd(this));
            getCommand("exchangesellwand").setExecutor(new ExchangeWandCmd(this));
            getCommand("exchangeshops").setExecutor(new ExchangeShopCmd(this));
            if (!configHandler.isShopGuiPlusEnabled()) {
                getCommand("shop").setExecutor(new ShopCmd(this));
                getCommand("sellgui").setExecutor(new SellGuiCmd(this));
            } else {
                // delayed task that tries to hookSGP() after 1200 ticks
                StaticUtils.log(ChatColor.YELLOW, "Overriding ShopGUIPlus in 2 minutes!");
                getServer().getScheduler().runTaskLater(this, () -> {
                    hookSGP();
                }, 1200L);
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
            StaticUtils.log(ChatColor.RED, "Failed to connect to MySQL. Disabling plugin.");
            return false;
        }
    }

    /**
     * Sets up the required hooks for plugin integration.
     * Disables the plugin if any required hook fails.
     */
    private boolean setupHooks() {
        if (!setupDisplayShops()) {
            StaticUtils.log(ChatColor.RED, "DisplayShops hook failed!");
        }
        if (!setupVault()) {
            StaticUtils.log(ChatColor.RED, "Vault hook failed -- disabling plugin!");
            return false;
        } if (!checkFloodgate()) {
            StaticUtils.log(ChatColor.RED, "Floodgate hook failed -- disabling plugin!");
            return false;
        }
        return true;
    }

    /**
     * Attempts to hook into the DisplayShops plugin.
     *
     * @return true if the hook was successful, false otherwise.
     */
    private boolean setupDisplayShops() {
        if (!isPluginAvailable("DisplayShops")) return false;

        dsHook = (DisplayShops) getServer().getPluginManager().getPlugin("DisplayShops");
        
        StaticUtils.log(ChatColor.GREEN, "DisplayShops hooked.");
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
     * Attempts to hook into the Vault plugin.
     *
     * @return true if the hook was successful, false otherwise.
     */
    private boolean checkFloodgate() {
        if (!configHandler.isFloodgateEnabled()) return true;
        if (!isPluginAvailable("Floodgate")) return false;

        StaticUtils.log(ChatColor.GREEN, "Floodgate hooked.");
        return true;
    }

    public void hookSGP() {
        StaticUtils.log(ChatColor.YELLOW, "Hooking into ShopGUIPlus...");
        cloneSGPCommand("originalsgplus");
        overrideSGPCommands();
    }

    @SuppressWarnings("unchecked")
    private void cloneSGPCommand(String newName) {
        Plugin sgp = getServer().getPluginManager().getPlugin("ShopGUIPlus");
        if (sgp == null) return;
        
        try {
            // 1) grab the SimpleCommandMap
            SimpleCommandMap commandMap = (SimpleCommandMap)
                getServer().getClass().getMethod("getCommandMap").invoke(getServer());
            Field knownField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownField.setAccessible(true);
            Map<String, Command> known = (Map<String, Command>) knownField.get(commandMap);

            // 2) find the real PluginCommand whose getName() == "shop"
            PluginCommand original = null;
            for (Command cmd : known.values()) {
                if (cmd instanceof PluginCommand pc
                    && pc.getPlugin().equals(sgp)
                    && pc.getName().equalsIgnoreCase("shop"))
                {
                    original = pc;
                    break;
                }
            }
            if (original == null) {
                getLogger().warning("Could not locate ShopGUIPlus's original /shop command");
                return;
            }

            // 3) reflectively construct a fresh PluginCommand
            Constructor<PluginCommand> ctor =
                PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            ctor.setAccessible(true);
            PluginCommand clone = ctor.newInstance(newName, sgp);

            // 4) copy over data
            clone.setDescription(original.getDescription());
            clone.setUsage(original.getUsage());
            clone.setPermission(original.getPermission());
            clone.setAliases(new ArrayList<>());
            clone.setExecutor(original.getExecutor());
            if (original.getTabCompleter() != null) {
                clone.setTabCompleter(original.getTabCompleter());
            }

            // 5) register it under SGP's namespace
            commandMap.register(sgp.getName(), clone);
            getLogger().info("Cloned ShopGUIPlus's /shop ➔ /" + newName);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to clone ShopGUIPlus's original /shop command", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void overrideSGPCommands() {
        Plugin sgp = getServer().getPluginManager().getPlugin("ShopGUIPlus");
        if (sgp == null) return;

        try {
            // grab commandMap & knownCommands
            SimpleCommandMap commandMap = (SimpleCommandMap)
                getServer().getClass().getMethod("getCommandMap").invoke(getServer());
            Field f = SimpleCommandMap.class.getDeclaredField("knownCommands");
            f.setAccessible(true);
            Map<String, Command> known = (Map<String, Command>) f.get(commandMap);

            for (Command cmd : known.values()) {
                if (!(cmd instanceof PluginCommand)) continue;
                PluginCommand pc = (PluginCommand) cmd;
                if (!pc.getPlugin().equals(sgp)) continue;

                // override /shop
                if (pc.getName().equalsIgnoreCase("shop")) {
                    //getLogger().info(pc.getName()+": "+pc.getLabel()+" "+pc.getAliases().subList(0, pc.getAliases().size()-1).toString());
                    ShopCmd shopCmd = new ShopCmd(this);
                    pc.setExecutor(shopCmd);
                    pc.setTabCompleter(shopCmd);
                    getLogger().info("Overrode ShopGUIPlus's /shop");
                    continue;
                }

                // override /sell
                if (pc.getName().equalsIgnoreCase("sell")) {
                    //getLogger().info(pc.getName()+": "+pc.getLabel()+" "+pc.getAliases().subList(0, pc.getAliases().size()-1).toString());
                    SellGuiCmd sellGuiCmd = new SellGuiCmd(this);
                    getCommand("sellgui").setExecutor(sellGuiCmd);
                    pc.setExecutor(sellGuiCmd);
                    pc.setTabCompleter(sellGuiCmd);
                    getLogger().info("Overrode ShopGUIPlus's /sell");
                    continue;
                }

                //getLogger().info("Leftover: "+pc.getName()+": "+pc.getLabel()+" "+pc.getAliases().toArray().toString());
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Could not override ShopGUIPlus commands", e);
        }
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

    /*private void cleanUpDisplays() {
        int count = 0;
        for (World world : getServer().getWorlds()) {
            for (var ent : world.getEntitiesByClasses(
                    ItemDisplay.class,
                    TextDisplay.class,
                    Item.class)) {
                if (!ent.getPersistentDataContainer().has(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING)) continue;
                ent.remove();
                count++;
            }
        }
        StaticUtils.log(ChatColor.YELLOW, "Deleted " + count + " stale display entities.");
    }*/

    /**
     * Disables the plugin. (never gets used in my codebase)
     */
    private void disablePlugin() {
        getLogger().info("PlayerShops64 disabling..! (1)");
        if (shopHandler!=null) shopHandler.shutdown();
        if (mysqlConnection != null) mysqlConnection.closeConnection();
        getServer().getPluginManager().disablePlugin(this);
    }


    /**
     * Called when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        getLogger().info("PlayerShops64 disabling..! (2)");
        if (shopHandler!=null) shopHandler.shutdown();
        if (mysqlConnection != null) mysqlConnection.closeConnection();
    }

    public ShopHandler getShopHandler() {
        if (shopHandler == null) throw new IllegalStateException("ShopHandler not ready -- PlayerShops64 not fully enabled?");
        return shopHandler;
    }
    public VaultHook getVaultHook() {
        if (vaultHook == null) throw new IllegalStateException("VaultHook not ready -- PlayerShops64 not fully enabled?");
        return vaultHook;
    }
    public ConfigHandler getConfigHandler() {
        if (configHandler == null) throw new IllegalStateException("ConfigHandler not ready -- PlayerShops64 not fully enabled?");
        return configHandler;
    }
}