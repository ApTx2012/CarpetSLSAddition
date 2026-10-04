package com.github.zly2006.carpetslsaddition.mixin.legacy;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
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
 * [旧版] 落沙替换的核心注入点。
 *
 * <p>26.1.2 的 {@code FallingBlockEntity.fall(Level, BlockPos, BlockState)} 直接使用传入的 state，
 * 不像 1.12 那样在构造实体时重新读取世界。本 mixin 用 {@link ModifyVariable} 拦截该 state 参数：
 * 当 {@code legacyFallingBlockReplace} 开启且配置了目标方块时，把 state 替换为目标方块的默认状态。
 *
 * <p>等价于 1.12 在"检查可下落"与"读取方块状态"之间的窗口替换效果——最终得到一个
 * "装着目标方块"的下落实体，落地后即还原为该目标方块。
 */
@Mixin(net.minecraft.world.entity.item.FallingBlockEntity.class)
public class FallingBlockEntityMixin {

    @ModifyVariable(
            method = "fall",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2
    )
    private static BlockState legacyReplaceFallingState(BlockState original) {
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
        return block.defaultBlockState();
    }
}