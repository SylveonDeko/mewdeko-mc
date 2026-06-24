package tech.mewdeko.mc.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import tech.mewdeko.mc.websocket.BotConnection;

public final class MewdekoCommand {

    private final BotConnection connection;

    public MewdekoCommand(BotConnection connection) {
        this.connection = connection;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("mewdeko")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> { sendStatus(ctx.getSource()); return 1; })
                .then(Commands.literal("status")
                        .executes(ctx -> { sendStatus(ctx.getSource()); return 1; }))
                .then(Commands.literal("reconnect")
                        .executes(ctx -> {
                            connection.disconnect();
                            Thread.ofVirtual().start(connection::connect);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Reconnecting to bot..."), false);
                            return 1;
                        }))
        );
    }

    private void sendStatus(CommandSourceStack source) {
        boolean conn = connection.isConnected();
        int color = conn ? 0x55FF55 : 0xFF5555;
        Component msg = Component.literal("[MewdekoMC] ")
                .withStyle(s -> s.withColor(0xFF79C6))
                .append(Component.literal(conn ? "Connected to bot" : "Not connected to bot")
                        .withStyle(s -> s.withColor(color)));
        source.sendSuccess(() -> msg, false);
    }
}
