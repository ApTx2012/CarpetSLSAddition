package com.github.zly2006.carpetslsaddition.util;

import com.github.zly2006.carpetslsaddition.ServerMain;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * 死亡榜 / 挖掘榜的 scoreboard 管理器（服务端）。
 *
 * <ul>
 *   <li>{@code sls_deaths}：使用原版 {@link ObjectiveCriteria#DEATH_COUNT} 判据，自动统计死亡次数。</li>
 *   <li>{@code sls_mined}：使用 {@link ObjectiveCriteria#DUMMY} 判据，由 {@code BoardEvents} 在破坏方块时写入。</li>
 * </ul>
 *
 * <p>objective 在服务器启动时自动创建；客户端会同步到这两个 objective，用于 HUD 显示。
 */
public final class BoardManager {

    private BoardManager() {}

    public static final String OBJ_DEATHS = "sls_deaths";
    public static final String OBJ_MINED = "sls_mined";

    /** 确保两个 objective 存在（服务器启动/重载时调用）。 */
    public static void ensureObjectives(MinecraftServer server) {
        Scoreboard sb = server.getScoreboard();
        if (sb.getObjective(OBJ_DEATHS) == null) {
            sb.addObjective(
                    OBJ_DEATHS,
                    ObjectiveCriteria.DEATH_COUNT,
                    Component.literal("死亡榜"),
                    ObjectiveCriteria.RenderType.INTEGER,
                    true,
                    null
            );
            ServerMain.LOGGER.info("[SLSA] created scoreboard objective {}", OBJ_DEATHS);
        }
        if (sb.getObjective(OBJ_MINED) == null) {
            sb.addObjective(
                    OBJ_MINED,
                    ObjectiveCriteria.DUMMY,
                    Component.literal("挖掘榜"),
                    ObjectiveCriteria.RenderType.INTEGER,
                    true,
                    null
            );
            ServerMain.LOGGER.info("[SLSA] created scoreboard objective {}", OBJ_MINED);
        }
    }

    /** 玩家破坏一个方块 → 挖掘榜 +1。 */
    public static void onBlockMined(ServerPlayer player, int count) {
        MinecraftServer server = ServerMain.server;
        if (server == null) {
            return;
        }
        Scoreboard sb = server.getScoreboard();
        Objective obj = sb.getObjective(OBJ_MINED);
        if (obj == null) {
            return;
        }
        ScoreAccess access = sb.getOrCreatePlayerScore((ScoreHolder) player, obj);
        access.add(count);
    }
}