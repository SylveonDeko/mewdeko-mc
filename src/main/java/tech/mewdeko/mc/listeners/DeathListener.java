package tech.mewdeko.mc.listeners;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import tech.mewdeko.mc.MewdekoPlugin;

/**
 * Listens for player death events and forwards them to Discord.
 */
public final class DeathListener implements Listener {

    private final MewdekoPlugin plugin;

    /**
     * Creates a new death listener.
     *
     * @param plugin the plugin instance
     */
    public DeathListener(MewdekoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles a player death event.
     *
     * @param event the death event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getPluginConfig().isDeathMessagesEnabled()) {
            return;
        }

        String deathMessage = event.deathMessage() != null
                ? PlainTextComponentSerializer.plainText().serialize(event.deathMessage())
                : event.getPlayer().getName() + " died";

        JsonObject json = new JsonObject();
        json.addProperty("type", "death");
        json.addProperty("player", event.getPlayer().getName());
        json.addProperty("message", deathMessage);

        plugin.getConnection().send(json);
    }
}
