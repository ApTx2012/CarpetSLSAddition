package com.github.zly2006.carpetslsaddition.mixin.turtle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.turtle.Turtle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Turtle.class)
public interface TurtleAccessor {
	@Invoker("setHasEgg")
	void turtleReproduce$setHasEgg(boolean hasEgg);

	@Invoker("setLayingEgg")
	void turtleReproduce$setLayingEgg(boolean layingEgg);

	@Accessor("homePos")
	BlockPos turtleReproduce$getHomePos();

	@Accessor("goingHome")
	boolean turtleReproduce$isGoingHome();
}
