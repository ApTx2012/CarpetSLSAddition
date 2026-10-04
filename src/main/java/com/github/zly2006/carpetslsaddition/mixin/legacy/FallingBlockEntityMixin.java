package com.github.zly2006.carpetslsaddition.mixin.legacy;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.ServerMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * [旧版] 落沙替换——固定目标版。
 *
 * <p>26.1.2 单线程下 1.12 的"替换窗口"物理不存在，本 mixin 采用直接方案：
 * 当 {@code legacyFallingBlockReplace} 开启且配置了 {@code legacyFallingBlockTarget} 时，
 * 把下落实体的方块状态替换为目标方块的默认状态——沙子下落即变目标方块。
 */
@Mixin(net.minecraft.world.entity.item.FallingBlockEntity.class)
public class FallingBlockEntityMixin {

    @ModifyVariable(
            method = "fall",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2
    )
    private static BlockState legacyReplaceFallingState(BlockState original, Level level, BlockPos pos, BlockState state) {
        if (!SLSCarpetSettings.legacyExploitMode || !SLSCarpetSettings.legacyFallingBlockReplace) {
            return original;
        }
        String target = SLSCarpetSettings.legacyFallingBlockTarget;
        if (target == null || target.isEmpty() || "#none".equals(target)) {
            return original;
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(target)).orElse(null);
        if (block == null) {
            return original;
        }
        ServerMain.LOGGER.info("[SLSA-DEBUG] >>> REPLACE at {}: {} -> {}", pos, original.getBlock(), block);
        return block.defaultBlockState();
    }
}