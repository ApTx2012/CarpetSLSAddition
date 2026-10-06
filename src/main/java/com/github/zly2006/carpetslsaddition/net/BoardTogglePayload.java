package com.github.zly2006.carpetslsaddition.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 服务端 → 客户端：榜单功能总开关状态（对应 Carpet 规则 {@code slsBoardEnabled}）。
 *
 * <p>规则变化时下发；客户端收到后决定 {@code /slsboard} 是否可用。
 */
public record BoardTogglePayload(boolean enabled) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BoardTogglePayload> TYPE =
            CustomPacketPayload.createType("slsa:board_toggle");

    public static final StreamCodec<RegistryFriendlyByteBuf, BoardTogglePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BoardTogglePayload::enabled,
            BoardTogglePayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
    }
}