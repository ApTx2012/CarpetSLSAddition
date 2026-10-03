package com.github.zly2006.carpetslsaddition.turtle;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds dry nesting blocks beside nearby, exposed source water.
 *
 * <p>This class was rewritten for throughput. The scan is the hot path: one call
 * covers 73 x 73 columns x 19 vertical steps, and a pregnant turtle re-runs it
 * whenever it drifts more than four blocks from the previous search origin or
 * exhausts its cached candidate list.
 *
 * <p>Every result-affecting rule is unchanged:
 * <ul>
 *   <li>the same box is visited (32 in X/Z for nests, plus a 4 block halo for
 *       water, 8 up/down for nests, plus 1 for water);</li>
 *   <li>the same conditions are tested, in the same order;</li>
 *   <li>unloaded chunks are still skipped, and are still never loaded or
 *       generated ({@code loadOrGenerate = false});</li>
 *   <li>the candidate list is still ordered by the same comparator, which ends
 *       in the block coordinates and therefore is a total order. Visit order no
 *       longer matters, so the sort result is identical.</li>
 * </ul>
 *
 * <p>What changed, and why it is equivalent:
 * <ul>
 *   <li>blocks are read through the chunk section that holds them instead of
 *       {@code Level#getBlockState}, which re-resolves the chunk, the section
 *       index and the build-height check on every single block. Sections that
 *       {@code hasOnlyAir()} are skipped entirely: they cannot contribute water
 *       or sand;</li>
 *   <li>the same availability test as {@code Level#hasChunk} is used, but the
 *       resolved chunk is kept for the section reads instead of being thrown
 *       away and looked up again per block;</li>
 *   <li>sand positions are collected as packed longs instead of thousands of
 *       {@code BlockPos} objects, so a scan allocates no per-block garbage;</li>
 *   <li>{@code isValidSite} hoists its loop invariants and remembers the last
 *       chunk availability answer; the disk it walks spans at most four chunks,
 *       so this only removes repeated lookups.</li>
 * </ul>
 */
public final class NestingSiteFinder {
    public static final int SEARCH_RADIUS = 32;
    public static final int VERTICAL_SEARCH_RADIUS = 8;
    public static final int WATER_RADIUS = 4;
    public static final int WATER_VERTICAL_RADIUS = 1;

    private static final Comparator<Candidate> CANDIDATE_ORDER = Comparator
            .comparingDouble(Candidate::waterDistanceSquared)
            .thenComparingDouble(Candidate::sandDistanceSquared)
            .thenComparingInt(candidate -> candidate.sandPos().getX())
            .thenComparingInt(candidate -> candidate.sandPos().getY())
            .thenComparingInt(candidate -> candidate.sandPos().getZ());

    private NestingSiteFinder() {
    }

    /**
     * Searches a square extending 32 blocks in X/Z and 8 blocks in Y from the
     * turtle. A four-block halo includes water beside nests at the boundary.
     * The caller must check the turtle's collision box and navigation path.
     */
    public static List<Candidate> findCandidates(Turtle turtle) {
        Level level = turtle.level();
        BlockPos origin = turtle.blockPosition();
        int originX = origin.getX();
        int originY = origin.getY();
        int originZ = origin.getZ();
        int scanRadius = SEARCH_RADIUS + WATER_RADIUS;
        int minY = Math.max(level.getMinY(), originY - VERTICAL_SEARCH_RADIUS - WATER_VERTICAL_RADIUS);
        int maxY = Math.min(level.getMaxY() - 1, originY + VERTICAL_SEARCH_RADIUS + WATER_VERTICAL_RADIUS);
        int nestMinX = originX - SEARCH_RADIUS;
        int nestMaxX = originX + SEARCH_RADIUS;
        int nestMinZ = originZ - SEARCH_RADIUS;
        int nestMaxZ = originZ + SEARCH_RADIUS;
        int nestMinY = originY - VERTICAL_SEARCH_RADIUS;
        int nestMaxY = originY + VERTICAL_SEARCH_RADIUS;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        LongArrayList sandBlocks = new LongArrayList();
        Long2DoubleOpenHashMap waterDistances = new Long2DoubleOpenHashMap();
        waterDistances.defaultReturnValue(Double.POSITIVE_INFINITY);

        // Read each block once, instead of repeatedly querying the world around
        // every sand block. Unloaded chunks are never requested or generated.
        if (minY <= maxY) {
            Vec3 turtlePos = turtle.position();
            int minSection = level.getSectionIndex(minY);
            int maxSection = level.getSectionIndex(maxY);
            for (int chunkX = (originX - scanRadius) >> 4; chunkX <= (originX + scanRadius) >> 4; chunkX++) {
                int fromX = Math.max(originX - scanRadius, chunkX << 4);
                int toX = Math.min(originX + scanRadius, (chunkX << 4) + 15);
                for (int chunkZ = (originZ - scanRadius) >> 4; chunkZ <= (originZ + scanRadius) >> 4; chunkZ++) {
                    // The same test Level#hasChunk performs, except the chunk is
                    // kept: loadOrGenerate = false never loads or generates.
                    ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                    if (chunk == null) {
                        continue;
                    }
                    int fromZ = Math.max(originZ - scanRadius, chunkZ << 4);
                    int toZ = Math.min(originZ + scanRadius, (chunkZ << 4) + 15);
                    LevelChunkSection[] sections = chunk.getSections();
                    for (int sectionIndex = minSection; sectionIndex <= maxSection; sectionIndex++) {
                        if (sectionIndex < 0 || sectionIndex >= sections.length) {
                            continue;
                        }
                        LevelChunkSection section = sections[sectionIndex];
                        // An all-air section holds neither water nor sand, so
                        // skipping it reads no block the loops could have used.
                        if (section == null || section.hasOnlyAir()) {
                            continue;
                        }
                        int sectionMinY = SectionPos.sectionToBlockCoord(level.getMinSectionY() + sectionIndex);
                        int fromY = Math.max(minY, sectionMinY);
                        int toY = Math.min(maxY, sectionMinY + 15);
                        boolean nestSection = fromY <= nestMaxY && toY >= nestMinY;
                        for (int x = fromX; x <= toX; x++) {
                            int localX = x & 15;
                            boolean nestX = x >= nestMinX && x <= nestMaxX;
                            for (int z = fromZ; z <= toZ; z++) {
                                boolean inNestRange = nestSection && nestX && z >= nestMinZ && z <= nestMaxZ;
                                int localZ = z & 15;
                                for (int y = fromY; y <= toY; y++) {
                                    BlockState state = section.getBlockState(localX, y & 15, localZ);
                                    if (isExposedSourceWater(level, cursor, x, y, z, state)) {
                                        cursor.set(x, y, z);
                                        waterDistances.put(BlockPos.asLong(x, y, z),
                                                cursor.distToCenterSqr(turtlePos));
                                    }
                                    if (inNestRange && y >= nestMinY && y <= nestMaxY
                                            && state.is(BlockTags.SAND)
                                            && level.isEmptyBlock(cursor.set(x, y + 1, z))) {
                                        sandBlocks.add(BlockPos.asLong(x, y, z));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        List<Candidate> candidates = new ArrayList<>();
        if (waterDistances.isEmpty()) {
            return candidates;
        }
        Vec3 turtlePos = turtle.position();
        for (int index = 0; index < sandBlocks.size(); index++) {
            long packedSand = sandBlocks.getLong(index);
            int sandX = BlockPos.getX(packedSand);
            int sandY = BlockPos.getY(packedSand);
            int sandZ = BlockPos.getZ(packedSand);
            double nearestWaterDistance = Double.POSITIVE_INFINITY;
            for (int dx = -WATER_RADIUS; dx <= WATER_RADIUS; dx++) {
                for (int dz = -WATER_RADIUS; dz <= WATER_RADIUS; dz++) {
                    if (dx * dx + dz * dz > WATER_RADIUS * WATER_RADIUS) {
                        continue;
                    }
                    for (int dy = -WATER_VERTICAL_RADIUS; dy <= WATER_VERTICAL_RADIUS; dy++) {
                        long waterPosition = BlockPos.asLong(sandX + dx, sandY + dy, sandZ + dz);
                        nearestWaterDistance = Math.min(nearestWaterDistance, waterDistances.get(waterPosition));
                    }
                }
            }
            if (Double.isFinite(nearestWaterDistance)) {
                cursor.set(sandX, sandY, sandZ);
                candidates.add(new Candidate(new BlockPos(sandX, sandY, sandZ), nearestWaterDistance,
                        cursor.distToCenterSqr(turtlePos)));
            }
        }
        candidates.sort(CANDIDATE_ORDER);
        return candidates;
    }

    /** Rechecks a chosen nest, including changes to its sand, air, or water. */
    public static boolean isValidSite(LevelReader level, BlockPos sandPos) {
        if (!level.isInsideBuildHeight(sandPos) || !level.isInsideBuildHeight(sandPos.above())
                || !level.hasChunkAt(sandPos)
                || !TurtleEggBlock.isSand(level, sandPos)
                || !level.isEmptyBlock(sandPos.above())) {
            return false;
        }
        int sandX = sandPos.getX();
        int sandY = sandPos.getY();
        int sandZ = sandPos.getZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int lastChunkX = Integer.MIN_VALUE;
        int lastChunkZ = Integer.MIN_VALUE;
        boolean lastChunkLoaded = false;
        for (int dx = -WATER_RADIUS; dx <= WATER_RADIUS; dx++) {
            for (int dz = -WATER_RADIUS; dz <= WATER_RADIUS; dz++) {
                if (dx * dx + dz * dz > WATER_RADIUS * WATER_RADIUS) {
                    continue;
                }
                int x = sandX + dx;
                int z = sandZ + dz;
                int chunkX = x >> 4;
                int chunkZ = z >> 4;
                // The disk above spans at most four chunks, so this only removes
                // repeated queries for columns that share a chunk.
                if (chunkX != lastChunkX || chunkZ != lastChunkZ) {
                    lastChunkX = chunkX;
                    lastChunkZ = chunkZ;
                    lastChunkLoaded = level.hasChunk(chunkX, chunkZ);
                }
                if (!lastChunkLoaded) {
                    continue;
                }
                for (int dy = -WATER_VERTICAL_RADIUS; dy <= WATER_VERTICAL_RADIUS; dy++) {
                    int y = sandY + dy;
                    cursor.set(x, y, z);
                    if (level.isInsideBuildHeight(y) && level.isInsideBuildHeight(y + 1)
                            && isExposedSourceWater(level, cursor, x, y, z, level.getBlockState(cursor))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isExposedSourceWater(LevelReader level, BlockPos.MutableBlockPos cursor,
                                                int x, int y, int z, BlockState state) {
        // Requiring a liquid block excludes waterlogged fences, slabs and other
        // solid blocks even though their fluid state may report source water.
        if (!state.liquid()) {
            return false;
        }
        FluidState fluid = state.getFluidState();
        return fluid.is(FluidTags.WATER) && fluid.isSource() && level.isEmptyBlock(cursor.set(x, y + 1, z));
    }

    public record Candidate(BlockPos sandPos, double waterDistanceSquared, double sandDistanceSquared) {
    }
}
