package com.github.zly2006.carpetslsaddition.command;

import carpet.patches.EntityPlayerMPFake;
import carpet.utils.Messenger;
import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.util.access.SLSBotAccessor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.literal;

/**
 * /botall —— 批量控制本模组生成的所有假人。
 *
 * <p>与 {@link BotCommand} 的区别：{@code /bot <player>} 操作单个指定玩家，
 * 而 {@code /botall} 操作所有由本模组（Carpet SLS Addition）生成的假人（即 isBot() 为 true 的 EntityPlayerMPFake）。
 */
public class BotAllCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("botall")
                .requires(source -> source.hasPermission(4) || me.lucko.fabric.api.permissions.v0.Permissions.check(source, "slsaddition.command.bot"))
                .then(literal("sit").executes(BotAllCommand::sitAll))
                .then(literal("stand").executes(BotAllCommand::standAll));
        dispatcher.register(command);
    }

    /** 收集所有本模组生成的假人。 */
    private static List<ServerPlayer> collectBots(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        List<ServerPlayer> bots = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player instanceof EntityPlayerMPFake && ((SLSBotAccessor) player).carpet_SLS_Addition$isBot()) {
                bots.add(player);
            }
        }
        return bots;
    }

    /** 批量坐下。 */
    private static int sitAll(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        if (!SLSCarpetSettings.canUseSitCommand) {
            Messenger.m(context.getSource(), "r 坐下功能未启用（canUseSitCommand）");
            return 0;
        }
        List<ServerPlayer> bots = collectBots(context.getSource());
        int ok = 0;
        for (ServerPlayer bot : bots) {
            if (SitCommand.sitPlayer(bot)) {
                ok++;
            }
        }
        final int done = ok;
        context.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal(
                        "[SLSA] 已让 " + done + "/" + bots.size() + " 个假人坐下"),
                false);
        return ok;
    }

    /** 批量站起（下坐骑）。 */
    private static int standAll(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        List<ServerPlayer> bots = collectBots(context.getSource());
        int ok = 0;
        for (ServerPlayer bot : bots) {
            if (bot.getVehicle() != null) {
                bot.stopRiding();
                ok++;
            }
        }
        final int done = ok;
        context.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal(
                        "[SLSA] 已让 " + done + "/" + bots.size() + " 个假人站起"),
                false);
        return ok;
    }
}