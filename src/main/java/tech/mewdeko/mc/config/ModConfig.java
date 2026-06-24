package tech.mewdeko.mc.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> API_URL;
    public static final ModConfigSpec.ConfigValue<String> API_KEY;
    public static final ModConfigSpec.IntValue STATUS_INTERVAL;
    public static final ModConfigSpec.BooleanValue CHAT_BRIDGE;
    public static final ModConfigSpec.BooleanValue JOIN_LEAVE_EVENTS;
    public static final ModConfigSpec.BooleanValue DEATH_MESSAGES;
    public static final ModConfigSpec.BooleanValue ADVANCEMENT_MESSAGES;
    public static final ModConfigSpec.BooleanValue CONSOLE_STREAMING;

    static {
        BUILDER.comment("Mewdeko companion mod configuration");

        API_URL = BUILDER.comment("The WebSocket URL shown when generating a plugin key in the dashboard")
                .define("api_url", "");
        API_KEY = BUILDER.comment("The per-server plugin API key generated from the dashboard")
                .define("api_key", "");
        STATUS_INTERVAL = BUILDER.comment("How often to send server status updates (in seconds)")
                .defineInRange("status_interval", 30, 5, 3600);
        CHAT_BRIDGE = BUILDER.comment("Whether to relay chat messages to Discord")
                .define("chat_bridge", true);
        JOIN_LEAVE_EVENTS = BUILDER.comment("Whether to send player join/leave events")
                .define("join_leave_events", true);
        DEATH_MESSAGES = BUILDER.comment("Whether to send death messages")
                .define("death_messages", true);
        ADVANCEMENT_MESSAGES = BUILDER.comment("Whether to send advancement messages")
                .define("advancement_messages", true);
        CONSOLE_STREAMING = BUILDER.comment("Whether to stream console log lines")
                .define("console_streaming", false);

        SPEC = BUILDER.build();
    }

    private ModConfig() {}
}
