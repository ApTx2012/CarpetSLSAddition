package com.github.zly2006.carpetslsaddition.mixin.shulker;

import com.github.zly2006.carpetslsaddition.util.ShulkerBoxItemUtil;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractContainerMenu.class)
public abstract class MixinScreenHandler {
    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"
            ),
            require = 0
    )
    private boolean checkEqual(ItemStack stack, ItemStack otherStack) {
        if (ShulkerBoxItemUtil.shulkerBoxEqual(stack, otherStack)) return true;
        return ItemStack.isSameItemSameComponents(stack, otherStack);
    }

    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;isStackable()Z"
            ),
            require = 0
    )
    private boolean isStackable(ItemStack stack) {
        if (ShulkerBoxItemUtil.isEmptyShulkerBoxItem(stack)) return true;
        return stack.isStackable();
    }
}