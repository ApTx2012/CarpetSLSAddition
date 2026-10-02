package com.github.zly2006.carpetslsaddition.mixin.shulker;

import com.github.zly2006.carpetslsaddition.util.ShulkerBoxItemUtil;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class MixinItemStack {
    @Inject(
            method = "limitSize",
            at = @At("HEAD"),
            cancellable = true
    )
    private void capCount(int maxCount, CallbackInfo ci) {
        if (ShulkerBoxItemUtil.isEmptyShulkerBoxItem((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }
}