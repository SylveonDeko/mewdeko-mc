package tech.mewdeko.mc.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.mewdeko.mc.MewdekoMod;
import tech.mewdeko.mc.config.ModConfig;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class BotConnection extends WebSocketListener {

    private static final Gson GSON = new Gson();
    private static final long RECONNECT_DELAY_MS = 5000;

    private final Logger logger;
    private final AtomicReference<MinecraftServer> serverRef;
    private final AtomicReference<WebSocket> socket = new AtomicReference<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private volatile OkHttpClient httpClient;

    public BotConnection(Logger logger, AtomicReference<MinecraftServer> serverRef) {
        this.logger = logger;
        this.serverRef = serverRef;
        this.httpClient = buildClient();
    }

    private static OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
    }

    public void connect() {
        String apiUrl = ModConfig.API_URL.get();
        String apiKey = ModConfig.API_KEY.get();

        if (apiUrl.isEmpty() || apiKey.isEmpty()) {
            logger.warn("API URL or key not configured. Set 'api_url' and 'api_key' in the mod config.");
            return;
        }

        if (httpClient.dispatcher().executorService().isShutdown()) {
            httpClient = buildClient();
        }

        String url = apiUrl + "?key=" + apiKey;
        Request request = new Request.Builder().url(url).build();

        logger.info("Connecting to Mewdeko bot...");
        httpClient.newWebSocket(request, this);
    }

    public void disconnect() {
        shuttingDown.set(true);
        WebSocket ws = socket.getAndSet(null);
        if (ws != null) {
            ws.close(1000, "Mod shutting down");
        }
        httpClient.dispatcher().executorService().shutdown();
    }

    public void send(Object message) {
        WebSocket ws = socket.get();
        if (ws == null || !connected.get()) {
            return;
        }
        ws.send(GSON.toJson(message));
    }

    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        socket.set(webSocket);
        connected.set(true);
        logger.info("Connected to Mewdeko bot.");
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        try {
            JsonObject json = GSON.fromJson(text, JsonObject.class);
            String type = json.has("type") ? json.get("type").getAsString() : "";

            switch (type) {
                case "hello" -> logger.info("Bot acknowledged connection.");
                case "chat" -> handleIncomingChat(json);
                case "command" -> handleIncomingCommand(json);
                case "broadcast" -> handleBroadcast(json);
            }
        } catch (Exception e) {
            logger.warn("Error processing bot message: {}", e.getMessage());
        }
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        connected.set(false);
        socket.set(null);
        logger.info("Bot connection closing: {}", reason);
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        connected.set(false);
        socket.set(null);
        logger.info("Bot connection closed: {}", reason);
        scheduleReconnect();
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        connected.set(false);
        socket.set(null);
        logger.warn("Bot connection failed: {}", t.getMessage());
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        if (shuttingDown.get()) return;
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
            } catch (InterruptedException ignored) {}
            if (!shuttingDown.get()) connect();
        });
    }

    private void handleIncomingChat(JsonObject json) {
        String user = json.has("user") ? json.get("user").getAsString() : "Unknown";
        String message = json.has("message") ? json.get("message").getAsString() : "";
        String formatted = json.has("formattedMessage") && !json.get("formattedMessage").isJsonNull()
                ? json.get("formattedMessage").getAsString() : null;

        MinecraftServer server = serverRef.get();
        if (server == null) return;

        server.execute(() -> {
            Component component;
            if (formatted != null && !formatted.isEmpty()) {
                component = Component.literal(formatted);
            } else {
                component = Component.literal("")
                        .append(Component.literal("[Discord] ").withStyle(s -> s.withColor(0x5865F2)))
                        .append(Component.literal(user + ": ").withStyle(s -> s.withColor(0xAAAAAA)))
                        .append(Component.literal(message));
            }
            server.getPlayerList().broadcastSystemMessage(component, false);
        });
    }

    private void handleIncomingCommand(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "";
        String command = json.has("command") ? json.get("command").getAsString() : "";

        MinecraftServer server = serverRef.get();
        if (server == null) return;

        server.execute(() -> {
            try {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);

                JsonObject response = new JsonObject();
                response.addProperty("type", "command_response");
                response.addProperty("id", id);
                response.addProperty("response", "Command executed");
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

        MinecraftServer server = serverRef.get();
        if (server == null) return;

        server.execute(() -> {
            Component component = Component.literal("")
                    .append(Component.literal("[Mewdeko] ").withStyle(s -> s.withColor(0xFF79C6)))
                    .append(Component.literal(message));
            server.getPlayerList().broadcastSystemMessage(component, false);
        });
    }
}
