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

/**
 * [旧版] 落沙替换 / 瞬间下落。
 *
 * <p>1.12 的漏洞窗口：BlockFalling 在创建 EntityFallingBlock 时【重新读取世界】的方块状态，
 * 若在"检查可下落"与"读取状态"之间被异步线程替换，就能得到任意方块的下落实体。
 *
 * <p>26.1.2 已把该窗口结构性消除（fall() 直接接收 state 参数，不重读世界）。
 * 本 mixin 用规则【重新引入】等价行为：
 * <ul>
 *   <li>legacyInstantFall：跳过计划刻延迟，立即下落（等价 1.12 的 instantFall flag）</li>
 *   <li>legacyFallingBlockReplace：在创建下落实体前，把状态替换为配置的目标方块</li>
 * </ul>
 */
@Mixin(FallingBlock.class)
public class FallingBlockMixin {

    // instantFall 等价：在 tick 入口，若规则开启，直接执行"立即下落"分支。
    // 26.1.2 的 tick 已是计划刻回调，这里仅在规则开启时额外触发一次立即下落逻辑。
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void legacyTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand, CallbackInfo ci) {
        if (!SLSCarpetSettings.legacyExploitMode) {
            return;
        }
        if (SLSCarpetSettings.legacyInstantFall) {
            // 标记：本次下落由 instantFall 触发（供替换逻辑判断窗口）
            LegacyExploitState.markInstantFall(pos);
        }
        if (SLSCarpetSettings.legacyFallingBlockReplace) {
            // 由 FallingBlockEntityMixin.fall 处接管状态替换
            LegacyExploitState.setReplaceTarget(SLSCarpetSettings.legacyFallingBlockTarget);
        }
    }
}