package com.github.zly2006.carpetslsaddition.mixin.legacy;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.ServerMain;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [旧版] 区块替换（chunk swap）——race condition 的等价实现。
 *
 * <p>1.12 的漏洞：{@code ChunkProviderServer.id2ChunkMap}（非线程安全的
 * {@code Long2ObjectOpenHashMap}）被主线程 {@code remove}（卸载）与异步线程 {@code get}
 * 并发访问，导致 {@code get} 错过已加载的区块，进而触发"重新加载 + 世界装饰"。
 *
 * <p>26.1.2 已用 {@code Long2ObjectLinkedOpenHashMap} + {@code mainThreadExecutor} 约束修复。
 * 直接并发改 map 风险极高（可能崩溃/损坏存档），故本 mixin 采用<b>低风险等价方案</b>：
 * hook {@link ChunkMap#getVisibleChunkIfPresent(long)}，当满足以下条件时对<b>非主线程</b>调用返回 {@code null}：
 * <ul>
 *   <li>{@code legacyExploitMode} + {@code legacyChunkSwap} 均开启</li>
 *   <li>当前不是服务器主线程（即异步线程在访问）</li>
 * </ul>
 * 这样异步线程会"误以为区块未加载"，后续逻辑会尝试重新加载并触发世界装饰，
 * 从而在异步线程上产生方块更新——与 1.12 的效果等价，但不触碰共享数据结构。
 *
 * <p><b>⚠ 风险</b>：仍可能引起区块状态不一致。仅在明确开启时生效。
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Inject(method = "getVisibleChunkIfPresent", at = @At("RETURN"), cancellable = true)
    private void legacyChunkSwap(long chunkPos, CallbackInfoReturnable<ChunkHolder> cir) {
        if (cir.getReturnValue() == null) {
            return;
        }
        if (!SLSCarpetSettings.legacyExploitMode || !SLSCarpetSettings.legacyChunkSwap) {
            return;
        }
        // 仅在异步线程访问时伪装"区块未加载"
        if (!isServerMainThread()) {
            cir.setReturnValue(null);
        }
    }

    /** 判断当前是否在服务器主线程上。 */
    private static boolean isServerMainThread() {
        net.minecraft.server.MinecraftServer server = ServerMain.server;
        if (server == null) {
            return true;
        }
        return server.isSameThread();
    }
}