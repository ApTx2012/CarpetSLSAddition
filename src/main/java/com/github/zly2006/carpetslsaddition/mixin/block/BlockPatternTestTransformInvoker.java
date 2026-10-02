package com.github.zly2006.carpetslsaddition.mixin.block;

import com.google.common.cache.LoadingCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockPattern.class)
public interface BlockPatternTestTransformInvoker {
    @Invoker("matches")
    BlockPattern.BlockPatternMatch invokeTestTransform(BlockPos frontTopLeft, Direction forwards, Direction up, LoadingCache<BlockPos, BlockInWorld> cache);
}