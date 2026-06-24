package tech.mewdeko.mc.config;

import org.bukkit.configuration.file.FileConfiguration;
import tech.mewdeko.mc.MewdekoPlugin;

/**
 * Manages plugin configuration from config.yml.
 */
public final class PluginConfig {

    private final MewdekoPlugin plugin;

    private String apiUrl;
    private String apiKey;
    private int statusInterval;
    private boolean chatBridge;
    private boolean joinLeaveEvents;
    private boolean deathMessages;
    private boolean advancementMessages;
    private boolean consoleStreaming;

    /**
     * Creates a new config instance and loads values from disk.
     *
     * @param plugin the plugin instance
     */
    public PluginConfig(MewdekoPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Reloads configuration values from config.yml.
     */
    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        apiUrl = config.getString("api-url", "");
        apiKey = config.getString("api-key", "");
        statusInterval = config.getInt("status-interval", 30);
        chatBridge = config.getBoolean("chat-bridge", true);
        joinLeaveEvents = config.getBoolean("join-leave-events", true);
        deathMessages = config.getBoolean("death-messages", true);
        advancementMessages = config.getBoolean("advancement-messages", true);
        consoleStreaming = config.getBoolean("console-streaming", false);
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public int getStatusInterval() {
        return statusInterval;
    }

    public boolean isChatBridgeEnabled() {
        return chatBridge;
    }

    public boolean isJoinLeaveEnabled() {
        return joinLeaveEvents;
    }

    public boolean isDeathMessagesEnabled() {
        return deathMessages;
    }

    public boolean isAdvancementMessagesEnabled() {
        return advancementMessages;
    }

    public boolean isConsoleStreamingEnabled() {
        return consoleStreaming;
    }
}
