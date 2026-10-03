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
    // oldRedstoneConnectionLogic：让红石能连到活板门上的红石线。
    // 新版在 getConnectingSide 里用 `blockState.getBlock() instanceof TrapDoorBlock` 判断。
    // 与旧版一致的做法是把这个 getBlock() 结果换成 AIR（绕过活板门特殊分支，走 canSurviveOn）。
    @Redirect(
            method = "getConnectingSide(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)Lnet/minecraft/world/level/block/state/properties/RedstoneSide;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;")
    )
    public Block redirectConnectionBlockState(BlockState instance) {
        if (SLSCarpetSettings.oldRedstoneConnectionLogic) {
            return Blocks.AIR;
        }
        else {
            return instance.getBlock();
        }
    }
}