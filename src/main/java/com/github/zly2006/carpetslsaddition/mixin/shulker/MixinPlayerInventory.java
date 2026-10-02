package com.github.zly2006.carpetslsaddition.mixin.shulker;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.util.ShulkerBoxItemUtil;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 空潜影盒可堆叠（上限 64）。
 * 26.1 起堆叠上限统一由 ItemInstance.getMaxStackSize 决定，参考 Carpet 实现。
 */
@Mixin(ItemInstance.class)
public interface MixinPlayerInventory {
    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void getMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        if (SLSCarpetSettings.emptyShulkerBoxStack
                && ((ItemInstance) (Object) this) instanceof ItemStack itemStack
                && ShulkerBoxItemUtil.isEmptyShulkerBoxItem(itemStack)) {
            cir.setReturnValue(ShulkerBoxItemUtil.SHULKERBOX_MAX_STACK_AMOUNT);
        }
    }
}