package com.github.zly2006.carpetslsaddition.command;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class HatCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hat")
                .requires(CommandSourceStack::isPlayer)
                .requires((commandSource) -> SLSCarpetSettings.canUseHatCommand)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    assert player != null;
                    ItemStack stack = player.getMainHandItem();
                    ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
                    player.setItemSlot(EquipmentSlot.HEAD, stack);
                    player.getInventory().setItem(player.getInventory().getSelectedSlot(), head);
                    player.containerMenu.sendAllDataToRemote();

                    return 1;
                })
        );
    }
}