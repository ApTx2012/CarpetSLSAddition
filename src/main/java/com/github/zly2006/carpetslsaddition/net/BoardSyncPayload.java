package com.github.zly2006.carpetslsaddition.net;

import com.github.zly2006.carpetslsaddition.ServerMain;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * 服务端 → 客户端：同步排行榜数据。
 *
 * <p>携带某一种榜单（死亡/挖掘）的完整排行：条目为 (玩家名, 分数)，已按分数降序。
 * 客户端收到后缓存，供 HUD 渲染。
 */
public record BoardSyncPayload(String board, List<Entry> entries) implements CustomPacketPayload {

    /** 单条排行数据。 */
    public record Entry(String name, int score) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::name,
                ByteBufCodecs.VAR_INT, Entry::score,
                Entry::new
        );
    }

    public static final CustomPacketPayload.Type<BoardSyncPayload> TYPE =
            CustomPacketPayload.createType("slsa:board_sync");

    public static final StreamCodec<RegistryFriendlyByteBuf, BoardSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BoardSyncPayload::board,
            Entry.CODEC.apply(ByteBufCodecs.list()), BoardSyncPayload::entries,
            BoardSyncPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 在 onInitialize 里调用，注册 payload 类型。 */
    public static void register() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
        ServerMain.LOGGER.info("[SLSA] registered BoardSyncPayload");
    }
}