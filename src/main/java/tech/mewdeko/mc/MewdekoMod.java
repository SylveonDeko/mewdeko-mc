package tech.mewdeko.mc;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.mewdeko.mc.command.MewdekoCommand;
import tech.mewdeko.mc.events.ServerEventHandler;
import tech.mewdeko.mc.websocket.BotConnection;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Mod("mewdeko")
public final class MewdekoMod {

    public static final Logger LOGGER = LogManager.getLogger("mewdeko");

    private final AtomicReference<MinecraftServer> serverRef = new AtomicReference<>();
    private final BotConnection connection;
    private final ServerEventHandler eventHandler;
    private final MewdekoCommand command;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> Thread.ofVirtual().unstarted(r));
    private ScheduledFuture<?> statusTask;

    public MewdekoMod(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, tech.mewdeko.mc.config.ModConfig.SPEC, "mewdeko-server.toml");

        connection = new BotConnection(LOGGER, serverRef);
        eventHandler = new ServerEventHandler(connection);
        command = new MewdekoCommand(connection);

        NeoForge.EVENT_BUS.register(eventHandler);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onServerStarted(ServerStartedEvent event) {
        serverRef.set(event.getServer());

        int intervalSecs = tech.mewdeko.mc.config.ModConfig.STATUS_INTERVAL.get();
        statusTask = scheduler.scheduleAtFixedRate(
                eventHandler::sendStatusUpdate,
                intervalSecs, intervalSecs, TimeUnit.SECONDS);

        scheduler.schedule(connection::connect, 2, TimeUnit.SECONDS);

        LOGGER.info("MewdekoMC enabled.");
    }

    private void onServerStopping(ServerStoppingEvent event) {
        if (statusTask != null) statusTask.cancel(false);
        scheduler.shutdown();
        connection.disconnect();
        serverRef.set(null);
        LOGGER.info("MewdekoMC disabled.");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        command.register(event.getDispatcher());
    }
}
