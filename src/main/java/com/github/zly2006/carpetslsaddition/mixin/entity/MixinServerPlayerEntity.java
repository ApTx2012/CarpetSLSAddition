package com.github.zly2006.carpetslsaddition.mixin.entity;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import com.github.zly2006.carpetslsaddition.util.SitEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayerEntity extends Player {
    @Shadow
    public ServerGamePacketListenerImpl connection;

    @Shadow public abstract boolean isSpectator();

    @Unique
    private int sneakTimes = 0;
    @Unique
    private long lastSneakTime = 0;

    public MixinServerPlayerEntity(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, gameProfile);
    }


    @Override
    public void setShiftKeyDown(boolean sneaking){
        if (!SLSCarpetSettings.playerSit || (sneaking && this.isShiftKeyDown())) {
            super.setShiftKeyDown(sneaking);
            return;
        }

        if (sneaking) {
            long nowTime = System.currentTimeMillis();
            if (nowTime - lastSneakTime < 400 && sneakTimes == 0) {
                return;
            }
            super.setShiftKeyDown(true);
            if (this.onGround() && nowTime - lastSneakTime < 400) {
                sneakTimes += 1;
                if (sneakTimes == 3) {
                    ArmorStand armorStandEntity = new ArmorStand(this.level(), this.getX(), this.getY() - 0.16, this.getZ());
                    ((SitEntity) armorStandEntity).setSitEntity(true);
                    this.level().addFreshEntity(armorStandEntity);
                    this.setShiftKeyDown(false);
                    this.startRiding(armorStandEntity);
                    sneakTimes = 0;
                }
            } else {
                sneakTimes = 1;
            }
            lastSneakTime = nowTime;
        } else {
            super.setShiftKeyDown(false);
            // 同步潜行状态到客户端
            if (sneakTimes == 0 && this.connection != null) {
                this.connection.send(new ClientboundSetEntityDataPacket(this.getId(), this.getEntityData().packDirty()));
            }
        }
    }
}