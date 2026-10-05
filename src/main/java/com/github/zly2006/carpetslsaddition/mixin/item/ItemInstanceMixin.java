package com.github.zly2006.carpetslsaddition.mixin.item;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 载具堆叠：让矿车/船的物品可以堆叠到 64。
 *
 * <p>26.1.2 的堆叠上限来自 {@code ItemInstance.getMaxStackSize()} 的 default 实现：
 * 读取 {@code DataComponents.MAX_STACK_SIZE} 组件，默认 1（故矿车/船默认不堆叠）。
 *
 * <p>本 mixin 注入该 default 方法：规则 {@code stackableVehicles} 开启时，
 * 若物品是矿车（{@link MinecartItem}）或船（{@link BoatItem}），返回 64。
 * 由于堆叠上限变大，同类载具在拾取/整理时会自动合并，实现"自动堆叠"。
 */
@Mixin(ItemInstance.class)
public interface ItemInstanceMixin {

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void stackableVehicles(CallbackInfoReturnable<Integer> cir) {
        if (!SLSCarpetSettings.stackableVehicles) {
            return;
        }
        if (!((Object) this instanceof ItemStack stack)) {
            return;
        }
        Item item = stack.getItem();
        if (item instanceof MinecartItem || item instanceof BoatItem) {
            cir.setReturnValue(64);
        }
    }
}