package com.github.zly2006.carpetslsaddition.mixin.turtle;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.world.entity.animal.turtle.Turtle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.animal.turtle.Turtle$TurtleGoHomeGoal")
public abstract class TurtleGoHomeGoalMixin {
	@Shadow @Final
	private Turtle turtle;

	// This goal has no MOVE flag, so priority alone cannot stop it fighting nesting.
	@Inject(method = {"canUse", "canContinueToUse"}, at = @At("HEAD"), cancellable = true)
	private void turtleReproduce$stayLocalWhileCarryingEggs(CallbackInfoReturnable<Boolean> cir) {
		if (SLSCarpetSettings.turtleLocalNesting && this.turtle.hasEgg()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "stop", at = @At("TAIL"))
	private void turtleReproduce$discardOldHomeRoute(CallbackInfo ci) {
		if (SLSCarpetSettings.turtleLocalNesting && this.turtle.hasEgg()) {
			// The vanilla stop only clears goingHome, leaving its last route active.
			this.turtle.getNavigation().stop();
		}
	}
}