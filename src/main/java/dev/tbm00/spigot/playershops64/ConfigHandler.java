package dev.tbm00.spigot.playershops64;

import org.bukkit.configuration.ConfigurationSection;

public class ConfigHandler {
    private final PlayerShops64 javaPlugin;
    private String chatPrefix;
    private boolean featureEnabled = false;
    private int dsMaxStoredBalance;
    private int dsMaxStoredStock;
    private int dsCreationItemPrice;
    private String guiDefaultCategory;
    private boolean dsDescChange = false;
    private boolean dsEditorPrevention = false;

    /**
     * Constructs a ConfigHandler instance.
     * Loads configuration values for the plugin.
     *
     * @param javaPlugin the main plugin instance
     */
    public ConfigHandler(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
        try {
            loadLanguageSection();
            loadFeatureSection();
        } catch (Exception e) {
            javaPlugin.getLogger().warning("Caught exception loading config: " + e.getMessage());
        }
    }

    /**
     * Loads the "lang" section of the configuration.
     */
    private void loadLanguageSection() {
        ConfigurationSection section = javaPlugin.getConfig().getConfigurationSection("lang");
        if (section!=null)
            chatPrefix = section.contains("prefix") ? section.getString("prefix") : null;
    }

    /**
     * Loads the "feature" section of the configuration.
     */
    private void loadFeatureSection() {
        ConfigurationSection section = javaPlugin.getConfig().getConfigurationSection("feature");
        if (section!=null) {
            featureEnabled = section.contains("enabled") ? section.getBoolean("enabled") : false;
            dsMaxStoredBalance = section.contains("dsMaxStoredBalance") ? section.getInt("dsMaxStoredBalance") : 20000000;
            dsMaxStoredStock = section.contains("dsMaxStoredStock") ? section.getInt("dsMaxStoredStock") : 8192;
            dsCreationItemPrice = section.contains("dsCreationItemPrice") ? section.getInt("dsCreationItemPrice") : 4000;
            guiDefaultCategory = section.contains("guiDefaultCategory") ? section.getString("guiDefaultCategory") : "shopgui";
            dsDescChange = section.contains("dsDescChange") ? section.getBoolean("dsDescChange") : false;
            dsDescChange = section.contains("dsEditorPrevention") ? section.getBoolean("dsEditorPrevention") : true;
        }
    }

    public String getChatPrefix() {
        return chatPrefix;
    }

    public boolean isFeatureEnabled() {
        return featureEnabled;
    }

    public int getDSMaxStoredBalance() {
        return dsMaxStoredBalance;
    }

    public int getDSMaxStoredStock() {
        return dsMaxStoredStock;
    }

    public int getDSCreationItemPrice() {
        return dsCreationItemPrice;
    }

    public String getGuiDefaultCategory() {
        return guiDefaultCategory;
    }

    public boolean isDsDescChanged() {
        return dsDescChange;
    }

    public boolean isDsEditorPrevented() {
        return dsEditorPrevention;
    }
}