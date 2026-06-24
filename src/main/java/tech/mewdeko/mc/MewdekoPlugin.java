package tech.mewdeko.mc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import tech.mewdeko.mc.commands.MewdekoCommand;
import tech.mewdeko.mc.config.PluginConfig;
import tech.mewdeko.mc.listeners.AdvancementListener;
import tech.mewdeko.mc.listeners.ChatListener;
import tech.mewdeko.mc.listeners.DeathListener;
import tech.mewdeko.mc.listeners.JoinLeaveListener;
import tech.mewdeko.mc.websocket.BotConnection;

/**
 * Mewdeko companion plugin for Paper/Spigot Minecraft servers.
 * Bridges events, chat, and server status to the Mewdeko Discord bot.
 */
public final class MewdekoPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private BotConnection connection;
    private int statusTaskId = -1;

    @Override
    public void onEnable() {
        pluginConfig = new PluginConfig(this);
        connection = new BotConnection(this, pluginConfig);

        new MewdekoCommand(this).register();

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new AdvancementListener(this), this);

        getServer().getScheduler().runTaskLaterAsynchronously(this, () -> connection.connect(), 40L);

        startStatusUpdates();

        getLogger().info("MewdekoMC enabled.");
    }

    @Override
    public void onDisable() {
        if (statusTaskId != -1) {
            getServer().getScheduler().cancelTask(statusTaskId);
        }
        connection.disconnect();
        getLogger().info("MewdekoMC disabled.");
    }

    /**
     * Returns the plugin configuration.
     *
     * @return the plugin config
     */
    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    /**
     * Returns the bot WebSocket connection.
     *
     * @return the connection
     */
    public BotConnection getConnection() {
        return connection;
    }

    /**
     * Sends a server status update to the bot immediately.
     * Called on player join/leave and periodically for TPS/memory metrics.
     */
    public void sendStatusUpdate() {
        if (!connection.isConnected()) {
            return;
        }

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            double[] tps = Bukkit.getTPS();
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            long maxMemory = runtime.maxMemory() / (1024 * 1024);

            JsonArray players = new JsonArray();
            for (Player player : Bukkit.getOnlinePlayers()) {
                JsonObject playerObj = new JsonObject();
                playerObj.addProperty("name", player.getName());
                playerObj.addProperty("uuid", player.getUniqueId().toString().replace("-", ""));
                players.add(playerObj);
            }

            JsonObject status = new JsonObject();
            status.addProperty("type", "server_status");
            status.addProperty("tps", Math.round(tps[0] * 10.0) / 10.0);
            status.addProperty("usedMemory", usedMemory);
            status.addProperty("maxMemory", maxMemory);
            status.add("players", players);
            status.addProperty("uptime", System.currentTimeMillis() / 1000);

            connection.send(status);
        });
    }

    private void startStatusUpdates() {
        long intervalTicks = pluginConfig.getStatusInterval() * 20L;

        statusTaskId = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            sendStatusUpdate();
        }, intervalTicks, intervalTicks).getTaskId();
    }
}
