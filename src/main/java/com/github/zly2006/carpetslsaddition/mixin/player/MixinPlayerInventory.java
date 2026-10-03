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
    // creativeNoInfinitePickup：创造模式下满背包不能拾取。
    // 新版 Inventory.add 在满背包 + hasInfiniteMaterials 时把数量清 0 并返回 true。
    // 规则开启时改为 false，避免"假拾取"。
    @Redirect(
            method = "add(ILnet/minecraft/world/item/ItemStack;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;hasInfiniteMaterials()Z")
    )
    private boolean insertStack(Player instance) {
        if (SLSCarpetSettings.creativeNoInfinitePickup) {
            return false;
        }
        return instance.hasInfiniteMaterials();
    }
}