package com.github.zly2006.carpetslsaddition.mixin.item;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 载具堆叠（仅玩家背包）：矿车/船在【玩家背包】内可堆叠到 64，其他容器不堆叠。
 *
 * <p>原理：{@code Slot.getMaxStackSize(ItemStack)} = min(容器上限, 物品上限)。
 * 载具的物品上限全局为 1（默认不叠）。本 mixin 在【容器是玩家背包 Inventory】且物品是载具时，
 * 直接返回 64，绕过 min 限制。
 *
 * <p>这样只有玩家背包（物品栏 36 格）内能叠，箱子/漏斗/潜影盒等其他容器不受影响。
 */
@Mixin(Slot.class)
public abstract class SlotMixin {

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void stackableVehiclesInPlayerInventory(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (!SLSCarpetSettings.stackableVehicles) {
            return;
        }
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Slot self = (Slot) (Object) this;
        // 仅玩家背包
        if (!(self.container instanceof Inventory)) {
            return;
        }
        Item item = stack.getItem();
        if (item instanceof MinecartItem || item instanceof BoatItem) {
            cir.setReturnValue(64);
        }
    }
}