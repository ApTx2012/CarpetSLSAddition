package com.github.zly2006.carpetslsaddition.mixin.block;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RedStoneWireBlock.class, priority = 1)
public class MixinRedstoneWireBlock {
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"), require = 0)
    public Block redirectConnectionBlockState(BlockState instance) {
        if (SLSCarpetSettings.oldRedstoneConnectionLogic) {
            return Blocks.AIR;
        }
        else {
            return instance.getBlock();
        }
    }
}