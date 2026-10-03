package com.github.zly2006.carpetslsaddition.mixin.turtle;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.turtle.NestingSiteFinder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.animal.turtle.Turtle$TurtleLayEggGoal")
public abstract class TurtleLayEggGoalMixin extends MoveToBlockGoal {
	@Unique private static final int PATH_SEARCH_RANGE = 48;
	@Unique private static final int PATH_ATTEMPTS_PER_CHECK = 16;

	@Shadow @Final
	private Turtle turtle;
	@Unique private List<NestingSiteFinder.Candidate> turtleReproduce$candidates = List.of();
	@Unique private int turtleReproduce$candidateIndex;
	@Unique private BlockPos turtleReproduce$searchOrigin = BlockPos.ZERO;
	@Unique private Path turtleReproduce$nestingPath;

	protected TurtleLayEggGoalMixin(PathfinderMob mob, double speed, int range) {
		super(mob, speed, range);
	}

	@Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
	private void turtleReproduce$findLocalShore(CallbackInfoReturnable<Boolean> cir) {
		if (!SLSCarpetSettings.turtleLocalNesting) return;
		cir.setReturnValue(this.turtle.hasEgg() && !this.turtle.isBaby() && super.canUse());
	}

	@Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
	private void turtleReproduce$continueAwayFromHome(CallbackInfoReturnable<Boolean> cir) {
		if (!SLSCarpetSettings.turtleLocalNesting) return;
		cir.setReturnValue(this.turtle.hasEgg() && super.canContinueToUse());
	}

	@Inject(method = "isValidTarget", at = @At("HEAD"), cancellable = true)
	private void turtleReproduce$requireDrySandNearWater(LevelReader level, BlockPos pos,
			CallbackInfoReturnable<Boolean> cir) {
		if (!SLSCarpetSettings.turtleLocalNesting) return;
		cir.setReturnValue(NestingSiteFinder.isValidSite(level, pos));
	}

	@Override
	protected boolean findNearestBlock() {
		if (!SLSCarpetSettings.turtleLocalNesting) {
			return super.findNearestBlock();
		}
		if (this.turtleReproduce$candidateIndex >= this.turtleReproduce$candidates.size()
				|| this.turtle.blockPosition().distSqr(this.turtleReproduce$searchOrigin) > 16.0) {
			this.turtleReproduce$candidates = NestingSiteFinder.findCandidates(this.turtle);
			this.turtleReproduce$candidateIndex = 0;
			this.turtleReproduce$searchOrigin = this.turtle.blockPosition();
		}

		// Spread expensive path checks over AI updates when a shore is inaccessible.
		int attempts = 0;
		while (this.turtleReproduce$candidateIndex < this.turtleReproduce$candidates.size()
				&& attempts++ < PATH_ATTEMPTS_PER_CHECK) {
			BlockPos sand = this.turtleReproduce$candidates.get(this.turtleReproduce$candidateIndex++).sandPos();
			if (!this.turtle.isWithinHome(sand) || !NestingSiteFinder.isValidSite(this.turtle.level(), sand)) {
				continue;
			}
			Vec3 destination = Vec3.atBottomCenterOf(sand.above());
			AABB destinationBounds = this.turtle.getBoundingBox().move(destination.subtract(this.turtle.position()));
			// Adult turtles are wider than one block. An air block alone does not
			// guarantee that their body is clear of adjacent water or walls.
			if (!this.turtle.level().noCollision(this.turtle, destinationBounds)
					|| this.turtle.level().containsAnyLiquid(destinationBounds.deflate(0.001))) {
				continue;
			}
			Path path = this.turtleReproduce$createNestingPath(sand.above());
			if (path != null && path.canReach()) {
				this.blockPos = sand;
				this.turtleReproduce$nestingPath = path;
				this.turtleReproduce$candidates = List.of();
				return true;
			}
		}
		if (this.turtleReproduce$candidateIndex < this.turtleReproduce$candidates.size()) {
			this.nextStartTick = 1;
		}
		return false;
	}

	@Unique
	private Path turtleReproduce$createNestingPath(BlockPos destination) {
		PathNavigation navigation = this.turtle.getNavigation();
		float previousMultiplier = ((PathNavigationAccessor) navigation).turtleReproduce$getMaxVisitedNodesMultiplier();
		// A larger distance alone leaves vanilla's small node budget unchanged,
		// incorrectly rejecting shores that require a detour around an obstacle.
		navigation.setMaxVisitedNodesMultiplier(Math.max(previousMultiplier, 8.0F));
		try {
			return navigation.createPath(destination, 0, PATH_SEARCH_RANGE);
		} finally {
			navigation.setMaxVisitedNodesMultiplier(previousMultiplier);
		}
	}

	@Override
	protected void moveMobToBlock() {
		if (!SLSCarpetSettings.turtleLocalNesting) {
			super.moveMobToBlock();
			return;
		}
		this.turtle.getNavigation().moveTo(this.turtleReproduce$nestingPath, this.speedModifier);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void turtleReproduce$retryLongerPath(CallbackInfo ci) {
		if (!SLSCarpetSettings.turtleLocalNesting) return;
		if (!this.isReachedTarget() && this.tryTicks % 40 == 0 && this.turtle.getNavigation().isDone()) {
			this.turtleReproduce$nestingPath = this.turtleReproduce$createNestingPath(this.blockPos.above());
			this.moveMobToBlock();
		}
	}

	@Override
	public void stop() {
		super.stop();
		if (!SLSCarpetSettings.turtleLocalNesting) return;
		this.turtle.getNavigation().stop();
		((TurtleAccessor) this.turtle).turtleReproduce$setLayingEgg(false);
		this.turtleReproduce$nestingPath = null;
		this.turtleReproduce$candidates = List.of();
	}
}
