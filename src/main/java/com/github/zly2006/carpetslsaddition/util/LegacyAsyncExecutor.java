package com.github.zly2006.carpetslsaddition.util;

import com.github.zly2006.carpetslsaddition.ServerMain;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * [旧版] 自建异步线程，模拟 1.12 的"染色玻璃→信标搜索线程"。
 *
 * <p>1.12 的漏洞依赖一个独立的异步线程，与主线程并发访问区块哈希表
 * （{@code Long2ObjectOpenHashMap}），从而制造 race condition，让 chunk 被误判为"未加载"，
 * 触发重新加载与世界装饰。
 *
 * <p>26.1.2 的信标已无独立线程，故本类自建一个单线程调度器：
 * 周期性在<b>主线程之外</b>调用 {@link ServerChunkCache#getChunkNow} 与读取区块，
 * 尽最大可能复现 1.12 的并发访问模式。
 *
 * <p><b>⚠ 风险提示</b>：26.1.2 的区块访问有 {@code mainThreadExecutor} 约束，
 * 跨线程访问可能触发断言或未定义行为，极端情况下可导致服务器崩溃或存档损坏。
 * 仅在 {@code legacyExploitMode} + {@code legacyAsyncChunkAccess} 同时开启时启动。
 */
public final class LegacyAsyncExecutor {

    private LegacyAsyncExecutor() {}

    private static ScheduledExecutorService executor;
    private static volatile boolean started = false;

    /** 目标区块坐标（由玩家装置或命令设置）。 */
    private static volatile int targetChunkX = Integer.MIN_VALUE;
    private static volatile int targetChunkZ = Integer.MIN_VALUE;

    public static void setTargetChunk(int x, int z) {
        targetChunkX = x;
        targetChunkZ = z;
    }

    public static synchronized void start() {
        if (started) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SLSA-Legacy-AsyncChunk");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(LegacyAsyncExecutor::tick, 0, 1, TimeUnit.MILLISECONDS);
        started = true;
        LegacyExploitState.setAsyncThreadRunning(true);
        ServerMain.LOGGER.info("[SLSA] legacy async chunk thread started");
    }

    public static synchronized void stop() {
        if (!started || executor == null) {
            return;
        }
        executor.shutdownNow();
        executor = null;
        started = false;
        LegacyExploitState.setAsyncThreadRunning(false);
        ServerMain.LOGGER.info("[SLSA] legacy async chunk thread stopped");
    }

    /** 异步线程主体：并发访问目标区块，制造与主线程的竞态。 */
    private static void tick() {
        try {
            if (!SLSCarpetSettingsBridge.shouldRun()) {
                return;
            }
            MinecraftServer server = ServerMain.server;
            if (server == null) {
                return;
            }
            for (ServerLevel level : server.getAllLevels()) {
                ServerChunkCache cache = level.getChunkSource();
                if (targetChunkX != Integer.MIN_VALUE) {
                    // 故意在主线程之外读取区块表——制造 race condition
                    LevelChunk chunk = cache.getChunkNow(targetChunkX, targetChunkZ);
                    if (chunk != null) {
                        // 触发一次方块状态读取，模拟信标向下搜索
                        chunk.getBlockState(new net.minecraft.core.BlockPos(
                                targetChunkX << 4, level.getMinY(), targetChunkZ << 4));
                    }
                }
            }
        } catch (Throwable t) {
            // 异步线程绝不能让异常冒泡导致调度器静默停止
            ServerMain.LOGGER.debug("[SLSA] legacy async tick error: {}", t.toString());
        }
    }

    /** 桥接类：避免直接依赖 SLSCarpetSettings 的静态初始化顺序问题。 */
    private static final class SLSCarpetSettingsBridge {
        static boolean shouldRun() {
            return com.github.zly2006.carpetslsaddition.SLSCarpetSettings.legacyExploitMode
                    && com.github.zly2006.carpetslsaddition.SLSCarpetSettings.legacyAsyncChunkAccess;
        }
    }
}