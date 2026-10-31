package com.github.zly2006.carpetslsaddition.util;

import com.github.zly2006.carpetslsaddition.ServerMain;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatType;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 死亡榜 / 挖掘榜 / 放置榜的 scoreboard 管理器（服务端）。
 *
 * <p>数据源是 vanilla stats，但 stats 不会自动同步给其他客户端。
 * 因此本类定期把 stats 的聚合值写入 scoreboard objective，
 * 客户端再用原版计分板侧边栏显示（scoreboard 会自动同步给所有客户端）。
 *
 * <ul>
 *   <li>{@code sls_deaths}：死亡次数（DUMMY 判据，从 stats 的 {@link Stats#DEATHS} 写入）。</li>
 *   <li>{@code sls_mined}：挖掘总数（DUMMY 判据，从 stats 的 {@link Stats#BLOCK_MINED} 聚合）。</li>
 *   <li>{@code sls_placed}：放置总数（DUMMY 判据，通过 mixin 在放置方块时增加计数）。</li>
 * </ul>
 */
public final class BoardManager {

    private BoardManager() {}

    public static final String OBJ_DEATHS = "sls_deaths";
    public static final String OBJ_MINED = "sls_mined";
    public static final String OBJ_PLACED = "sls_placed";
    public static final String OBJ_PLAY_TIME = "sls_play_time";

    /** 存储每个玩家的放置计数（键为玩家 UUID 字符串）。 */
    private static final Map<String, Integer> placedCounts = new ConcurrentHashMap<>();

    /** 确保四个 objective 存在（服务器启动/重载时调用）。 */
    public static void ensureObjectives(MinecraftServer server) {
        Scoreboard sb = server.getScoreboard();
        ServerMain.LOGGER.info("[SLSA] ensureObjectives called, deaths={} mined={} placed={} play_time={}",
                sb.getObjective(OBJ_DEATHS) != null, sb.getObjective(OBJ_MINED) != null, sb.getObjective(OBJ_PLACED) != null, sb.getObjective(OBJ_PLAY_TIME) != null);
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
        if (sb.getObjective(OBJ_PLACED) == null) {
            sb.addObjective(OBJ_PLACED, ObjectiveCriteria.DUMMY,
                    Component.literal("放置榜"), ObjectiveCriteria.RenderType.INTEGER, true, null);
            ServerMain.LOGGER.info("[SLSA] created scoreboard objective {}", OBJ_PLACED);
        }
        if (sb.getObjective(OBJ_PLAY_TIME) == null) {
            Objective obj = sb.addObjective(OBJ_PLAY_TIME, ObjectiveCriteria.DUMMY,
                    Component.literal("在线时长"), ObjectiveCriteria.RenderType.INTEGER, true, null);
            // 设置自定义 NumberFormat 用于客户端显示格式化
            obj.setNumberFormat(new PlayTimeNumberFormat());
            ServerMain.LOGGER.info("[SLSA] created scoreboard objective {}", OBJ_PLAY_TIME);
        }
        // 让客户端能收到这四个 objective（load 进来的 objective 可能未被追踪）
        if (sb instanceof net.minecraft.server.ServerScoreboard ssb) {
            Objective d = sb.getObjective(OBJ_DEATHS);
            if (d != null) ssb.startTrackingObjective(d);
            Objective m = sb.getObjective(OBJ_MINED);
            if (m != null) ssb.startTrackingObjective(m);
            Objective p = sb.getObjective(OBJ_PLACED);
            if (p != null) ssb.startTrackingObjective(p);
            Objective t = sb.getObjective(OBJ_PLAY_TIME);
            if (t != null) ssb.startTrackingObjective(t);
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

    /** 获取玩家的放置计数。 */
    public static int getPlacedCount(ServerPlayer player) {
        return placedCounts.getOrDefault(player.getUUID().toString(), 0);
    }

    /** 增加玩家的放置计数（放置方块时调用）。 */
    public static void incrementPlaced(ServerPlayer player) {
        String uuid = player.getUUID().toString();
        placedCounts.merge(uuid, 1, Integer::sum);
        // 实时同步到 scoreboard
        Scoreboard sb = player.level().getServer().getScoreboard();
        Objective placed = sb.getObjective(OBJ_PLACED);
        if (placed != null) {
            ScoreAccess access = sb.getOrCreatePlayerScore((ScoreHolder) player, placed);
            access.set(placedCounts.get(uuid));
        }
    }

    /** 重置玩家的放置计数（如玩家离开或重置时）。 */
    public static void resetPlacedCount(ServerPlayer player) {
        placedCounts.remove(player.getUUID().toString());
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
        Objective placed = sb.getObjective(OBJ_PLACED);
        Objective playTime = sb.getObjective(OBJ_PLAY_TIME);
        if (deaths == null && mined == null && placed == null && playTime == null) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerStatsCounter counter = player.getStats();
            setScore(sb, deaths, player, totalDeaths(counter));
            setScore(sb, mined, player, totalMined(counter));
            // 放置榜已实时同步，这里只需确保在线玩家有分数条目
            if (placed != null) {
                String uuid = player.getUUID().toString();
                int count = placedCounts.getOrDefault(uuid, 0);
                ScoreAccess access = sb.getOrCreatePlayerScore((ScoreHolder) player, placed);
                access.set(count);
            }
            // 在线时长：从 stats 获取 PLAY_TIME（单位：tick），转换为秒
            if (playTime != null) {
                int ticks = counter.getValue(Stats.CUSTOM, Stats.PLAY_TIME);
                int seconds = ticks / 20;
                ScoreAccess access = sb.getOrCreatePlayerScore((ScoreHolder) player, playTime);
                access.set(seconds);
            }
        }
    }

    /** 格式化时长显示（秒 -> 可读字符串）。 */
    public static String formatDuration(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;
        if (minutes < 60) {
            int remainingSeconds = seconds % 60;
            if (remainingSeconds == 0) {
                return minutes + "min";
            }
            return minutes + "min " + remainingSeconds + "s";
        }
        int hours = minutes / 60;
        minutes = minutes % 60;
        if (hours < 24) {
            if (minutes == 0) {
                return hours + "h";
            }
            return hours + "h " + minutes + "min";
        }
        int days = hours / 24;
        hours = hours % 24;
        if (hours == 0) {
            return days + "d";
        }
        return days + "d " + hours + "h";
    }

    /** 客户端请求显示某榜单 → 给该玩家单独发 display 包（不影响其他玩家）。 */
    public static void onClientRequest(ServerPlayer player, String objectiveName) {
        MinecraftServer server = ServerMain.server;
        if (server == null) {
            return;
        }
        Objective obj = objectiveName == null || objectiveName.isEmpty()
                ? null
                : server.getScoreboard().getObjective(objectiveName);
        // 干净重建客户端该 objective：先 REMOVE（客户端没有则忽略），再 ADD + 分数包
        if (obj != null) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetObjectivePacket(
                    obj, net.minecraft.network.protocol.game.ClientboundSetObjectivePacket.METHOD_REMOVE));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetObjectivePacket(
                    obj, net.minecraft.network.protocol.game.ClientboundSetObjectivePacket.METHOD_ADD));
            if (server.getScoreboard() instanceof net.minecraft.server.ServerScoreboard ssb) {
                for (var pkt : ssb.getStartTrackingPackets(obj)) {
                    if (pkt instanceof net.minecraft.network.protocol.game.ClientboundSetScorePacket) {
                        player.connection.send(pkt);
                    }
                }
            }
        }
        // 发 display 包（slot=SIDEBAR，objective 可为 null 表示清除）
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket(
                net.minecraft.world.scores.DisplaySlot.SIDEBAR, obj));
    }
}

/**
 * 自定义 NumberFormat，用于在线时长的客户端显示格式化。
 * 将秒数格式化为可读字符串（如 5s, 1min 30s, 2h 15min, 1d 3h）。
 */
class PlayTimeNumberFormat implements NumberFormat {
    
    @Override
    public MutableComponent format(int seconds) {
        return Component.literal(BoardManager.formatDuration(seconds));
    }
    
    @Override
    public NumberFormatType<PlayTimeNumberFormat> type() {
        // 返回 BlankFormat 的 TYPE，虽然类型参数不同，但在运行时是兼容的
        // 这是由于 Java 泛型擦除，且 BlankFormat.TYPE 用于简单显示
        @SuppressWarnings("unchecked")
        NumberFormatType<PlayTimeNumberFormat> type = (NumberFormatType<PlayTimeNumberFormat>) (Object) net.minecraft.network.chat.numbers.BlankFormat.TYPE;
        return type;
    }
}