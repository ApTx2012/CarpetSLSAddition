package com.github.zly2006.carpetslsaddition.mixin.legacy;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.util.LegacyBlockChangeRecorder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * [旧版] 落沙替换的核心注入点——重建 1.12 的"替换窗口"（记录+回放版）。
 *
 * <p>1.12：{@code BlockFalling} 创建实体时【重读世界】，异步线程在"检查→读取"间改块即可替换。
 * 26.1.2 单线程下该窗口物理不存在（tick 内同步 fall，无让出）。
 *
 * <p>本 mixin 改用 {@link LegacyBlockChangeRecorder}：
 * 当 {@code legacyFallingBlockReplace} 开启时，若该位置在最近 {@code WINDOW_TICKS} 内被改动过，
 * 就用记录的新方块作为下落实体的内容。这样玩家搭建的装置（改变世界方块）会决定下落实体的内容，
 * 与 1.12 的"装置驱动"玩法一致。
 */
@Mixin(net.minecraft.world.entity.item.FallingBlockEntity.class)
public class FallingBlockEntityMixin {

    @ModifyVariable(
            method = "fall",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2
    )
    private static BlockState legacyReplaceFallingState(BlockState original, net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
        if (!SLSCarpetSettings.legacyExploitMode || !SLSCarpetSettings.legacyFallingBlockReplace) {
            return original;
        }
        BlockState recent = LegacyBlockChangeRecorder.getRecentChange(pos);
        if (recent != null && !recent.isAir()) {
            return recent;
        }
        return original;
    }
}