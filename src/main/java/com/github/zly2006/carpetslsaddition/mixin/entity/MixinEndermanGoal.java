package com.github.zly2006.carpetslsaddition.mixin.entity;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
public class MixinEndermanGoal {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    public boolean isHoldable(BlockState blockState, TagKey<Block> tagKey) {
        if (!SLSCarpetSettings.endermanCanPickUpMushroom) {
            return blockState.is(tagKey) && !(blockState.getBlock() instanceof MushroomBlock);
        }
        else {
            return blockState.is(tagKey);
        }
    }
}