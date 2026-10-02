package com.github.zly2006.carpetslsaddition.mixin.world;

import carpet.patches.EntityPlayerMPFake;
import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SleepStatus.class)
public class MixinSleepManager {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"))
    private boolean isNormalPlayer(ServerPlayer serverPlayerEntity) {
        if (SLSCarpetSettings.fakePlayersNotOccupiedSleepQuota) {
            return serverPlayerEntity.isSpectator() || serverPlayerEntity instanceof EntityPlayerMPFake;
        }
        else {
            return serverPlayerEntity.isSpectator();
        }
    }
}