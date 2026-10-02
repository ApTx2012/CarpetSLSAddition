package com.github.zly2006.carpetslsaddition.mixin.block;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TrialSpawnerState.class)
public class MixinTrialSpawnerState {
    @WrapOperation(
            method = "tickAndGetNext",
            at = @At(
                    value = "INVOKE",
                    ordinal = 0,
                    target = "Lnet/minecraft/world/level/block/entity/trialspawner/TrialSpawner;getTargetCooldownLength()I"
            )
    )
    private int tick(TrialSpawner instance, Operation<Integer> original)  {
        if (SLSCarpetSettings.trialSpawnerCD < 0) {
            return original.call(instance);
        } else {
            return SLSCarpetSettings.trialSpawnerCD;
        }
    }
}