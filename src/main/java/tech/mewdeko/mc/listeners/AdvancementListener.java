package tech.mewdeko.mc.listeners;

import com.google.gson.JsonObject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import tech.mewdeko.mc.MewdekoPlugin;

/**
 * Listens for player advancement events and forwards them to Discord.
 */
public final class AdvancementListener implements Listener {

    private final MewdekoPlugin plugin;

    /**
     * Creates a new advancement listener.
     *
     * @param plugin the plugin instance
     */
    public AdvancementListener(MewdekoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles a player advancement completion event.
     *
     * @param event the advancement event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (!plugin.getPluginConfig().isAdvancementMessagesEnabled()) {
            return;
        }

        var advancement = event.getAdvancement();
        var display = advancement.getDisplay();
        if (display == null || !display.doesAnnounceToChat()) {
            return;
        }

        String title = display.title() != null
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(display.title())
                : advancement.getKey().getKey();

        JsonObject json = new JsonObject();
        json.addProperty("type", "advancement");
        json.addProperty("player", event.getPlayer().getName());
        json.addProperty("advancement", title);

        plugin.getConnection().send(json);
    }
}
