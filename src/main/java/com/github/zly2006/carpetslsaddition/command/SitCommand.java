package com.github.zly2006.carpetslsaddition.command;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.util.SitEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;

public class SitCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sit")
                .executes(context -> {
                    if (!SLSCarpetSettings.canUseSitCommand) {
                        return 0;
                    }
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        return 0;
                    }
                    sitPlayer(player);
                    return 1;
                })
        );
    }

    /**
     * 让指定玩家原地坐下（生成一个标记为 SitEntity 的 ArmorStand 并骑乘）。
     *
     * @return 若成功坐下返回 true，否则返回 false（如已在骑乘/不在地面）。
     */
    public static boolean sitPlayer(ServerPlayer player) {
        if (player.getVehicle() != null || !player.onGround()) {  // 防止错误的坐下行为
            return false;
        }
        ServerLevel world = (ServerLevel) player.level();
        ArmorStand armorStandEntity = new ArmorStand(world, player.getX(), player.getY(), player.getZ());
        ((SitEntity) armorStandEntity).setSitEntity(true);
        world.addFreshEntity(armorStandEntity);
        player.setShiftKeyDown(false);
        player.startRiding(armorStandEntity);
        return true;
    }
}