package com.github.zly2006.carpetslsaddition.util;

import com.github.zly2006.carpetslsaddition.ServerMain;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 假人名单管理器：维护一份"受控假人名字"列表，持久化到 config/slsa-bot-list.json。
 *
 * <p>用途：{@code /botlist add cute wazi} 把名字加入名单后，{@code /botall} 的操作范围
 * 就限定为名单内的假人（名单为空时操作所有 SLS 假人）。
 */
public final class BotListManager {

    private BotListManager() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 名字列表（保序、去重）。 */
    private static final Set<String> NAMES = new LinkedHashSet<>();

    private static Path configPath;

    private static Path path() {
        if (configPath == null) {
            configPath = FabricLoader.getInstance().getConfigDir().resolve("slsa-bot-list.json");
        }
        return configPath;
    }

    /** 加载名单（服务器启动时调用）。 */
    public static void load() {
        NAMES.clear();
        Path p = path();
        if (!Files.exists(p)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(p)) {
            Set<String> loaded = GSON.fromJson(r, new TypeToken<LinkedHashSet<String>>() {}.getType());
            if (loaded != null) {
                NAMES.addAll(loaded);
            }
            ServerMain.LOGGER.info("[SLSA] loaded bot list: {} entries", NAMES.size());
        } catch (Exception e) {
            ServerMain.LOGGER.error("[SLSA] failed to load bot list", e);
        }
    }

    /** 保存名单。 */
    public static void save() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                GSON.toJson(NAMES, w);
            }
        } catch (IOException e) {
            ServerMain.LOGGER.error("[SLSA] failed to save bot list", e);
        }
    }

    public static boolean add(String name) {
        boolean added = NAMES.add(name);
        if (added) save();
        return added;
    }

    public static boolean remove(String name) {
        boolean removed = NAMES.remove(name);
        if (removed) save();
        return removed;
    }

    public static void clear() {
        NAMES.clear();
        save();
    }

    /** 返回名单快照。 */
    public static Set<String> getNames() {
        return new LinkedHashSet<>(NAMES);
    }

    public static boolean contains(String name) {
        return NAMES.contains(name);
    }

    public static boolean isEmpty() {
        return NAMES.isEmpty();
    }
}