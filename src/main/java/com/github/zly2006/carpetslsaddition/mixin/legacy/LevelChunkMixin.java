package com.github.zly2006.carpetslsaddition.mixin.legacy;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.util.LegacyBlockChangeRecorder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [旧版] 方块变更记录——为"替换窗口"提供数据源。
 *
 * <p>hook {@link LevelChunk#setBlockState}：当规则开启时，记录每次方块变更（位置 + 新状态 + 当前 tick）。
 * 重力方块下落时（{@code FallingBlockEntityMixin}）会查询此记录，
 * 若该位置"刚被改过"，就用记录的新方块作为下落实体内容——重建 1.12 的窗口替换效果。
 *
 * <p>仅在 {@code legacyExploitMode} + {@code legacyFallingBlockReplace} 同时开启时记录，避免性能损耗。
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void legacyRecordBlockChange(BlockPos pos, BlockState newState, int flags, CallbackInfoReturnable<BlockState> cir) {
        if (!SLSCarpetSettings.legacyExploitMode || !SLSCarpetSettings.legacyFallingBlockReplace) {
            return;
        }
        if (newState == null) {
            return;
        }
        LegacyBlockChangeRecorder.record(pos, newState);
    }
}