package com.github.zly2006.carpetslsaddition.command;

import carpet.CarpetSettings;
import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import carpet.patches.FakeClientConnection;
import carpet.utils.Messenger;
import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.ServerMain;
import com.github.zly2006.carpetslsaddition.util.access.PlayerAccessor;
import com.github.zly2006.carpetslsaddition.util.access.SLSBotAccessor;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class BotCommand {
    private static boolean hasLevel(CommandSourceStack source, int level) {
        var perm = switch (level) {
            case 0 -> net.minecraft.commands.Commands.LEVEL_ALL;
            case 1 -> net.minecraft.commands.Commands.LEVEL_MODERATORS;
            case 2 -> net.minecraft.commands.Commands.LEVEL_GAMEMASTERS;
            case 3 -> net.minecraft.commands.Commands.LEVEL_ADMINS;
            default -> net.minecraft.commands.Commands.LEVEL_OWNERS;
        };
        return perm.check(source.permissions());
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("bot")
                .requires(source -> hasLevel(source, 4) || Permissions.check(source, "slsaddition.command.bot"))
                .then(argument("player", StringArgumentType.word())
                        .suggests((c, b) -> suggest(getPlayerSuggestions(c.getSource()), b))
                        .then(literal("stop").executes(manipulation(EntityPlayerActionPack::stopAll)))
                        .then(makeActionCommand("use", EntityPlayerActionPack.ActionType.USE))
                        .then(makeActionCommand("jump", EntityPlayerActionPack.ActionType.JUMP))
                        .then(makeActionCommand("attack", EntityPlayerActionPack.ActionType.ATTACK))
                        .then(makeActionCommand("drop", EntityPlayerActionPack.ActionType.DROP_ITEM))
                        .then(makeDropCommand("drop", false))
                        .then(makeActionCommand("dropStack", EntityPlayerActionPack.ActionType.DROP_STACK))
                        .then(makeDropCommand("dropStack", true))
                        .then(makeActionCommand("swapHands", EntityPlayerActionPack.ActionType.SWAP_HANDS))
                        .then(literal("hotbar")
                                .then(argument("slot", IntegerArgumentType.integer(1, 9))
                                        .executes(c -> manipulate(c, ap -> ap.setSlot(IntegerArgumentType.getInteger(c, "slot"))))))
                        .then(literal("kill").executes(BotCommand::kill))
                        .then(literal("respawn").executes(BotCommand::respawn))
                        .then(literal("mount").executes(manipulation(ap -> ap.mount(true)))
                                .then(literal("anything").executes(manipulation(ap -> ap.mount(false)))))
                        .then(literal("dismount").executes(manipulation(EntityPlayerActionPack::dismount)))
                        .then(literal("sneak").executes(manipulation(ap -> ap.setSneaking(true))))
                        .then(literal("unsneak").executes(manipulation(ap -> ap.setSneaking(false))))
                        .then(literal("sprint").executes(manipulation(ap -> ap.setSprinting(true))))
                        .then(literal("unsprint").executes(manipulation(ap -> ap.setSprinting(false))))
                        .then(literal("look")
                                .then(literal("north").executes(manipulation(ap -> ap.look(Direction.NORTH))))
                                .then(literal("south").executes(manipulation(ap -> ap.look(Direction.SOUTH))))
                                .then(literal("east").executes(manipulation(ap -> ap.look(Direction.EAST))))
                                .then(literal("west").executes(manipulation(ap -> ap.look(Direction.WEST))))
                                .then(literal("up").executes(manipulation(ap -> ap.look(Direction.UP))))
                                .then(literal("down").executes(manipulation(ap -> ap.look(Direction.DOWN))))
                                .then(literal("at").then(argument("position", Vec3Argument.vec3())
                                        .executes(c -> manipulate(c, ap -> ap.lookAt(Vec3Argument.getVec3(c, "position"))))))
                                .then(argument("direction", RotationArgument.rotation())
                                        .executes(c -> manipulate(c, ap -> ap.look(RotationArgument.getRotation(c, "direction").getRotation(c.getSource()))))))
                        .then(literal("turn")
                                .then(literal("left").executes(manipulation(ap -> ap.turn(-90, 0))))
                                .then(literal("right").executes(manipulation(ap -> ap.turn(90, 0))))
                                .then(literal("back").executes(manipulation(ap -> ap.turn(180, 0))))
                                .then(argument("rotation", RotationArgument.rotation())
                                        .executes(c -> manipulate(c, ap -> ap.turn(RotationArgument.getRotation(c, "rotation").getRotation(c.getSource()))))))
                        .then(literal("move").executes(manipulation(EntityPlayerActionPack::stopMovement))
                                .then(literal("forward").executes(manipulation(ap -> ap.setForward(1))))
                                .then(literal("backward").executes(manipulation(ap -> ap.setForward(-1))))
                                .then(literal("left").executes(manipulation(ap -> ap.setStrafing(1))))
                                .then(literal("right").executes(manipulation(ap -> ap.setStrafing(-1)))))
                        .then(literal("spawn").executes(BotCommand::spawn)
                                .then(literal("in").requires((player) -> hasLevel(player, 2))
                                        .then(argument("gamemode", GameModeArgument.gameMode())
                                                .executes(BotCommand::spawn)))
                                .then(literal("at").then(argument("position", Vec3Argument.vec3()).executes(BotCommand::spawn)
                                        .then(literal("facing").then(argument("direction", RotationArgument.rotation()).executes(BotCommand::spawn)
                                                .then(literal("in").then(argument("dimension", DimensionArgument.dimension()).executes(BotCommand::spawn)
                                                        .then(literal("in").requires((player) -> hasLevel(player, 2))
                                                                .then(argument("gamemode", GameModeArgument.gameMode())
                                                                        .executes(BotCommand::spawn)))))
                                        ))))
                        )
                );
        dispatcher.register(command);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> makeActionCommand(String actionName, EntityPlayerActionPack.ActionType type)
    {
        return literal(actionName)
                .executes(manipulation(ap -> ap.start(type, EntityPlayerActionPack.Action.once())))
                .then(literal("once").executes(manipulation(ap -> ap.start(type, EntityPlayerActionPack.Action.once()))))
                .then(literal("continuous").executes(manipulation(ap -> ap.start(type, EntityPlayerActionPack.Action.continuous()))))
                .then(literal("interval").then(argument("ticks", IntegerArgumentType.integer(1))
                        .executes(c -> manipulate(c, ap -> ap.start(type, EntityPlayerActionPack.Action.interval(IntegerArgumentType.getInteger(c, "ticks")))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> makeDropCommand(String actionName, boolean dropAll)
    {
        return literal(actionName)
                .then(literal("all").executes(manipulation(ap -> ap.drop(-2, dropAll))))
                .then(literal("mainhand").executes(manipulation(ap -> ap.drop(-1, dropAll))))
                .then(literal("offhand").executes(manipulation(ap -> ap.drop(40, dropAll))))
                .then(argument("slot", IntegerArgumentType.integer(0, 40)).
                        executes(c -> manipulate(c, ap -> ap.drop(IntegerArgumentType.getInteger(c, "slot"), dropAll))));
    }

    private static Collection<String> getPlayerSuggestions(CommandSourceStack source)
    {
        Set<String> players = new LinkedHashSet<>(List.of("Steve", "Alex"));
        players.addAll(List.of(source.getServer().getPlayerNames()));
        return players;
    }

    private static ServerPlayer getPlayer(CommandContext<CommandSourceStack> context)
    {
        String playerName = StringArgumentType.getString(context, "player");
        MinecraftServer server = context.getSource().getServer();
        return server.getPlayerList().getPlayerByName(playerName);
    }

    private static boolean cantManipulate(CommandContext<CommandSourceStack> context)
    {
        Player player = getPlayer(context);
        CommandSourceStack source = context.getSource();
        if (player == null)
        {
            Messenger.m(source, "r Can only manipulate existing players");
            return true;
        }
        Player sender = source.getPlayer();
        if (sender == null)
        {
            return false;
        }

        if (!source.getServer().getPlayerList().isOp(sender.nameAndId()))
        {
            if (sender != player && !(player instanceof EntityPlayerMPFake))
            {
                Messenger.m(source, "r Non OP players can't control other real players");
                return true;
            }
        }
        return false;
    }

    private static boolean cantReMove(CommandContext<CommandSourceStack> context)
    {
        if (cantManipulate(context)) return true;
        Player player = getPlayer(context);
        if (player instanceof EntityPlayerMPFake) return false;
        Messenger.m(context.getSource(), "r Only fake players can be moved or killed");
        return true;
    }

    private static boolean cantSpawn(CommandContext<CommandSourceStack> context)
    {
        String playerName = getBotPrefix() + StringArgumentType.getString(context, "player");
        MinecraftServer server = context.getSource().getServer();
        PlayerList manager = server.getPlayerList();

        if (manager.getPlayerByName(playerName) != null)
        {
            Messenger.m(context.getSource(), "r Player ", "rb " + playerName, "r  is already logged on");
            return true;
        }
        GameProfile profile = server.services().nameToIdCache().get(playerName)
                .map(n -> new GameProfile(n.id(), n.name())).orElse(null);
        if (profile == null)
        {
            if (!CarpetSettings.allowSpawningOfflinePlayers)
            {
                Messenger.m(context.getSource(), "r Player "+playerName+" is either banned by Mojang, or auth servers are down. " +
                        "Banned players can only be summoned in Singleplayer and in servers in off-line mode.");
                return true;
            } else {
                profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(playerName), playerName);
            }
        }
        if (manager.getBans().isBanned(new net.minecraft.server.players.NameAndId(profile)))
        {
            Messenger.m(context.getSource(), "r Player ", "rb " + playerName, "r  is banned on this server");
            return true;
        }
        if (manager.isUsingWhitelist() && manager.isWhiteListed(new net.minecraft.server.players.NameAndId(profile)) && !hasLevel(context.getSource(), 2))
        {
            Messenger.m(context.getSource(), "r Whitelisted players can only be spawned by operators");
            return true;
        }
        return false;
    }

    private static String getBotPrefix() {
        return SLSCarpetSettings.botPrefix.equals("#none") ? "" : SLSCarpetSettings.botPrefix;
    }

    private static int kill(CommandContext<CommandSourceStack> context)
    {
        if (cantReMove(context)) return 0;
        { var _p = getPlayer(context); _p.kill((ServerLevel) _p.level()); }
        return 1;
    }

    private static int respawn(CommandContext<CommandSourceStack> context) {
        var player = getPlayer(context);
        if (player instanceof EntityPlayerMPFake && ((SLSBotAccessor)player).carpet_SLS_Addition$isBot()) {
            ((SLSBotAccessor)player).carpet_SLS_Addition$setSpawnTime(System.currentTimeMillis());
            context.getSource().sendSuccess(
                    () -> Component.translatable("carpet.slsa.bot.respawned", player.getScoreboardName())
                            .setStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.GREEN)),
                    false
            );

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendSuccess(
                () -> Component.translatable("carpet.slsa.bot.not_a_bot", player.getScoreboardName())
                        .setStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.RED)),
                false
        );

        return 0;
    }

    @FunctionalInterface
    interface SupplierWithCSE<T>
    {
        T get() throws CommandSyntaxException;
    }

    private static <T> T getArgOrDefault(BotCommand.SupplierWithCSE<T> getter, T defaultValue) throws CommandSyntaxException
    {
        try
        {
            return getter.get();
        }
        catch (IllegalArgumentException e)
        {
            return defaultValue;
        }
    }

    private static int spawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        if (cantSpawn(context)) return 0;

        CommandSourceStack source = context.getSource();
        Vec3 pos = getArgOrDefault(
                () -> Vec3Argument.getVec3(context, "position"),
                source.getPosition()
        );
        Vec2 facing = getArgOrDefault(
                () -> RotationArgument.getRotation(context, "direction").getRotation(source),
                source.getRotation()
        );
        ResourceKey<Level> dimType = getArgOrDefault(
                () -> DimensionArgument.getDimension(context, "dimension").dimension(),
                source.getLevel().dimension()
        );

        String playerName = getBotPrefix() + StringArgumentType.getString(context, "player");
        if (playerName.length() > maxNameLength(source.getServer()))
        {
            Messenger.m(source, "rb Player name: " + playerName + " is too long");
            return 0;
        }

        if (!Level.isInSpawnableBounds(BlockPos.containing(pos)))
        {
            Messenger.m(source, "rb Player " + playerName + " cannot be placed outside of the world");
            return 0;
        }

        EntityPlayerMPFake bot = createBot(playerName, source.getServer(), pos, facing.y, facing.x, dimType);

        if (bot == null)
        {
            Messenger.m(source, "rb Player " + playerName + " doesn't exist and cannot spawn in online mode. " +
                    "Turn the server offline to spawn non-existing players");
            return 0;
        }

        ((PlayerAccessor) bot).carpet_SLS_Addition$setDisplayName(Component.empty().append(Component.literal("[%s] ".formatted(source.getTextName())).setStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.AQUA))).append(Component.literal(playerName).setStyle(Style.EMPTY)));

        ServerMain.server.getPlayerList().broadcastSystemMessage(Component.empty()
                .append(Component.literal("假人").setStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.GREEN)))
                .append(Component.literal(playerName).setStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.GOLD).withBold(true)))
                .append(Component.literal("由玩家").setStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.GREEN)))
                .append(source.getDisplayName())
                .append(Component.literal("召唤！").setStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.GREEN))), false);

        ServerMain.server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                bot
        ));
        return 1;
    }

    @SuppressWarnings("unchecked")
    private static EntityPlayerMPFake createBot(String username, MinecraftServer server, Vec3 pos, double yaw, double pitch, ResourceKey<Level> dimensionId) {
        ServerLevel worldIn = server.getLevel(dimensionId);
        server.services().nameToIdCache().resolveOfflineUsers(false);
        GameProfile gameprofile;
        try {
            var nameAndId = server.services().nameToIdCache().get(username).orElse(null);
            gameprofile = nameAndId == null ? null : new GameProfile(nameAndId.id(), nameAndId.name());
        } finally {
            server.services().nameToIdCache().resolveOfflineUsers(server.isDedicatedServer() && server.usesAuthentication());
        }
        if (gameprofile == null)
        {
            if (!CarpetSettings.allowSpawningOfflinePlayers)
            {
                return null;
            } else {
                gameprofile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(username), username);
            }
        }

        // 孩子不懂，用反射写着玩的，报错了记得随Carpet一起升级一下
        try {
            Class<EntityPlayerMPFake> fakePlayerClass = (Class<EntityPlayerMPFake>)Class.forName("carpet.patches.EntityPlayerMPFake");
            Constructor<EntityPlayerMPFake> constructor = fakePlayerClass.getDeclaredConstructor(
                    MinecraftServer.class,
                    ServerLevel.class,
                    GameProfile.class,
                    ClientInformation.class,
                    boolean.class
            );
            constructor.setAccessible(true);
            EntityPlayerMPFake bot = constructor.newInstance(server, worldIn, gameprofile, ClientInformation.createDefault(), false);

            bot.fixStartingPosition = () -> bot.snapTo(pos.x, pos.y, pos.z, (float) yaw, (float) pitch);
            server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), bot, new CommonListenerCookie(gameprofile, 0, bot.clientInformation(), false));

            bot.teleportTo(worldIn, pos.x, pos.y, pos.z, Set.of(), (float) yaw, (float) pitch, true);
            bot.setHealth(20.0F);
            bot.unsetRemoved();
            bot.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);
            bot.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);

            server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(bot, (byte) (bot.yHeadRot * 256 / 360)), dimensionId);
            server.getPlayerList().broadcastAll(ClientboundEntityPositionSyncPacket.of(bot), dimensionId);

            bot.entityData.set(ServerPlayer.DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f); // show all model layers (incl. capes)
            bot.getAbilities().flying = false;

            ((SLSBotAccessor)bot).carpet_SLS_Addition$setBot(true);
            ((SLSBotAccessor)bot).carpet_SLS_Addition$setSpawnTime(System.currentTimeMillis());

            return bot;
        }
        catch (ClassNotFoundException | NoSuchMethodException  | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("An error has occurred when trying spawn a new bot with reflection", e);
        }
    }

    private static int maxNameLength(MinecraftServer server)
    {
        return server.getPort() >= 0 ? 16 : 40;
    }

    private static int manipulate(CommandContext<CommandSourceStack> context, Consumer<EntityPlayerActionPack> action)
    {
        if (cantManipulate(context)) return 0;
        ServerPlayer player = getPlayer(context);
        action.accept(((ServerPlayerInterface) player).getActionPack());
        return 1;
    }

    private static Command<CommandSourceStack> manipulation(Consumer<EntityPlayerActionPack> action)
    {
        return c -> manipulate(c, action);
    }
}