package com.github.zly2006.carpetslsaddition.mixin.entity;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(FallingBlockEntity.class)
public abstract class MixinFallingBlockEntity extends Entity {

    @Shadow private BlockState blockState;

    @Shadow public abstract BlockPos getStartPos();

    public MixinFallingBlockEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "causeFallDamage", at = @At(value = "TAIL"))
    private void causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (!SLSCarpetSettings.obtainableReinforcedDeepSlate || !this.blockState.is(BlockTags.ANVIL)) {
            return;
        }

        Predicate<Entity> deepSlatePredicated = (entity) -> {
            if (entity instanceof ItemEntity itemEntity) {
                var itemStack = itemEntity.getItem();
                return itemStack.is(Items.DEEPSLATE) && itemStack.getCount() >= 64;
            }

            return false;
        };

        var world = this.level();
        var itemEntitiesList = world.getEntities(this, this.getBoundingBox(), deepSlatePredicated);
        if (itemEntitiesList.size() < 9) {
            return;
        }

        var entity = itemEntitiesList.getFirst();

        for (int i = 0; i < 9; i++) {
            itemEntitiesList.get(i).discard();
        }

        world.playSound(null, this.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

        var coreEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(Items.REINFORCED_DEEPSLATE, 1));
        coreEntity.setPickUpDelay(40);
        float f = this.random.nextFloat() * 0.5F;
        float g = this.random.nextFloat() * (float) (Math.PI * 2);
        coreEntity.setDeltaMovement(-Mth.sin(g) * f, 0.2F, Mth.cos(g) * f);
        world.addFreshEntity(coreEntity);
    }
}