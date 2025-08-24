package com.ardacraft.ardastuff.mixin;

import net.minecraft.block.RespawnAnchorBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting RespawnAnchorBlock to suppress explosion behavior in invalid dimensions.
 */
@Mixin(RespawnAnchorBlock.class)
public class RespawnAnchorMixin {

    /**
     * Cancels explode to prevent damage and block destruction.
     */
    @Inject(at = @At("HEAD"), method = "explode", cancellable = true)
    public void explode(CallbackInfo ci) {
        ci.cancel();
    }
}
