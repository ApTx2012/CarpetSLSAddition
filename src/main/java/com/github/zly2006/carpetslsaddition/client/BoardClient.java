package com.github.zly2006.carpetslsaddition.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

/**
 * 客户端入口：/slsboard 命令，控制本地计分板侧边栏显示哪个榜。
 *
 * <p>利用原版计分板侧边栏渲染（可靠），各玩家本地切换、互不影响。
 * 数据由服务端从 stats 同步到 scoreboard objective（见 BoardManager）。
 */
@Environment(EnvType.CLIENT)
public class BoardClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommands.literal("slsboard")
                            .then(ClientCommands.literal("deaths").executes(c -> show(c.getSource(), "sls_deaths")))
                            .then(ClientCommands.literal("mined").executes(c -> show(c.getSource(), "sls_mined")))
                            .then(ClientCommands.literal("off").executes(c -> { clear(c.getSource()); return 1; }))
                            .then(ClientCommands.argument("board", StringArgumentType.word())
                                    .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                            new String[]{"deaths", "mined", "off"}, b))
                                    .executes(c -> {
                                        String v = StringArgumentType.getString(c, "board");
                                        if ("off".equals(v)) { clear(c.getSource()); return 1; }
                                        return show(c.getSource(), "sls_" + v);
                                    }))
            );
        });
    }

    private static int show(FabricClientCommandSource source, String objectiveName) {
        var mc = source.getClient();
        var level = mc.level;
        if (level == null) {
            return 0;
        }
        Objective obj = level.getScoreboard().getObjective(objectiveName);
        if (obj == null) {
            source.sendError(Component.literal("[SLSA] 找不到计分板 " + objectiveName + "（服务端规则未开启？）"));
            return 0;
        }
        level.getScoreboard().setDisplayObjective(DisplaySlot.SIDEBAR, obj);
        source.sendFeedback(Component.literal("[SLSA] 榜单显示：" + objectiveName));
        return 1;
    }

    private static void clear(FabricClientCommandSource source) {
        var level = source.getClient().level;
        if (level != null) {
            level.getScoreboard().setDisplayObjective(DisplaySlot.SIDEBAR, null);
        }
        source.sendFeedback(Component.literal("[SLSA] 榜单已关闭"));
    }
}