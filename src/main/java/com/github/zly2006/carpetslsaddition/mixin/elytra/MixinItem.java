package com.github.zly2006.carpetslsaddition.mixin.elytra;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.1 起，配方剩余物改为 Item 的 craftingRemainingItem 字段。
 * 当 elytraCraftable 开启时，把 ELYTRA 的剩余物设为其自身。
 *
 * TODO(迁移): 该字段为 final，此处仅在 Item 构造后条件性覆盖。
 *  若需运行期动态开关，需改为在配方匹配阶段判断。
 */
@Mixin(Item.class)
public abstract class MixinItem {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        Item self = (Item) (Object) this;
        if (SLSCarpetSettings.elytraCraftable && self == Items.ELYTRA) {
            ((ItemAccessor) this).carpet_SLS_Addition$setCraftingRemainingItem(new ItemStackTemplate(self));
        }
    }
}