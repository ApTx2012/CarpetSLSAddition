package com.github.zly2006.carpetslsaddition.mixin.legacy;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [旧版] 计划刻立即执行（等价 1.12 的 instantTick flag）。
 *
 * <p>1.12 的 instantTick：世界装饰时会短暂开启，使计划刻（中继器、发射器、沙子等）
 * 立即执行而不等待延迟。26.1.2 已移除该 flag。
 *
 * <p>本 mixin 用 {@link Inject} 拦截 {@link ScheduledTick#triggerTick()}：
 * 规则开启时返回 0，使该计划刻在"当前时刻"被触发（等价立即执行）。
 *
 * <p><b>注意</b>：这会强制所有计划刻立即触发，可能造成大量连锁更新，
 * 仅应在 {@code legacyExploitMode} + {@code legacyInstantTick} 同时开启时使用。
 */
@Mixin(ScheduledTick.class)
public abstract class ScheduledTickMixin {

    @Inject(method = "triggerTick", at = @At("HEAD"), cancellable = true)
    private void legacyInstantTriggerTick(CallbackInfoReturnable<Long> cir) {
        if (SLSCarpetSettings.legacyExploitMode && SLSCarpetSettings.legacyInstantTick) {
            // 返回 0 表示"在当前时刻立即触发"
            cir.setReturnValue(0L);
        }
    }
}