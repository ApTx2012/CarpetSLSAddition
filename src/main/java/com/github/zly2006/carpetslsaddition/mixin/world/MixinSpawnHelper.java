package com.github.zly2006.carpetslsaddition.mixin.world;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.world.level.NaturalSpawner.class)
public class MixinSpawnHelper {
    @Redirect(method = "getMobForSpawn", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"
    ))
    private static Entity onCreateMob(EntityType<?> type, Level world, EntitySpawnReason reason) {
        if (SLSCarpetSettings.noBatSpawning && type == EntityType.BAT) {
            return null;
        }
        return type.create(world, reason);
    }
}