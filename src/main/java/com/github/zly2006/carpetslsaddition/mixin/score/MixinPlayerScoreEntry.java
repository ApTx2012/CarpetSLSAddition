package com.github.zly2006.carpetslsaddition.mixin.score;

import com.github.zly2006.carpetslsaddition.util.BoardManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerScoreEntry.class)
public class MixinPlayerScoreEntry {

    @Redirect(
            method = "formatValue",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/numbers/NumberFormat;format(I)Lnet/minecraft/network/chat/MutableComponent;")
    )
    private net.minecraft.network.chat.MutableComponent redirectFormat(NumberFormat format, int value) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                Objective obj = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
                if (obj != null && BoardManager.OBJ_PLAY_TIME.equals(obj.getName())) {
                    return Component.literal(BoardManager.formatDuration(value));
                }
            }
        } catch (Throwable ignored) {
        }
        return format.format(value);
    }
}