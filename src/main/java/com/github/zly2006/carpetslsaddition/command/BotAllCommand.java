package com.github.zly2006.carpetslsaddition.command;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.util.access.SLSBotAccessor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /botall —— 批量控制本模组生成的所有假人。
 *
 * <p>与 {@link BotCommand} 的区别：{@code /bot <player>} 操作单个指定玩家，
 * 而 {@code /botall} 对其余命令中出现的所有本模组假人（isBot()==true）批量执行同一操作。
 */
public class BotAllCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("botall")
                .requires(source -> Commands.LEVEL_OWNERS.check(source.permissions()) || me.lucko.fabric.api.permissions.v0.Permissions.check(source, "slsaddition.command.bot"))
                .then(literal("stop").executes(batch(EntityPlayerActionPack::stopAll)))
                .then(actionCommand("use", EntityPlayerActionPack.ActionType.USE))
                .then(actionCommand("jump", EntityPlayerActionPack.ActionType.JUMP))
                .then(actionCommand("attack", EntityPlayerActionPack.ActionType.ATTACK))
                .then(actionCommand("drop", EntityPlayerActionPack.ActionType.DROP_ITEM))
                .then(dropCommand("dropStack", true))
                .then(actionCommand("swapHands", EntityPlayerActionPack.ActionType.SWAP_HANDS))
                .then(literal("hotbar").then(argument("slot", IntegerArgumentType.integer(1, 9))
                        .executes(c -> batchAp(c, ap -> ap.setSlot(IntegerArgumentType.getInteger(c, "slot"))))))
                .then(literal("mount").executes(batch(ap -> ap.mount(true)))
                        .then(literal("anything").executes(batch(ap -> ap.mount(false)))))
                .then(literal("dismount").executes(batch(EntityPlayerActionPack::dismount)))
                .then(literal("sneak").executes(batch(ap -> ap.setSneaking(true))))
                .then(literal("unsneak").executes(batch(ap -> ap.setSneaking(false))))
                .then(literal("sprint").executes(batch(ap -> ap.setSprinting(true))))
                .then(literal("unsprint").executes(batch(ap -> ap.setSprinting(false))))
                .then(literal("look")
                        .then(literal("north").executes(batch(ap -> ap.look(Direction.NORTH))))
                        .then(literal("south").executes(batch(ap -> ap.look(Direction.SOUTH))))
                        .then(literal("east").executes(batch(ap -> ap.look(Direction.EAST))))
                        .then(literal("west").executes(batch(ap -> ap.look(Direction.WEST))))
                        .then(literal("up").executes(batch(ap -> ap.look(Direction.UP))))
                        .then(literal("down").executes(batch(ap -> ap.look(Direction.DOWN))))
                        .then(literal("at").then(argument("position", Vec3Argument.vec3())
                                .executes(c -> batchAp(c, ap -> ap.lookAt(Vec3Argument.getVec3(c, "position"))))))
                        .then(argument("direction", RotationArgument.rotation())
                                .executes(c -> batchAp(c, ap -> ap.look(RotationArgument.getRotation(c, "direction").getRotation(c.getSource()))))))
                .then(literal("turn")
                        .then(literal("left").executes(batch(ap -> ap.turn(-90, 0))))
                        .then(literal("right").executes(batch(ap -> ap.turn(90, 0))))
                        .then(literal("back").executes(batch(ap -> ap.turn(180, 0))))
                        .then(argument("rotation", RotationArgument.rotation())
                                .executes(c -> batchAp(c, ap -> ap.turn(RotationArgument.getRotation(c, "rotation").getRotation(c.getSource()))))))
                .then(literal("move").executes(batch(EntityPlayerActionPack::stopMovement))
                        .then(literal("forward").executes(batch(ap -> ap.setForward(1))))
                        .then(literal("backward").executes(batch(ap -> ap.setForward(-1))))
                        .then(literal("left").executes(batch(ap -> ap.setStrafing(1))))
                        .then(literal("right").executes(batch(ap -> ap.setStrafing(-1)))))
                .then(literal("sit").executes(BotAllCommand::sitAll))
                .then(literal("stand").executes(BotAllCommand::standAll));
        dispatcher.register(command);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> actionCommand(String name, EntityPlayerActionPack.ActionType type) {
        return literal(name)
                .executes(batch(ap -> ap.start(type, EntityPlayerActionPack.Action.once())))
                .then(literal("once").executes(batch(ap -> ap.start(type, EntityPlayerActionPack.Action.once()))))
                .then(literal("continuous").executes(batch(ap -> ap.start(type, EntityPlayerActionPack.Action.continuous()))))
                .then(literal("interval").then(argument("ticks", IntegerArgumentType.integer(1))
                        .executes(c -> batchAp(c, ap -> ap.start(type, EntityPlayerActionPack.Action.interval(IntegerArgumentType.getInteger(c, "ticks")))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dropCommand(String name, boolean dropAll) {
        return literal(name)
                .then(literal("all").executes(batch(ap -> ap.drop(-2, dropAll))))
                .then(literal("mainhand").executes(batch(ap -> ap.drop(-1, dropAll))))
                .then(literal("offhand").executes(batch(ap -> ap.drop(40, dropAll))))
                .then(argument("slot", IntegerArgumentType.integer(0, 40))
                        .executes(c -> batchAp(c, ap -> ap.drop(IntegerArgumentType.getInteger(c, "slot"), dropAll))));
    }

    /** 批量执行一个 action pack 操作。 */
    private static int batchAp(CommandContext<CommandSourceStack> context, Consumer<EntityPlayerActionPack> action) {
        List<ServerPlayer> bots = collectBots(context.getSource());
        for (ServerPlayer bot : bots) {
            action.accept(((ServerPlayerInterface) bot).getActionPack());
        }
        final int n = bots.size();
        context.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal("[SLSA] 已对 " + n + " 个假人执行操作"), false);
        return n;
    }

    private static com.mojang.brigadier.Command<CommandSourceStack> batch(Consumer<EntityPlayerActionPack> action) {
        return c -> batchAp(c, action);
    }

    /** 收集所有本模组生成的假人（受名单限制：名单非空时只取名单内的）。 */
    private static List<ServerPlayer> collectBots(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        List<ServerPlayer> bots = new ArrayList<>();
        boolean filterByList = !com.github.zly2006.carpetslsaddition.util.BotListManager.isEmpty();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player instanceof EntityPlayerMPFake && ((SLSBotAccessor) player).carpet_SLS_Addition$isBot()) {
                if (filterByList && !com.github.zly2006.carpetslsaddition.util.BotListManager.contains(player.getScoreboardName())) {
                    continue;
                }
                bots.add(player);
            }
        }
        return bots;
    }
    private static int sitAll(CommandContext<CommandSourceStack> context) {
        if (!SLSCarpetSettings.canUseSitCommand) {
            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                    "[SLSA] 坐下功能未启用（canUseSitCommand）"), false);
            return 0;
        }
        List<ServerPlayer> bots = collectBots(context.getSource());
        int ok = 0;
        for (ServerPlayer bot : bots) {
            if (SitCommand.sitPlayer(bot)) ok++;
        }
        final int done = ok;
        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "[SLSA] 已让 " + done + "/" + bots.size() + " 个假人坐下"), false);
        return ok;
    }

    private static int standAll(CommandContext<CommandSourceStack> context) {
        List<ServerPlayer> bots = collectBots(context.getSource());
        int ok = 0;
        for (ServerPlayer bot : bots) {
            if (bot.getVehicle() != null) {
                bot.stopRiding();
                ok++;
            }
        }
        final int done = ok;
        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "[SLSA] 已让 " + done + "/" + bots.size() + " 个假人站起"), false);
        return ok;
    }
}