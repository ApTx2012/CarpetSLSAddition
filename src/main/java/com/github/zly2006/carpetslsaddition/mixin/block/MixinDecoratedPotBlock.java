package com.github.zly2006.carpetslsaddition.mixin.block;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DecoratedPotBlock.class)
public class MixinDecoratedPotBlock {
    // sturdyDecoratedPot：投掷物命中不碎。
    @Inject(method = "onProjectileHit", at = @At("HEAD"), cancellable = true)
    private void sturdyOnProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile, CallbackInfo ci) {
        if (SLSCarpetSettings.sturdyDecoratedPot) {
            ci.cancel();
        }
    }

    // unbreakableDecoratedPot：玩家挖掘时不设 CRACKED（无裂纹效果），仍正常挖掉。
    @Redirect(
            method = "playerWillDestroy",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;setValue(Lnet/minecraft/world/level/block/state/properties/Property;Ljava/lang/Comparable;)Ljava/lang/Object;"
            )
    )
    private Object noCrackOnPlayerBreak(BlockState state, net.minecraft.world.level.block.state.properties.Property property, Comparable value) {
        if (SLSCarpetSettings.unbreakableDecoratedPot) {
            return state;
        }
        return state.setValue(property, value);
    }
}