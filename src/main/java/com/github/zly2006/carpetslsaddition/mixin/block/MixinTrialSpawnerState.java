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
    // trialSpawnerCD：覆盖试炼刷怪笼的冷却长度。
    // 新版 TrialSpawnerState.tickAndGetNext 里调用 TrialSpawner.getTargetCooldownLength() 三次，
    // 不指定 ordinal 以覆盖全部调用。
    @WrapOperation(
            method = "tickAndGetNext",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/trialspawner/TrialSpawner;getTargetCooldownLength()I"
            )
    )
    private int tick(TrialSpawner instance, Operation<Integer> original) {
        if (SLSCarpetSettings.trialSpawnerCD < 0) {
            return original.call(instance);
        } else {
            return SLSCarpetSettings.trialSpawnerCD;
        }
    }
}