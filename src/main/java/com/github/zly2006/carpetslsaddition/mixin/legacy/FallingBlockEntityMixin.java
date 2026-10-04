package com.github.zly2006.carpetslsaddition.mixin.legacy;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.ServerMain;
import com.github.zly2006.carpetslsaddition.util.LegacyBlockChangeRecorder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * [旧版] 落沙替换的核心注入点——重建 1.12 的"替换窗口"（记录+回放版）。
 *
 * <p>当 {@code legacyFallingBlockReplace} 开启时，若该位置在最近 {@code WINDOW_TICKS} 内被改动过，
 * 就用记录的新方块作为下落实体的内容。这样玩家搭建的装置（改变世界方块）会决定下落实体的内容。
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
        ServerMain.LOGGER.info("[SLSA-DEBUG] fall() pos={} original={}", pos, original.getBlock());
        BlockState recent = LegacyBlockChangeRecorder.getRecentChange(pos);
        if (recent != null && !recent.isAir()) {
            ServerMain.LOGGER.info("[SLSA-DEBUG] >>> REPLACE at {}: {} -> {}", pos, original.getBlock(), recent.getBlock());
            return recent;
        }
        ServerMain.LOGGER.info("[SLSA-DEBUG] no recent change at {}, recent={}", pos, recent);
        return original;
    }
}