package tech.mewdeko.mc.listeners;

import com.google.gson.JsonObject;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tech.mewdeko.mc.MewdekoPlugin;

/**
 * Listens for in-game chat messages and forwards them to Discord.
 */
public final class ChatListener implements Listener {

    private final MewdekoPlugin plugin;

    /**
     * Creates a new chat listener.
     *
     * @param plugin the plugin instance
     */
    public ChatListener(MewdekoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles an async chat event.
     *
     * @param event the chat event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getPluginConfig().isChatBridgeEnabled()) {
            return;
        }

        String playerName = event.getPlayer().getName();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        JsonObject json = new JsonObject();
        json.addProperty("type", "chat");
        json.addProperty("player", playerName);
        json.addProperty("message", message);

        plugin.getConnection().send(json);
    }
}
