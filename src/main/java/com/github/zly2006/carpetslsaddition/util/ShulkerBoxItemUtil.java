package com.github.zly2006.carpetslsaddition.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class ShulkerBoxItemUtil {
    public static final int SHULKERBOX_MAX_STACK_AMOUNT = 64;

    public static boolean shulkerBoxEqual(ItemStack stack, ItemStack otherStack) {
        if (ShulkerBoxItemUtil.isEmptyShulkerBoxItem(stack) && ShulkerBoxItemUtil.isEmptyShulkerBoxItem(otherStack)) {
            if (stack.isEmpty() && otherStack.isEmpty()) {
                return true;
            }
            return stack.getComponents().equals(otherStack.getComponents());
        }
        return ItemStack.isSameItemSameComponents(stack, otherStack);
    }

    public static boolean isEmptyShulkerBoxItem(ItemStack itemStack) {
        if (itemStack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            ItemContainerContents container = itemStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            try {
                // newer MC API
                return container.allItemsCopyStream().findAny().isEmpty();
            } catch (NoSuchMethodError e) {
                // fallback for older MC versions
                try {
                    java.lang.reflect.Method streamMethod = container.getClass().getMethod("stream");
                    @SuppressWarnings("unchecked")
                    java.util.stream.Stream<ItemStack> stream = (java.util.stream.Stream<ItemStack>) streamMethod.invoke(container);
                    return !stream.findAny().isPresent();
                } catch (Throwable t) {
                    return false;
                }
            }
        }
        return false;
    }
}