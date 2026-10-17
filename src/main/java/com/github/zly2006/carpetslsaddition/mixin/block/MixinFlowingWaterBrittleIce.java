package com.github.zly2006.carpetslsaddition.mixin.block;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.4 快照加入的“碎冰”（Brittle Ice）机制：
 * LiquidBlock#shouldSpreadLiquid 中，水流方块任意水平方向的邻居是蓝冰时，
 * 会把当前水流方块替换为碎冰。
 *
 * 开启 noBrittleIceFromFlowingWater 规则后：
 * 只有「水源方块」贴蓝冰时照常变碎冰；所有「流动的水」（横向、下落）都不再变碎冰，
 * 且水流扩散不受影响。
 *
 * 实现：原版判断邻居是不是蓝冰用 BlockState#is(Blocks.BLUE_ICE)，一旦命中就
 * 变碎冰并 return false（中断扩散）。本 mixin 让流动水贴蓝冰时这次 is() 返回 false，
 * 整个“变碎冰”分支被跳过，shouldSpreadLiquid 正常返回 true，水流照常扩散。
 * 位置信息通过 ThreadLocal 从方法入口传入。
 */
@Mixin(LiquidBlock.class)
public class MixinFlowingWaterBrittleIce {
    @Unique
    private static final ThreadLocal<Level> CURRENT_LEVEL = new ThreadLocal<>();
    @Unique
    private static final ThreadLocal<BlockPos> CURRENT_POS = new ThreadLocal<>();

    @Inject(method = "shouldSpreadLiquid", at = @At("HEAD"))
    private void captureArgs(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        CURRENT_LEVEL.set(level);
        CURRENT_POS.set(pos);
    }

    @WrapOperation(
        method = "shouldSpreadLiquid",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z")
    )
    private boolean wrapIsBlueIce(BlockState instance, Object target, Operation<Boolean> original) {
        Boolean result = original.call(instance, target);
        if (!result) return false;
        // 只有判断目标确实是 BLUE_ICE 时才可能干预
        if (!(target instanceof net.minecraft.world.level.block.Block b) || !b.equals(Blocks.BLUE_ICE)) {
            return true;
        }
        if (!SLSCarpetSettings.noBrittleIceFromFlowingWater) return true;

        Level level = CURRENT_LEVEL.get();
        BlockPos pos = CURRENT_POS.get();
        if (level == null || pos == null) return true;

        FluidState current = level.getFluidState(pos);
        // 流动水（非水源）不让它触发碎冰替换
        if (!current.isSource()) {
            return false;
        }
        return true;
    }
}