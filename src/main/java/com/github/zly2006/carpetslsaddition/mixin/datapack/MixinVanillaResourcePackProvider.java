package com.github.zly2006.carpetslsaddition.mixin.datapack;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.ServerPacksSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author zly2006
 *
 * TODO(迁移 26.1): 原 VanillaResourcePackProvider.forEachProfile 已不存在。
 *  26.1 数据包系统重构为 PackRepository/BuiltInPackSource 体系。
 *  此处需改为把 data/slsa/datapacks 作为内置数据包注册。
 *  当前暂以 ServerPacksSource 为挂载点占位。
 */
@Mixin(ServerPacksSource.class)
public abstract class MixinVanillaResourcePackProvider {

    @Inject(method = "createVanillaPackSource", at = @At("RETURN"), require = 0)
    private static void andMe(CallbackInfo ci) throws URISyntaxException {
        // TODO: 实现内置 datapack 注册
    }
}