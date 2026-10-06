package com.github.zly2006.carpetslsaddition.client;

import com.github.zly2006.carpetslsaddition.net.BoardRequestPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * 客户端入口：/slsboard 命令，请求服务端给自己显示某个榜单。
 *
 * <p>服务端收到请求后，给该玩家单独发 display 包，实现各玩家各自显示、互不影响。
 * 数据由服务端从 stats 同步到 scoreboard objective（见 BoardManager）。
 */
@Environment(EnvType.CLIENT)
public class BoardClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommands.literal("slsboard")
                            .then(ClientCommands.literal("deaths").executes(c -> request(c.getSource(), "sls_deaths")))
                            .then(ClientCommands.literal("mined").executes(c -> request(c.getSource(), "sls_mined")))
                            .then(ClientCommands.literal("off").executes(c -> request(c.getSource(), "")))
                            .then(ClientCommands.argument("board", StringArgumentType.word())
                                    .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                            new String[]{"deaths", "mined", "off"}, b))
                                    .executes(c -> {
                                        String v = StringArgumentType.getString(c, "board");
                                        return request(c.getSource(), "off".equals(v) ? "" : "sls_" + v);
                                    }))
            );
        });
    }

    private static int request(FabricClientCommandSource source, String objectiveName) {
        ClientPlayNetworking.send(new BoardRequestPayload(objectiveName));
        source.sendFeedback(Component.literal(
                objectiveName.isEmpty() ? "[SLSA] 榜单已关闭" : "[SLSA] 请求显示：" + objectiveName));
        return 1;
    }
}