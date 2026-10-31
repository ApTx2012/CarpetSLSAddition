package com.github.zly2006.carpetslsaddition.mixin.board;

import com.github.zly2006.carpetslsaddition.util.BoardManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 记录玩家放置方块的统计（用于放置榜）。
 * 在 handleUseItemOn 处理放置方块包后增加计数。
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl {

    @Inject(
            method = "handleUseItemOn",
            at = @At("TAIL")
    )
    private void onPlaceBlock(net.minecraft.network.protocol.game.ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).getPlayer();
        if (player == null) {
            return;
        }
        ItemStack stack = player.getItemInHand(packet.getHand());
        if (stack.getItem() instanceof BlockItem) {
            // 尝试放置方块，增加统计
            BoardManager.incrementPlaced(player);
        }
    }
}