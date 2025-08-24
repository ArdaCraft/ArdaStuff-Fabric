package com.ardacraft.ardastuff.mixin;

import net.minecraft.block.FarmlandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents farmland from turning back into dirt by cancelling FarmlandBlock#setToDirt.
 * Protects farmland from trampling and hydration loss mechanics.
 */
@Mixin(FarmlandBlock.class)
public class FarmlandBlockMixin {

    @Inject(at = @At("HEAD"), method = "setToDirt", cancellable = true)
    private static void setToDirt(CallbackInfo ci) {
        ci.cancel();
    }
}
