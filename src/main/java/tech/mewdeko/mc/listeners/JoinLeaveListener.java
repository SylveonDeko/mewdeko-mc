package tech.mewdeko.mc.listeners;

import com.google.gson.JsonObject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tech.mewdeko.mc.MewdekoPlugin;

/**
 * Listens for player join and leave events and forwards them to Discord.
 */
public final class JoinLeaveListener implements Listener {

    private final MewdekoPlugin plugin;

    /**
     * Creates a new join/leave listener.
     *
     * @param plugin the plugin instance
     */
    public JoinLeaveListener(MewdekoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles a player join event.
     *
     * @param event the join event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getPluginConfig().isJoinLeaveEnabled()) {
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("type", "player_join");
        json.addProperty("player", event.getPlayer().getName());
        json.addProperty("uuid", event.getPlayer().getUniqueId().toString().replace("-", ""));

        plugin.getConnection().send(json);
        plugin.sendStatusUpdate();
    }

    /**
     * Handles a player quit event.
     *
     * @param event the quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.getPluginConfig().isJoinLeaveEnabled()) {
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("type", "player_leave");
        json.addProperty("player", event.getPlayer().getName());
        json.addProperty("uuid", event.getPlayer().getUniqueId().toString().replace("-", ""));

        plugin.getConnection().send(json);
        plugin.sendStatusUpdate();
    }
}
