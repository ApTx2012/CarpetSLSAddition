package com.github.zly2006.carpetslsaddition.client;

import com.github.zly2006.carpetslsaddition.net.BoardSyncPayload;
import com.github.zly2006.carpetslsaddition.net.BoardTogglePayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端入口：接收榜单数据、注册 /slsboard 命令、注册 HUD 渲染。
 *
 * <p>纯客户端功能：显示哪个榜由本地状态决定，各玩家可不同步。
 */
@Environment(EnvType.CLIENT)
public class BoardClient implements ClientModInitializer {

    public static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("slsa", "board_hud");

    @Override
    public void onInitializeClient() {
        // 1) 接收服务端下发的榜单数据
        ClientPlayNetworking.registerGlobalReceiver(BoardSyncPayload.TYPE, (payload, context) -> {
            List<BoardClientState.Entry> entries = new ArrayList<>();
            for (BoardSyncPayload.Entry e : payload.entries()) {
                entries.add(new BoardClientState.Entry(e.name(), e.score()));
            }
            context.client().execute(() -> BoardClientState.setBoard(payload.board(), entries));
        });

        // 接收开关状态
        ClientPlayNetworking.registerGlobalReceiver(BoardTogglePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> BoardClientState.enabled = payload.enabled());
        });

        // 2) /slsboard 命令（纯客户端，非 OP 也能用）
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommands.literal("slsboard")
                            .then(ClientCommands.literal("deaths")
                                    .executes(c -> run(c.getSource(), () -> BoardClientState.toggle("deaths"))))
                            .then(ClientCommands.literal("mined")
                                    .executes(c -> run(c.getSource(), () -> BoardClientState.toggle("mined"))))
                            .then(ClientCommands.literal("off")
                                    .executes(c -> run(c.getSource(), () -> BoardClientState.set(""))))
                            .then(ClientCommands.argument("board", StringArgumentType.word())
                                    .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                            new String[]{"deaths", "mined", "off"}, b))
                                    .executes(c -> {
                                        String v = StringArgumentType.getString(c, "board");
                                        return run(c.getSource(), () -> BoardClientState.set("off".equals(v) ? "" : v));
                                    }))
            );
        });

        // 3) HUD 渲染
        HudElementRegistry.addLast(HUD_ID, new BoardHudElement());
    }

    /** 统一的命令入口：先检查服务端开关，再执行动作。 */
    private static int run(FabricClientCommandSource source, Runnable action) {
        if (!BoardClientState.enabled) {
            source.sendError(Component.literal("[SLSA] 榜单功能未开启（服务端 /carpet slsBoardEnabled true）"));
            return 0;
        }
        action.run();
        String d = BoardClientState.displaying;
        source.sendFeedback(Component.literal("[SLSA] 榜单显示：" + (d.isEmpty() ? "关闭" : d)));
        return 1;
    }
}