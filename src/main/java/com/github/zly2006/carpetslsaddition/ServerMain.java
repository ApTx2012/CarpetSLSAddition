package com.github.zly2006.carpetslsaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.github.zly2006.carpetslsaddition.command.BotAllCommand;
import com.github.zly2006.carpetslsaddition.command.BotCommand;
import com.github.zly2006.carpetslsaddition.command.BotListCommand;
import com.github.zly2006.carpetslsaddition.command.HatCommand;
import com.github.zly2006.carpetslsaddition.command.SitCommand;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class ServerMain implements ModInitializer, CarpetExtension {
    public static final String MOD_ID = "carpet-sls-addition";
    public static final String MOD_NAME = "Carpet SLS Addition";
    public static final Version MOD_VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).get().getMetadata().getVersion();

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    static final Gson GSON = new GsonBuilder().setLenient().create();

    public static ServerMain INSTANCE;
    public static MinecraftServer server;

    public static final boolean tisCarpetLoaded = FabricLoader.getInstance().isModLoaded("carpet-tis-addition");

    @Override
    public void onInitialize() {
        LOGGER.info("[SLSA] onInitialize: managing Carpet extension");
        INSTANCE = this;
        CarpetServer.manageExtension(this);
        com.github.zly2006.carpetslsaddition.net.BoardSyncPayload.register();
        com.github.zly2006.carpetslsaddition.net.BoardTogglePayload.register();
        // 玩家加入时同步榜单
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, srv) ->
                        com.github.zly2006.carpetslsaddition.util.BoardSyncManager.syncTo(handler.getPlayer()));
    }

    @Override
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(SLSCarpetSettings.class);
    }

    @Override
    public void onServerLoaded(MinecraftServer server) {
        ServerMain.server = server;
        com.github.zly2006.carpetslsaddition.util.BotListManager.load();
        com.github.zly2006.carpetslsaddition.util.BoardSyncManager.syncToAll(server);
        startBoardWatcher(server);
    }

    /** 每秒检测 slsBoardEnabled 规则变化并同步给客户端。 */
    private void startBoardWatcher(MinecraftServer server) {
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    com.github.zly2006.carpetslsaddition.util.BoardSyncManager.tick(server);
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    return;
                } catch (Throwable ex) {
                    LOGGER.debug("[SLSA] board watcher error: {}", ex.toString());
                }
            }
        }, "SLSA-BoardWatcher");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        Map<String, String> translation = Maps.newHashMap();

        try {
            try (InputStream stream = ServerMain.class.getResourceAsStream("/assets/slsaddition/lang/%s.json".formatted(lang))) {
                assert stream != null;
                return GSON.fromJson(new InputStreamReader(stream), Map.class);
            }
        } catch (IOException | NullPointerException ignored) {
            try {
                try (InputStream stream = ServerMain.class.getResourceAsStream("/assets/slsaddition/lang/en_us.json")) {
                    assert stream != null;
                    return GSON.fromJson(new InputStreamReader(stream), Map.class);
                }
            } catch (IOException | NullPointerException e) {
                return translation;
            }
        }
    }

    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        LOGGER.info("[SLSA] registerCommands called!");
        try {
            HatCommand.register(dispatcher);
            SitCommand.register(dispatcher);
            BotCommand.register(dispatcher);
            BotAllCommand.register(dispatcher);
            BotListCommand.register(dispatcher);
            LOGGER.info("[SLSA] commands registered: hat/sit/bot");
        } catch (Throwable t) {
            LOGGER.error("[SLSA] failed to register commands", t);
        }
    }
}
