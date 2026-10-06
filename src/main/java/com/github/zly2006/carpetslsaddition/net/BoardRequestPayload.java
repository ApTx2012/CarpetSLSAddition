package com.github.zly2006.carpetslsaddition.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：请求显示某个榜单（或关闭）。
 *
 * <p>服务端收到后，给该玩家单独发送 {@code ClientboundSetDisplayObjectivePacket}，
 * 实现"各玩家各自显示、互不影响"。
 *
 * <p>{@code objectiveName} 为空字符串表示关闭。
 */
public record BoardRequestPayload(String objectiveName) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BoardRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("slsa", "board_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoardRequestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BoardRequestPayload::objectiveName,
            BoardRequestPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
    }
}