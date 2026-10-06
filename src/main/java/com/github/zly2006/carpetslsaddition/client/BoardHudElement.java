package com.github.zly2006.carpetslsaddition.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 右侧排行榜 HUD：显示死亡榜/挖掘榜的 Top N。
 *
 * <p>纯客户端渲染，内容来自 {@link BoardClientState}（服务端同步而来）。
 */
@Environment(EnvType.CLIENT)
public class BoardHudElement implements HudElement {

    private static final int LINE_H = 10;
    private static final int PAD = 4;
    private static final int BG = 0x80000000;       // 半透明黑
    private static final int TITLE_COLOR = 0xFFFF55; // 黄
    private static final int TEXT_COLOR = 0xFFFFFF;  // 白

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        String board = BoardClientState.displaying;
        com.github.zly2006.carpetslsaddition.ServerMain.LOGGER.info("[SLSA-HUD] extractRenderState board={}", board);
        if (board == null || board.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        List<BoardClientState.Entry> list = BoardClientState.current();
        Font font = mc.font;

        String title = BoardClientState.currentTitle();
        int maxW = font.width(title);
        for (BoardClientState.Entry e : list) {
            maxW = Math.max(maxW, font.width(line(e)));
        }

        int screenW = g.guiWidth();
        int screenH = g.guiHeight();
        int x = screenW - maxW - PAD * 2 - 2;
        // 背景（右侧垂直居中）
        int h = PAD * 2 + LINE_H * (list.size() + 1);
        int y = screenH / 2 - h / 2;
        g.fill(x, y, x + maxW + PAD * 2, y + h, BG);

        int ty = y + PAD;
        g.text(font, Component.literal(title), x + PAD, ty, TITLE_COLOR);
        ty += LINE_H;
        for (int i = 0; i < list.size(); i++) {
            BoardClientState.Entry e = list.get(i);
            g.text(font, Component.literal(line(e)), x + PAD, ty, TEXT_COLOR);
            ty += LINE_H;
        }
    }

    private static String line(BoardClientState.Entry e) {
        String name = e.name();
        if (name.length() > 12) name = name.substring(0, 12);
        return name + " " + e.score();
    }
}
