package com.github.zly2006.carpetslsaddition.mixin.legacy;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.util.LegacyExploitState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [旧版] 瞬间下落（等价 1.12 的 instantFall flag）。
 *
 * <p>1.12：世界装饰时短暂开启 instantFall，使重力方块在被更新时【立即】转为下落实体，
 * 而不等待 2gt 的计划刻延迟。26.1.2 已移除该 flag。
 *
 * <p>实现方式（双保险）：
 * <ul>
 *   <li>{@link #legacyDelayAfterPlace}：把 {@code getDelayAfterPlace()} 的返回值改为 0。</li>
 *   <li>{@link #legacyScheduleDelay}：直接拦截 {@code onPlace}/{@code updateShape} 里
 *       对 {@code scheduleTick} 的 delay 参数，强制改为 0（更直接可靠）。</li>
 * </ul>
 */
@Mixin(FallingBlock.class)
public abstract class FallingBlockMixin {

    /** instantFall：把"放置后延迟"改为 0。 */
    @Inject(method = "getDelayAfterPlace", at = @At("HEAD"), cancellable = true)
    private void legacyDelayAfterPlace(CallbackInfoReturnable<Integer> cir) {
        if (SLSCarpetSettings.legacyExploitMode && SLSCarpetSettings.legacyInstantFall) {
            com.github.zly2006.carpetslsaddition.ServerMain.LOGGER.info("[SLSA-DEBUG] getDelayAfterPlace called -> 0");
            cir.setReturnValue(0);
        }
    }

    /**
     * instantFall：拦截 onPlace / updateShape 里 scheduleTick 的 delay 参数，强制为 0。
     * 第 3 个参数（index=2）是延迟 tick 数。
     */
    @ModifyArg(
            method = "onPlace",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;scheduleTick(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;I)V"),
            index = 2
    )
    private int legacyScheduleDelayLevel(int delay) {
        if (SLSCarpetSettings.legacyExploitMode && SLSCarpetSettings.legacyInstantFall) {
            return 0;
        }
        return delay;
    }

    @ModifyArg(
            method = "updateShape",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/ScheduledTickAccess;scheduleTick(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;I)V"),
            index = 2
    )
    private int legacyScheduleDelayAccess(int delay) {
        if (SLSCarpetSettings.legacyExploitMode && SLSCarpetSettings.legacyInstantFall) {
            return 0;
        }
        return delay;
    }

    /** instantFall：tick 入口标记（供其他机制判断窗口）。 */
    @Inject(method = "tick", at = @At("HEAD"))
    private void legacyTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand, CallbackInfo ci) {
        if (SLSCarpetSettings.legacyExploitMode && SLSCarpetSettings.legacyInstantFall) {
            com.github.zly2006.carpetslsaddition.ServerMain.LOGGER.info("[SLSA-DEBUG] FallingBlock.tick pos={}", pos);
            LegacyExploitState.markInstantFall(pos);
        }
    }
}