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
                .requires(CommandSourceStack::isPlayer)
                .requires((commandSource) -> SLSCarpetSettings.canUseSitCommand)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    assert player != null;

                    if (player.getVehicle() != null || !player.onGround()) {  // 防止错误的坐下行为
                        return 1;
                    }

                    ServerLevel world = (ServerLevel) player.level();

                    ArmorStand armorStandEntity = new ArmorStand(world, player.getX(), player.getY(), player.getZ());
                    ((SitEntity) armorStandEntity).setSitEntity(true);
                    world.addFreshEntity(armorStandEntity);
                    player.setShiftKeyDown(false);
                    player.startRiding(armorStandEntity);

                    return 1;
                })
        );
    }
}