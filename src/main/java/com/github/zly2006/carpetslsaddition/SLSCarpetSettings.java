package com.github.zly2006.carpetslsaddition;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;

public class SLSCarpetSettings {
    public static final String NEED_CLIENT = "needClient";  // 需要客户端安装SLS-Addition或实现相关支持
    public static final String FROM_AMS = "AMS";
    public static final String SLSA = "SLS";
    public static final String LEGACY = "legacy";

    @Rule(categories = {SLSA, RuleCategory.SURVIVAL})
    public static boolean obtainableReinforcedDeepSlate = false;

    @Rule(categories = {SLSA, RuleCategory.CREATIVE})
    public static boolean creativeNoInfinitePickup = false;

    @Rule(categories = {SLSA, RuleCategory.OPTIMIZATION})
    public static boolean noBatSpawning = false;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static boolean canUseHatCommand = false;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static boolean canUseSitCommand = false;

    @Rule(categories = {SLSA}, strict = false, options = {"#none", "bot_"})
    public static String botPrefix = "#none";

    @Rule(categories = {SLSA})
    public static long botMaxOnlineTime = -1;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static boolean creativeObeyEnchantmentRule = false;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static boolean fakePlayersNotOccupiedSleepQuota = false;

    @Rule(categories = {SLSA, RuleCategory.FEATURE, NEED_CLIENT})
    public static boolean emptyShulkerBoxStack = false;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static boolean playerSit = false;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static boolean endermanCanPickUpMushroom = true;

    @Rule(categories = {SLSA, RuleCategory.CREATIVE})
    public static boolean oldRedstoneConnectionLogic = false;

    // Ported from Carpet AMS Addition
    @Rule(categories = {SLSA, FROM_AMS, RuleCategory.OPTIMIZATION})
    public static boolean optimizedOnDragonRespawn = false;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static boolean elytraCraftable = false;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static boolean spectatorCannotUseLeash = false;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static boolean armadilloImmediateDespawns = false;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static boolean offlineFakePlayers = false;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static int netherPortalSize = 21;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static int trialSpawnerCD = -1;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static boolean turtleLocalNesting = false;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static boolean sturdyDecoratedPot = false;

    @Rule(categories = {SLSA, RuleCategory.FEATURE})
    public static boolean unbreakableDecoratedPot = false;
    // ===== [旧版] 遗留漏洞机制复刻 =====
    // 总开关：开启后，其余子规则才生效
    @Rule(categories = {LEGACY}, strict = false)
    public static boolean legacyExploitMode = false;

    // 等价 instantTick：计划刻（scheduleTick）立即执行，不等延迟
    @Rule(categories = {LEGACY})
    public static boolean legacyInstantTick = false;

    // 等价 instantFall：重力方块被计划刻触发时立即下落
    @Rule(categories = {LEGACY})
    public static boolean legacyInstantFall = false;

    // 自建后台线程，模拟 1.12 信标线程并发访问 chunk
    @Rule(categories = {LEGACY})
    public static boolean legacyAsyncChunkAccess = false;

    // 允许异步线程在 chunk 表竞态时把"已加载"当成"未加载"，触发重新加载/装饰
    @Rule(categories = {LEGACY})
    public static boolean legacyChunkSwap = false;

    // 落沙替换：在 FallingBlock 转实体窗口内把方块替换成目标方块
    @Rule(categories = {LEGACY})
    public static boolean legacyFallingBlockReplace = false;

    // 目标方块（替换结果），默认末地传送门框架
    @Rule(categories = {LEGACY}, strict = false, options = {"#none", "end_portal_frame", "command_block", "spawner", "barrier", "nether_portal"})
    public static String legacyFallingBlockTarget = "#none";
}
