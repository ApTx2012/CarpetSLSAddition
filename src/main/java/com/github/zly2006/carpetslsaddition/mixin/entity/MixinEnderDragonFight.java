package com.github.zly2006.carpetslsaddition.mixin.entity;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.mixin.block.BlockPatternTestTransformInvoker;
import com.google.common.cache.LoadingCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(net.minecraft.world.level.dimension.end.EnderDragonFight.class)
public class MixinEnderDragonFight {
    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private BlockPattern exitPortalPattern;

    @Shadow
    private BlockPos exitPortalLocation;

    @Unique
    private int defaultChunkX = -8;
    @Unique
    private int defaultChunkZ = -8;
    @Unique
    private int defaultOriginY = -1;

    /**
     * @author Disy920
     * @reason Optimize the search process of the end portal
     */
    @Overwrite
    private @Nullable BlockPattern.BlockPatternMatch findExitPortal() {
        int i, j;
        if (!SLSCarpetSettings.optimizedOnDragonRespawn) {
            defaultChunkX = -8;
            defaultChunkZ = -8;
        }
        for (i = defaultChunkX; i <= 8; ++i) {
            for (j = defaultChunkZ; j <= 8; ++j) {
                LevelChunk worldChunk = this.level.getChunk(i, j);
                for (BlockEntity blockEntity : worldChunk.getBlockEntities().values()) {
                    if (SLSCarpetSettings.optimizedOnDragonRespawn && blockEntity instanceof TheEndGatewayBlockEntity) continue;
                    if (blockEntity instanceof TheEndPortalBlockEntity) {
                        BlockPattern.BlockPatternMatch result = this.exitPortalPattern.find(this.level, blockEntity.getBlockPos());
                        if (result != null) {
                            BlockPos blockPos = result.getFrontTopLeft().offset(3, 3, 3);
                            if (this.exitPortalLocation == null) {
                                this.exitPortalLocation = blockPos;
                            }
                            defaultChunkX = i;
                            defaultChunkZ = j;
                            return result;
                        }
                    }
                }
            }
        }
        if (this.exitPortalLocation == null) {
            if (SLSCarpetSettings.optimizedOnDragonRespawn && defaultOriginY != -1) {
                i = defaultOriginY;
            } else {
                i = this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, EndPodiumFeature.getLocation(BlockPos.ZERO).getX(), EndPodiumFeature.getLocation(BlockPos.ZERO).getZ());
            }
            boolean notFirstSearch = false;
            for (j = i; j >= 0; --j) {
                BlockPattern.BlockPatternMatch result2;
                BlockPos origin = EndPodiumFeature.getLocation(BlockPos.ZERO);
                BlockPos searchPos = new BlockPos(origin.getX(), j, origin.getZ());
                if (SLSCarpetSettings.optimizedOnDragonRespawn && notFirstSearch) {
                    result2 = partialSearchAround(this.exitPortalPattern, this.level, searchPos);
                } else {
                    result2 = this.exitPortalPattern.find(this.level, searchPos);
                }
                if (result2 != null) {
                    if (this.exitPortalLocation == null) {
                        this.exitPortalLocation = result2.getFrontTopLeft().offset(3, 3, 3);
                    }
                    defaultOriginY = j;
                    return result2;
                }
                notFirstSearch = true;
            }
        }

        return null;
    }

    @Inject(method = "respawnDragon", at = @At("HEAD"))
    private void resetCache(List<EndCrystal> crystals, CallbackInfo ci) {
        this.defaultChunkX = -8;
        this.defaultChunkZ = -8;
        this.defaultOriginY = -1;
    }

    @Unique
    private BlockPattern.BlockPatternMatch partialSearchAround(BlockPattern pattern, LevelReader world, BlockPos pos) {
        LoadingCache<BlockPos, BlockInWorld> loadingCache = BlockPattern.createLevelCache(world, false);
        int i = Math.max(Math.max(pattern.getWidth(), pattern.getHeight()), pattern.getDepth());
        for (BlockPos blockPos : BlockPos.betweenClosed(pos, pos.offset(i - 1, 0, i - 1))) {
            for (Direction direction : Direction.values()) {
                for (Direction direction2 : Direction.values()) {
                    BlockPattern.BlockPatternMatch result;
                    if (direction2 == direction || direction2 == direction.getOpposite() || (result = ((BlockPatternTestTransformInvoker) pattern).invokeTestTransform(blockPos, direction, direction2, loadingCache)) == null) continue;
                    return result;
                }
            }
        }
        return null;
    }
}