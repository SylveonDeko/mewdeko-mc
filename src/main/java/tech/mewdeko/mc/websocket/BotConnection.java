package tech.mewdeko.mc.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.mewdeko.mc.MewdekoPlugin;
import tech.mewdeko.mc.config.PluginConfig;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Manages the WebSocket connection to the Mewdeko bot API.
 * Handles automatic reconnection on disconnect.
 */
public final class BotConnection extends WebSocketListener {

    private static final Gson GSON = new Gson();
    private static final long RECONNECT_DELAY_MS = 5000;

    private final MewdekoPlugin plugin;
    private final PluginConfig config;
    private final AtomicReference<WebSocket> socket = new AtomicReference<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private volatile OkHttpClient httpClient;

    /**
     * Creates a new bot connection manager.
     *
     * @param plugin the plugin instance
     * @param config the plugin configuration
     */
    public BotConnection(MewdekoPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.httpClient = buildClient();
    }

    private static OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Initiates the WebSocket connection.
     */
    public void connect() {
        if (config.getApiUrl().isEmpty() || config.getApiKey().isEmpty()) {
            plugin.getLogger().warning("API URL or key not configured. Set 'api-url' and 'api-key' in config.yml from the dashboard.");
            return;
        }

        if (httpClient.dispatcher().executorService().isShutdown()) {
            httpClient = buildClient();
        }

        String url = config.getApiUrl() + "?key=" + config.getApiKey();
        Request request = new Request.Builder().url(url).build();

        plugin.getLogger().info("Connecting to Mewdeko bot...");
        httpClient.newWebSocket(request, this);
    }

    /**
     * Disconnects from the bot and stops reconnection attempts.
     */
    public void disconnect() {
        shuttingDown.set(true);
        WebSocket ws = socket.getAndSet(null);
        if (ws != null) {
            ws.close(1000, "Plugin shutting down");
        }
        httpClient.dispatcher().executorService().shutdown();
    }

    /**
     * Sends a JSON message to the bot.
     *
     * @param message the message object to serialize and send
     */
    public void send(Object message) {
        WebSocket ws = socket.get();
        if (ws == null || !connected.get()) {
            return;
        }
        ws.send(GSON.toJson(message));
    }

    /**
     * Returns whether the connection is currently active.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        socket.set(webSocket);
        connected.set(true);
        plugin.getLogger().info("Connected to Mewdeko bot.");
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        try {
            JsonObject json = GSON.fromJson(text, JsonObject.class);
            String type = json.has("type") ? json.get("type").getAsString() : "";

            switch (type) {
                case "hello" -> plugin.getLogger().info("Bot acknowledged connection.");
                case "chat" -> handleIncomingChat(json);
                case "command" -> handleIncomingCommand(json);
                case "broadcast" -> handleBroadcast(json);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error processing bot message", e);
        }
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        connected.set(false);
        socket.set(null);
        plugin.getLogger().info("Bot connection closing: " + reason);
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        connected.set(false);
        socket.set(null);
        plugin.getLogger().info("Bot connection closed: " + reason);
        scheduleReconnect();
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        connected.set(false);
        socket.set(null);
        plugin.getLogger().log(Level.WARNING, "Bot connection failed: " + t.getMessage());
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        if (shuttingDown.get()) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this::connect,
                RECONNECT_DELAY_MS / 50);
    }

    private void handleIncomingChat(JsonObject json) {
        String user = json.has("user") ? json.get("user").getAsString() : "Unknown";
        String message = json.has("message") ? json.get("message").getAsString() : "";
        String formatted = json.has("formattedMessage") && !json.get("formattedMessage").isJsonNull()
                ? json.get("formattedMessage").getAsString() : null;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (formatted != null && !formatted.isEmpty()) {
                plugin.getServer().broadcast(
                        net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                                .legacySection().deserialize(formatted));
            } else {
                plugin.getServer().broadcast(
                        net.kyori.adventure.text.Component.text()
                                .append(net.kyori.adventure.text.Component.text("[Discord] ",
                                        net.kyori.adventure.text.format.NamedTextColor.BLUE))
                                .append(net.kyori.adventure.text.Component.text(user + ": ",
                                        net.kyori.adventure.text.format.NamedTextColor.GRAY))
                                .append(net.kyori.adventure.text.Component.text(message,
                                        net.kyori.adventure.text.format.NamedTextColor.WHITE))
                                .build());
            }
        });
    }

    private void handleIncomingCommand(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "";
        String command = json.has("command") ? json.get("command").getAsString() : "";

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                boolean result = plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(), command);

                JsonObject response = new JsonObject();
                response.addProperty("type", "command_response");
                response.addProperty("id", id);
                response.addProperty("response", result ? "Command executed" : "Command failed");
                send(response);
            } catch (Exception e) {
                JsonObject response = new JsonObject();
                response.addProperty("type", "command_response");
                response.addProperty("id", id);
                response.addProperty("response", "Error: " + e.getMessage());
                send(response);
            }
        });
    }

    private void handleBroadcast(JsonObject json) {
        String message = json.has("message") ? json.get("message").getAsString() : "";

        plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getServer().broadcast(
                        net.kyori.adventure.text.Component.text()
                                .append(net.kyori.adventure.text.Component.text("[Mewdeko] ",
                                        net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE))
                                .append(net.kyori.adventure.text.Component.text(message,
                                        net.kyori.adventure.text.format.NamedTextColor.WHITE))
                                .build()
                )
        );
    }
}
