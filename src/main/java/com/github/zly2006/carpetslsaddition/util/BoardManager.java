package com.github.zly2006.carpetslsaddition.util;

import com.github.zly2006.carpetslsaddition.ServerMain;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * 死亡榜 / 挖掘榜的 scoreboard 管理器（服务端）。
 *
 * <p>数据源是 vanilla stats，但 stats 不会自动同步给其他客户端。
 * 因此本类定期把 stats 的聚合值写入 scoreboard objective，
 * 客户端再用原版计分板侧边栏显示（scoreboard 会自动同步给所有客户端）。
 *
 * <ul>
 *   <li>{@code sls_deaths}：死亡次数（DUMMY 判据，从 stats 的 {@link Stats#DEATHS} 写入）。</li>
 *   <li>{@code sls_mined}：挖掘总数（DUMMY 判据，从 stats 的 {@link Stats#BLOCK_MINED} 聚合）。</li>
 * </ul>
 */
public final class BoardManager {

    private BoardManager() {}

    public static final String OBJ_DEATHS = "sls_deaths";
    public static final String OBJ_MINED = "sls_mined";

    /** 确保两个 objective 存在（服务器启动/重载时调用）。 */
    public static void ensureObjectives(MinecraftServer server) {
        Scoreboard sb = server.getScoreboard();
        ServerMain.LOGGER.info("[SLSA] ensureObjectives called, deaths={} mined={}",
                sb.getObjective(OBJ_DEATHS) != null, sb.getObjective(OBJ_MINED) != null);
        if (sb.getObjective(OBJ_DEATHS) == null) {
            sb.addObjective(OBJ_DEATHS, ObjectiveCriteria.DUMMY,
                    Component.literal("死亡榜"), ObjectiveCriteria.RenderType.INTEGER, true, null);
            ServerMain.LOGGER.info("[SLSA] created scoreboard objective {}", OBJ_DEATHS);
        }
        if (sb.getObjective(OBJ_MINED) == null) {
            sb.addObjective(OBJ_MINED, ObjectiveCriteria.DUMMY,
                    Component.literal("挖掘榜"), ObjectiveCriteria.RenderType.INTEGER, true, null);
            ServerMain.LOGGER.info("[SLSA] created scoreboard objective {}", OBJ_MINED);
        }
    }

    /** 读取某玩家的挖掘总数（遍历 stats map 中 BLOCK_MINED 类型条目）。 */
    private static int totalMined(ServerStatsCounter counter) {
        int sum = 0;
        for (Stat<?> stat : counter.stats.keySet()) {
            if (stat.getType() == Stats.BLOCK_MINED) {
                sum += counter.getValue(stat);
            }
        }
        return sum;
    }

    /** 读取某玩家的死亡次数。 */
    private static int totalDeaths(ServerStatsCounter counter) {
        return counter.getValue(Stats.CUSTOM, Stats.DEATHS);
    }

    /** 把一个分数写入 objective（若不存在则创建/更新）。 */
    private static void setScore(Scoreboard sb, Objective obj, ServerPlayer player, int value) {
        if (obj == null) {
            return;
        }
        ScoreAccess access = sb.getOrCreatePlayerScore((ScoreHolder) player, obj);
        access.set(value);
    }

    /** 从 stats 同步所有在线玩家的分数到 scoreboard（定期调用）。 */
    public static void syncScores(MinecraftServer server) {
        Scoreboard sb = server.getScoreboard();
        Objective deaths = sb.getObjective(OBJ_DEATHS);
        Objective mined = sb.getObjective(OBJ_MINED);
        if (deaths == null && mined == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerStatsCounter counter = player.getStats();
            setScore(sb, deaths, player, totalDeaths(counter));
            setScore(sb, mined, player, totalMined(counter));
        }
    }
}