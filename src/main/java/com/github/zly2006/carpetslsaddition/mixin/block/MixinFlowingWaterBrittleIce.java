package com.github.zly2006.carpetslsaddition.mixin.block;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.4 快照加入的“碎冰”（Brittle Ice）机制：
 * LiquidBlock#shouldSpreadLiquid 中，水流方块任意水平方向的邻居是蓝冰时，
 * 会把当前水流方块替换为碎冰。
 *
 * 开启 noBrittleIceFromFlowingWater 规则后：
 * 仅当当前水方块处于“下落”状态（FlowingFluid#isFalling，即非水源且流体等级=8）时，
 * 才允许它被替换为碎冰；纯横向流动的水不再触发该替换，以保护蓝冰水道。
 *
 * 实现方式：用 WrapOperation 包住 shouldSpreadLiquid 里对 Level#setBlockAndUpdate 的调用，
 * 只在「目标方块是 BRITTLE_ICE」且「当前水不是下落状态」时跳过该次设置，其余调用原样放行。
 */
@Mixin(LiquidBlock.class)
public class MixinFlowingWaterBrittleIce {
    @WrapOperation(
        method = "shouldSpreadLiquid",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z")
    )
    private boolean wrapSetBlockAndUpdate(Level level, BlockPos pos, BlockState newState,
                                          Operation<Boolean> original) {
        if (SLSCarpetSettings.noBrittleIceFromFlowingWater
            && newState.is(Blocks.BRITTLE_ICE)) {
            FluidState current = level.getFluidState(pos);
            if (!isFalling(current)) {
                return false; // 跳过把这块水替换成碎冰
            }
        }
        return original.call(level, pos, newState);
    }

    private static boolean isFalling(FluidState fluid) {
        if (!(fluid.getType() instanceof FlowingFluid flowing)) return false;
        return flowing.isFalling(fluid);
    }
}