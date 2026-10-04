package com.github.zly2006.carpetslsaddition.util;

import com.github.zly2006.carpetslsaddition.ServerMain;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [旧版] 方块变更记录器——用于重建 1.12 的"替换窗口"。
 *
 * <p>1.12 的窗口依赖"异步线程在主线程 tick 中途改块"，26.1.2 单线程模型下物理不存在。
 * 本类改用"记录+回放"：把最近发生的方块变更记录在此，
 * 当重力方块触发下落时，若该位置"刚被改过"，就用记录的新方块作为下落实体的内容——
 * 等价于 1.12 的窗口替换效果（只是窗口由 mod 主动开启）。
 *
 * <p>记录以"服务器 tick 数"为界：仅保留最近 {@link #WINDOW_TICKS} 个 tick 内的变更，
 * 超过即失效，避免记录无限增长或误伤无关方块。
 */
public final class LegacyBlockChangeRecorder {

    private LegacyBlockChangeRecorder() {}

    /** 窗口大小：变更与下落之间允许的最大 tick 间隔。 */
    public static final long WINDOW_TICKS = 2L;

    private record Change(BlockState state, long tick) {}

    private static final Map<Long, Change> RECENT = new ConcurrentHashMap<>();

    /** 获取当前服务器 tick（拿不到时回退为 0）。 */
    private static long now() {
        MinecraftServer server = ServerMain.server;
        return server != null ? server.getTickCount() : 0L;
    }

    /** 记录一次方块变更。 */
    public static void record(BlockPos pos, BlockState newState) {
        RECENT.put(pos.asLong(), new Change(newState, now()));
    }

    /**
     * 查询某位置是否在窗口期内有变更记录。
     *
     * @return 若命中窗口，返回记录的新状态；否则返回 null。
     */
    public static BlockState getRecentChange(BlockPos pos) {
        Change c = RECENT.get(pos.asLong());
        if (c == null) {
            return null;
        }
        if (now() - c.tick() > WINDOW_TICKS) {
            RECENT.remove(pos.asLong());
            return null;
        }
        return c.state();
    }

    /** 清理过期记录。 */
    public static void cleanup() {
        long t = now();
        RECENT.entrySet().removeIf(e -> t - e.getValue().tick() > WINDOW_TICKS);
    }

    public static void clear() {
        RECENT.clear();
    }
}