package com.github.zly2006.carpetslsaddition.mixin.block;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TrialSpawnerStateData.class)
public class MixinTrialSpawnerData {
    @WrapOperation(
            method = "reset",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/trialspawner/TrialSpawnerConfig;ticksBetweenItemSpawners()J",
                    ordinal = 0
            ),
            require = 0
    )
    private long hackCooldown(TrialSpawnerConfig instance, Operation<Long> original) {
        if (SLSCarpetSettings.trialSpawnerCD < 0) {
            return original.call(instance);
        } else {
            return SLSCarpetSettings.trialSpawnerCD;
        }
    }
}