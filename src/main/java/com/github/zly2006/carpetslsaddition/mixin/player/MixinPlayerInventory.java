package com.github.zly2006.carpetslsaddition.mixin.player;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Inventory.class)
public abstract class MixinPlayerInventory implements Container, Nameable {
    @Redirect(
            method = "add(ILnet/minecraft/world/item/ItemStack;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isCreative()Z"
            ),
            require = 0
    )
    private boolean insertStack(Player instance) {
        if (SLSCarpetSettings.creativeNoInfinitePickup) {
            return false;
        }
        return instance.isCreative();
    }
}