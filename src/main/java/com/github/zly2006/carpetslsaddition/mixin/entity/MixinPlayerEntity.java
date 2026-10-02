package com.github.zly2006.carpetslsaddition.mixin.entity;

import com.github.zly2006.carpetslsaddition.util.access.PlayerAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayerEntity implements PlayerAccessor {
    @Unique
    private Component displayName = null;

    @Override
    public void carpet_SLS_Addition$setDisplayName(Component name) {
        this.displayName = name;
    }

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    public void injectGetDisplayName(CallbackInfoReturnable<Component> cir) {
        if (this.displayName != null) {
            cir.setReturnValue(displayName);
        }
    }
}