package com.github.zly2006.carpetslsaddition.util;

import com.github.zly2006.carpetslsaddition.ServerMain;
import com.github.zly2006.carpetslsaddition.net.BoardSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 服务端：读取玩家原版统计（挖掘/死亡），打包排行榜发给客户端。
 *
 * <p>数据源是 vanilla stats（{@code stats/<uuid>.json}），非 scoreboard。
 * 客户端原生拿不到别人的 stats，故由服务端读取后通过网络包 {@link BoardSyncPayload} 下发。
 */
public final class BoardSyncManager {

    private BoardSyncManager() {}

    public static final String BOARD_DEATHS = "deaths";
    public static final String BOARD_MINED = "mined";

    /** 排行榜最多显示条目数。 */
    public static final int TOP_N = 10;

    /** 读取某玩家的挖掘总数（遍历所有方块的 BLOCK_MINED 累加）。 */
    private static int totalMined(ServerStatsCounter counter) {
        int sum = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            sum += counter.getValue(Stats.BLOCK_MINED, block);
        }
        return sum;
    }
    /** 读取某玩家的死亡次数。 */
    private static int deaths(ServerStatsCounter counter) {
        return counter.getValue(Stats.CUSTOM, Stats.DEATHS);
    }

    /** 构建某榜单的排行数据（降序，取前 TOP_N）。 */
    public static List<BoardSyncPayload.Entry> buildBoard(MinecraftServer server, String board) {
        List<BoardSyncPayload.Entry> list = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerStatsCounter counter = player.getStats();
            int score = BOARD_MINED.equals(board) ? totalMined(counter) : deaths(counter);
            com.github.zly2006.carpetslsaddition.ServerMain.LOGGER.info("[SLSA-BOARD] player={} board={} score={}", player.getScoreboardName(), board, score);
            if (score > 0) {
                list.add(new BoardSyncPayload.Entry(player.getScoreboardName(), score));
            }
        }
        list.sort(Comparator.comparingInt(BoardSyncPayload.Entry::score).reversed());
        if (list.size() > TOP_N) {
            return new ArrayList<>(list.subList(0, TOP_N));
        }
        return list;
    }

    /** 把两个榜单 + 开关状态同步给某玩家。 */
    public static void syncTo(ServerPlayer player) {
        MinecraftServer server = ServerMain.server;
        if (server == null) {
            return;
        }
        boolean enabled = com.github.zly2006.carpetslsaddition.SLSCarpetSettings.slsBoardEnabled;
        // 先发开关状态
        ServerPlayNetworking.send(player, new com.github.zly2006.carpetslsaddition.net.BoardTogglePayload(enabled));
        if (!enabled) {
            return;
        }
        List<BoardSyncPayload.Entry> d = buildBoard(server, BOARD_DEATHS);
        List<BoardSyncPayload.Entry> m = buildBoard(server, BOARD_MINED);
        com.github.zly2006.carpetslsaddition.ServerMain.LOGGER.info("[SLSA-BOARD] syncTo {} deaths={} mined={}", player.getScoreboardName(), d.size(), m.size());
        ServerPlayNetworking.send(player, new BoardSyncPayload(BOARD_DEATHS, d));
        ServerPlayNetworking.send(player, new BoardSyncPayload(BOARD_MINED, m));
    }

    /** 同步给所有在线玩家。 */
    public static void syncToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncTo(player);
        }
    }

    private static volatile Boolean lastEnabled = null;
    private static long lastSyncTick = 0;

    /** 由轮询线程每秒调用：规则值变化时立即同步；否则每 5 秒定期同步一次数据。 */
    public static void tick(MinecraftServer server) {
        boolean now = com.github.zly2006.carpetslsaddition.SLSCarpetSettings.slsBoardEnabled;
        if (lastEnabled == null || lastEnabled != now) {
            lastEnabled = now;
            syncToAll(server);
            return;
        }
        if (now) {
            long tick = server.getTickCount();
            if (tick - lastSyncTick >= 100) {  // 每 100 tick（5秒）同步一次数据
                lastSyncTick = tick;
                syncToAll(server);
            }
        }
    }
}
