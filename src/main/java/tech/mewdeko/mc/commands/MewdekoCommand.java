package tech.mewdeko.mc.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tech.mewdeko.mc.MewdekoPlugin;

/**
 * Registers and handles the /mewdeko command using Paper's Brigadier API.
 */
@SuppressWarnings("UnstableApiUsage")
public final class MewdekoCommand {

    private final MewdekoPlugin plugin;

    /**
     * Creates a new command handler.
     *
     * @param plugin the plugin instance
     */
    public MewdekoCommand(MewdekoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers the /mewdeko command via Paper's lifecycle event system.
     */
    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var registrar = event.registrar();

            var command = Commands.literal("mewdeko")
                    .requires(source -> source.getSender().hasPermission("mewdeko.admin"))
                    .executes(ctx -> {
                        sendStatus(ctx.getSource().getSender());
                        return 1;
                    })
                    .then(Commands.literal("status")
                            .executes(ctx -> {
                                sendStatus(ctx.getSource().getSender());
                                return 1;
                            }))
                    .then(Commands.literal("reload")
                            .executes(ctx -> {
                                plugin.getPluginConfig().reload();
                                ctx.getSource().getSender().sendMessage(
                                        Component.text("Configuration reloaded.", NamedTextColor.GREEN));
                                return 1;
                            }))
                    .then(Commands.literal("reconnect")
                            .executes(ctx -> {
                                plugin.getConnection().disconnect();
                                plugin.getConnection().connect();
                                ctx.getSource().getSender().sendMessage(
                                        Component.text("Reconnecting to bot...", NamedTextColor.YELLOW));
                                return 1;
                            }))
                    .build();

            registrar.register(command, "Mewdeko companion plugin management");
        });
    }

    private void sendStatus(org.bukkit.command.CommandSender sender) {
        boolean connected = plugin.getConnection().isConnected();
        var color = connected ? NamedTextColor.GREEN : NamedTextColor.RED;

        sender.sendMessage(Component.text()
                .append(Component.text("[MewdekoMC] ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(connected ? "Connected to bot" : "Not connected to bot", color))
                .build());
    }
}
