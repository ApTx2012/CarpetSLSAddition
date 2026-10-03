package com.github.zly2006.carpetslsaddition.mixin.world;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public class MixinSpawnHelper {
    // 注意：不能用 @Redirect EntityType.create —— 会和 Carpet 的 NaturalSpawnerMixin 冲突。
    // 改为在 getMobForSpawn 返回处拦截。
    @Inject(method = "getMobForSpawn", at = @At("HEAD"), cancellable = true)
    private static void onCreateMob(ServerLevel level, EntityType<?> type, CallbackInfoReturnable<Mob> cir) {
        if (SLSCarpetSettings.noBatSpawning && type == EntityType.BAT) {
            cir.setReturnValue(null);
        }
    }
}