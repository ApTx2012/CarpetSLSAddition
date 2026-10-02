package com.github.zly2006.carpetslsaddition.mixin.screen;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilMenu.class)
public class MixinAnvilScreenHandler {
    @Redirect(
            method = "createResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getAbilities()Lnet/minecraft/world/entity/player/Abilities;"
            ),
            require = 0
    )
    private Abilities updateResult(Player instance) {
        if (SLSCarpetSettings.creativeObeyEnchantmentRule) {
            var ability = new Abilities();
            ability.instabuild = false;
            return ability;
        }

        return instance.getAbilities();
    }
}