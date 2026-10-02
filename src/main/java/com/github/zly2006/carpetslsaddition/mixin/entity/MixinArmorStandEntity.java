package com.github.zly2006.carpetslsaddition.mixin.entity;

import com.github.zly2006.carpetslsaddition.util.SitEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStand.class)
public abstract class MixinArmorStandEntity extends LivingEntity implements SitEntity {
    private boolean sitEntity = false;

    protected MixinArmorStandEntity(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow
    protected abstract void setMarker(boolean marker);

    @Override
    public boolean isSitEntity() {
        return sitEntity;
    }

    @Override
    public void setSitEntity(boolean isSitEntity) {
        this.sitEntity = isSitEntity;
        this.setMarker(isSitEntity);
        this.setInvisible(isSitEntity);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        if (this.isSitEntity()) {
            this.setPos(this.getX(), this.getY() + 0.16, this.getZ());
            this.kill((net.minecraft.server.level.ServerLevel) this.level());
        }
        super.removePassenger(passenger);
    }

    @Inject(method = "addAdditionalSaveData", at = @At(value = "RETURN"))
    private void postWriteCustomDataToNbt(ValueOutput nbt, CallbackInfo ci) {
        if (this.sitEntity) {
            nbt.putBoolean("SitEntity", true);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At(value = "RETURN"))
    private void postReadCustomDataFromNbt(ValueInput nbt, CallbackInfo ci) {
        if (nbt.getBooleanOr("SitEntity", false)) {
            this.sitEntity = true;
        }
    }
}