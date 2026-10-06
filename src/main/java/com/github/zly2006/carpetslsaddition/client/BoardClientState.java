package com.github.zly2006.carpetslsaddition.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户端榜单状态（纯客户端，各玩家独立，不同步）。
 *
 * <ul>
 *   <li>{@link #displaying}：当前显示哪个榜（"" = 不显示）。</li>
 *   <li>{@code deaths}/{@code mined}：从服务端收到的排行数据缓存。</li>
 * </ul>
 */
public final class BoardClientState {

    private BoardClientState() {}

    /** 当前显示的榜："" / "deaths" / "mined"。 */
    public static volatile String displaying = "";

    private static volatile List<Entry> deaths = Collections.emptyList();
    private static volatile List<Entry> mined = Collections.emptyList();

    public record Entry(String name, int score) {}

    public static void setBoard(String board, List<Entry> entries) {
        if ("deaths".equals(board)) {
            deaths = entries;
        } else if ("mined".equals(board)) {
            mined = entries;
        }
    }

    public static List<Entry> current() {
        return switch (displaying) {
            case "deaths" -> deaths;
            case "mined" -> mined;
            default -> Collections.emptyList();
        };
    }

    public static void toggle(String board) {
        displaying = displaying.equals(board) ? "" : board;
    }

    public static void set(String board) {
        displaying = board;
    }

    public static String currentTitle() {
        return switch (displaying) {
            case "deaths" -> "死亡榜";
            case "mined" -> "挖掘榜";
            default -> "";
        };
    }
}