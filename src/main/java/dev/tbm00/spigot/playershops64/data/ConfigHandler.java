package dev.tbm00.spigot.playershops64.data;

import org.bukkit.configuration.ConfigurationSection;

import dev.tbm00.spigot.playershops64.PlayerShops64;

public class ConfigHandler {
    private final PlayerShops64 javaPlugin;
    private String chatPrefix;
    private boolean featureEnabled = false;

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
        }
    }

    public String getChatPrefix() {
        return chatPrefix;
    }

    public boolean isFeatureEnabled() {
        return featureEnabled;
    }
}