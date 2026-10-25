package com.github.zly2006.carpetslsaddition.command;

import carpet.patches.EntityPlayerMPFake;
import com.github.zly2006.carpetslsaddition.util.BotListManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /botlist —— 维护"受控假人名单"，供 /botall 限定操作范围。
 *
 * <p>名单持久化在 config/slsa-bot-list.json，重启保留。
 */
public class BotListCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("botlist")
                .requires(source -> Commands.LEVEL_OWNERS.check(source.permissions())
                        || me.lucko.fabric.api.permissions.v0.Permissions.check(source, "slsaddition.command.bot"))
                .then(literal("show").executes(BotListCommand::show))
                .then(literal("clear").executes(c -> {
                    BotListManager.clear();
                    c.getSource().sendSuccess(() -> Component.literal("[SLSA] 已清空假人名单"), false);
                    return 1;
                }))
                .then(literal("add")
                        .then(argument("names", StringArgumentType.greedyString())
                                .executes(c -> add(c.getSource(), StringArgumentType.getString(c, "names")))))
                .then(literal("remove")
                        .then(argument("names", StringArgumentType.greedyString())
                                .executes(c -> remove(c.getSource(), StringArgumentType.getString(c, "names")))))
                .then(literal("addall").executes(BotListCommand::addAll));
        dispatcher.register(command);
    }

    private static int show(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c) {
        Set<String> names = BotListManager.getNames();
        if (names.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.literal("[SLSA] 假人名单为空（此时 /botall 操作所有假人）"), false);
        } else {
            c.getSource().sendSuccess(() -> Component.literal(
                    "[SLSA] 假人名单（" + names.size() + "）：" + String.join(", ", names)), false);
        }
        return names.size();
    }

    /**
     * 添加名字到名单。支持两种形式：
     * <ul>
     *   <li>{@code /botlist add bot1 bot2 bot3} —— 空格分隔，一次加多个名字；</li>
     *   <li>{@code /botlist add bot_ 5} —— 名字 + 数量，批量生成式：加入 {@code bot_1 ~ bot_5}。</li>
     * </ul>
     */
    private static int add(CommandSourceStack source, String raw) {
        String[] parts = raw.trim().split("\\s+");
        // 批量生成式：<名字> <数量>
        if (parts.length == 2) {
            try {
                int count = Integer.parseInt(parts[1]);
                if (count >= 1 && count <= 1000) {
                    return addBatch(source, parts[0], count);
                }
                source.sendSuccess(() -> Component.literal(
                        "[SLSA] 数量需在 1~1000 之间"), false);
                return 0;
            } catch (NumberFormatException ignored) {
                // 第二个词不是数字，当作两个普通名字处理
            }
        }
        int n = 0;
        for (String p : parts) {
            if (!p.isEmpty() && BotListManager.add(p)) {
                n++;
            }
        }
        final int added = n;
        source.sendSuccess(() -> Component.literal(
                "[SLSA] 已添加 " + added + " 个名字，名单共 " + BotListManager.getNames().size() + " 个"), false);
        return n;
    }

    /** 批量生成式添加：把 {@code 名字1 ~ 名字count} 加入名单。 */
    private static int addBatch(CommandSourceStack source, String base, int count) {
        int n = 0;
        for (int i = 1; i <= count; i++) {
            String name = base + i;
            if (name.length() > 16) {
                final String tooLong = name;
                source.sendSuccess(() -> Component.literal(
                        "[SLSA] 名字 " + tooLong + " 超过16字符，已截断后续添加"), false);
                break;
            }
            if (BotListManager.add(name)) {
                n++;
            }
        }
        final int added = n;
        source.sendSuccess(() -> Component.literal(
                "[SLSA] 已批量添加 " + base + "1 ~ " + base + count + "，新加入 " + added + " 个，名单共 "
                        + BotListManager.getNames().size() + " 个"), false);
        return n;
    }

    private static int remove(CommandSourceStack source, String raw) {
        String[] parts = raw.trim().split("\\s+");
        int n = 0;
        for (String p : parts) {
            if (!p.isEmpty() && BotListManager.remove(p)) {
                n++;
            }
        }
        final int removed = n;
        source.sendSuccess(() -> Component.literal(
                "[SLSA] 已移除 " + removed + " 个名字，名单共 " + BotListManager.getNames().size() + " 个"), false);
        return n;
    }

    /** 把当前所有在线假人加入名单（含 Carpet /player、MCDR !!ae 生成的）。 */
    private static int addAll(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c) {
        int n = 0;
        for (ServerPlayer player : c.getSource().getServer().getPlayerList().getPlayers()) {
            if (player instanceof EntityPlayerMPFake) {
                if (BotListManager.add(player.getScoreboardName())) {
                    n++;
                }
            }
        }
        final int added = n;
        c.getSource().sendSuccess(() -> Component.literal(
                "[SLSA] 已把 " + added + " 个在线假人加入名单，名单共 " + BotListManager.getNames().size() + " 个"), false);
        return n;
    }
}