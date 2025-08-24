package com.ardacraft.ardastuff.mixin;

import net.minecraft.block.FarmlandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting FarmlandBlock to prevent trampling reverting it to dirt.
 */
@Mixin(FarmlandBlock.class)
public class FarmlandBlockMixin {

    /**
     * Cancels setToDirt so farmland remains tilled.
     *
     * @param ci mixin callback info
     */
    @Inject(at = @At("HEAD"), method = "setToDirt", cancellable = true)
    private static void setToDirt(CallbackInfo ci) {
        ci.cancel();
    }
}
