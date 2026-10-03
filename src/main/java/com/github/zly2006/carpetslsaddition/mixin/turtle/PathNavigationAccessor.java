package com.github.zly2006.carpetslsaddition.mixin.turtle;

import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PathNavigation.class)
public interface PathNavigationAccessor {
	@Accessor("maxVisitedNodesMultiplier")
	float turtleReproduce$getMaxVisitedNodesMultiplier();
}
