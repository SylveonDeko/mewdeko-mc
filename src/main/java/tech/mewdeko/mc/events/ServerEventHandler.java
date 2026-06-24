package tech.mewdeko.mc.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import tech.mewdeko.mc.config.ModConfig;
import tech.mewdeko.mc.websocket.BotConnection;

import java.util.Optional;

public final class ServerEventHandler {

    private final BotConnection connection;

    public ServerEventHandler(BotConnection connection) {
        this.connection = connection;
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        if (!ModConfig.CHAT_BRIDGE.get() || !connection.isConnected()) return;

        JsonObject json = new JsonObject();
        json.addProperty("type", "chat");
        json.addProperty("player", event.getPlayer().getName().getString());
        json.addProperty("message", event.getRawText());
        connection.send(json);
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModConfig.JOIN_LEAVE_EVENTS.get()) return;

        JsonObject json = new JsonObject();
        json.addProperty("type", "player_join");
        json.addProperty("player", player.getName().getString());
        json.addProperty("uuid", player.getStringUUID().replace("-", ""));
        connection.send(json);

        sendStatusUpdate();
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModConfig.JOIN_LEAVE_EVENTS.get()) return;

        JsonObject json = new JsonObject();
        json.addProperty("type", "player_leave");
        json.addProperty("player", player.getName().getString());
        json.addProperty("uuid", player.getStringUUID().replace("-", ""));
        connection.send(json);

        sendStatusUpdate();
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModConfig.DEATH_MESSAGES.get() || !connection.isConnected()) return;

        String deathMessage = event.getSource().getLocalizedDeathMessage(player).getString();

        JsonObject json = new JsonObject();
        json.addProperty("type", "death");
        json.addProperty("player", player.getName().getString());
        json.addProperty("message", deathMessage);
        connection.send(json);
    }

    @SubscribeEvent
    public void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModConfig.ADVANCEMENT_MESSAGES.get() || !connection.isConnected()) return;

        AdvancementHolder holder = event.getAdvancement();
        Optional<DisplayInfo> display = holder.value().display();
        if (display.isEmpty() || !display.get().shouldAnnounceChat()) return;

        String title = display.get().getTitle().getString();

        JsonObject json = new JsonObject();
        json.addProperty("type", "advancement");
        json.addProperty("player", player.getName().getString());
        json.addProperty("advancement", title);
        connection.send(json);
    }

    public void sendStatusUpdate() {
        if (!connection.isConnected()) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Thread.ofVirtual().start(() -> {
            double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
            double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 1.0));
            tps = Math.round(tps * 10.0) / 10.0;

            Runtime runtime = Runtime.getRuntime();
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            long maxMemory = runtime.maxMemory() / (1024 * 1024);

            JsonArray players = new JsonArray();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                JsonObject playerObj = new JsonObject();
                playerObj.addProperty("name", p.getName().getString());
                playerObj.addProperty("uuid", p.getStringUUID().replace("-", ""));
                players.add(playerObj);
            }

            JsonObject status = new JsonObject();
            status.addProperty("type", "server_status");
            status.addProperty("tps", tps);
            status.addProperty("usedMemory", usedMemory);
            status.addProperty("maxMemory", maxMemory);
            status.add("players", players);
            status.addProperty("uptime", System.currentTimeMillis() / 1000);
            connection.send(status);
        });
    }
}
