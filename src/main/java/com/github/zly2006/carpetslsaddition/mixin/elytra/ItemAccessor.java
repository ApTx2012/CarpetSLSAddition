package com.github.zly2006.carpetslsaddition.mixin.elytra;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Item.class)
public interface ItemAccessor {
    @Mutable
    @Accessor("craftingRemainingItem")
    void carpet_SLS_Addition$setCraftingRemainingItem(ItemStackTemplate item);
}