package com.github.zly2006.carpetslsaddition.mixin.shulker;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.util.ShulkerBoxItemUtil;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerPlayNetworkHandler {
    @Redirect(
            method = "handleSetCreativeModeSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"
            ),
            require = 0
    )
    private int getMaxCount(ItemStack instance) {
        if (SLSCarpetSettings.emptyShulkerBoxStack && ShulkerBoxItemUtil.isEmptyShulkerBoxItem(instance)) {
            return ShulkerBoxItemUtil.SHULKERBOX_MAX_STACK_AMOUNT;
        }
        return instance.getMaxStackSize();
    }
}