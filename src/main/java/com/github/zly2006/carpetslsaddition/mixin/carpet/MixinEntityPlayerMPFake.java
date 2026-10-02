package com.github.zly2006.carpetslsaddition.mixin.carpet;

import carpet.patches.EntityPlayerMPFake;
import carpet.utils.Translations;
import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.ServerMain;
import com.github.zly2006.carpetslsaddition.util.access.SLSBotAccessor;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerMPFake.class)
public abstract class MixinEntityPlayerMPFake extends ServerPlayer implements SLSBotAccessor {
    @Unique
    private boolean bot = false;
    @Unique
    private long spawnTime = -1;

    public MixinEntityPlayerMPFake(MinecraftServer server, ServerLevel world, GameProfile profile, ClientInformation clientOptions) {
        super(server, world, profile, clientOptions);
    }

    @Override
    public boolean carpet_SLS_Addition$isBot() {
        return bot;
    }

    @Override
    public void carpet_SLS_Addition$setBot(boolean isBot) {
        this.bot = isBot;
    }

    @Override
    public long carpet_SLS_Addition$getSpawnTime() {
        if (spawnTime == -1) spawnTime = System.currentTimeMillis();
        return spawnTime;
    }

    @Override
    public void carpet_SLS_Addition$setSpawnTime(long spawnTime) {
        this.spawnTime = spawnTime;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        if (!this.bot || SLSCarpetSettings.botMaxOnlineTime < 0) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long liveTime = currentTime - spawnTime;

        if (liveTime > SLSCarpetSettings.botMaxOnlineTime * 1000) {
            ServerMain.server.getPlayerList().broadcastSystemMessage(
                    Component.literal(Translations.tr("carpet.slsa.bot.bot_timeout").formatted(this.getScoreboardName(), getFormattedTime(SLSCarpetSettings.botMaxOnlineTime)))
                            .setStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.RED)),
                    false
            );

            this.kill((net.minecraft.server.level.ServerLevel) this.level());
        }
    }

    @Unique
    private static String getFormattedTime(long time) {
        if (time > 120 * 60) {
            return String.format("%02dh%02dm%02ds", time / 3600, (time % 3600) / 60, time % 60);
        }
        else if (time > 120) {
            return String.format("%02dm%02ds", time / 60, time % 60);
        }
        else {
            return String.format("%02ds", time);
        }
    }
}