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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [旧版] 瞬间下落（等价 1.12 的 instantFall flag）。
 *
 * <p>1.12：世界装饰时短暂开启 instantFall，使重力方块在被更新时【立即】转为下落实体，
 * 而不等待 2gt 的计划刻延迟。26.1.2 已移除该 flag。
 *
 * <p>实现：
 * <ul>
 *   <li>{@link #legacyDelayAfterPlace}：规则开启时把放置后的延迟改为 0，使下落尽快触发。</li>
 *   <li>{@link #legacyTick}：保留 tick 入口的标记逻辑。</li>
 * </ul>
 */
@Mixin(FallingBlock.class)
public abstract class FallingBlockMixin {

    /** instantFall：把"放置后延迟"改为 0（最小延迟）。 */
    @Inject(method = "getDelayAfterPlace", at = @At("HEAD"), cancellable = true)
    private void legacyDelayAfterPlace(CallbackInfoReturnable<Integer> cir) {
        if (SLSCarpetSettings.legacyExploitMode && SLSCarpetSettings.legacyInstantFall) {
            cir.setReturnValue(0);
        }
    }

    /** instantFall：tick 入口标记（供其他机制判断窗口）。 */
    @Inject(method = "tick", at = @At("HEAD"))
    private void legacyTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand, CallbackInfo ci) {
        if (SLSCarpetSettings.legacyExploitMode && SLSCarpetSettings.legacyInstantFall) {
            LegacyExploitState.markInstantFall(pos);
        }
    }
}