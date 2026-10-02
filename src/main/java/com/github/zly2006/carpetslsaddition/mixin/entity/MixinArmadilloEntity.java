package com.github.zly2006.carpetslsaddition.mixin.entity;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Armadillo.class)
public abstract class MixinArmadilloEntity extends Animal {
    protected MixinArmadilloEntity(EntityType<? extends Animal> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return SLSCarpetSettings.armadilloImmediateDespawns;
    }
}