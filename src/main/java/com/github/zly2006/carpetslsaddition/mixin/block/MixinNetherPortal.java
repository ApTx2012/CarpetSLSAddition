package com.github.zly2006.carpetslsaddition.mixin.block;

import com.github.zly2006.carpetslsaddition.SLSCarpetSettings;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PortalShape.class)
public class MixinNetherPortal {
    @ModifyConstant(
            method = "*",
            constant = @Constant(intValue = 21),
            require = 0
    )
    private int portalSize(int original) {
        return SLSCarpetSettings.netherPortalSize;
    }
}