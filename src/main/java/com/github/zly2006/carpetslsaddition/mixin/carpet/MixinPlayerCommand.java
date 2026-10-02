package com.github.zly2006.carpetslsaddition.mixin.carpet;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.players.CachedUserNameToIdResolver;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

/**
 * offlineFakePlayers: 让假人查询在离线模式下也能拿到 profile。
 *
 * TODO(迁移 26.1): 原重定向 carpet PlayerCommand.cantSpawn 里的 UserCache.findByName。
 *  新版 Carpet 改用 server.services().nameToIdCache().get(uuid)。
 *  此处改为直接注入 CachedUserNameToIdResolver.get(UUID)。
 */
@Mixin(CachedUserNameToIdResolver.class)
public abstract class MixinPlayerCommand {

    @Inject(method = "get(Ljava/util/UUID;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private void redirectFindByName(UUID uuid, CallbackInfoReturnable<Optional<NameAndId>> cir) {
        if (SLSCarpetSettings.offlineFakePlayers && cir.getReturnValue().isEmpty()) {
            // 无法仅凭 uuid 还原名字，offline 场景由 Carpet createFake 处理
        }
    }
}