package com.ardacraft.ardastuff.mixin;

import net.minecraft.block.FallingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting FallingBlock to disable gravity updates (e.g., sand/gravel falling).
 */
@Mixin(FallingBlock.class)
public class FallingBlockMixin {

    /**
     * Cancels the scheduledTick to stop falling behavior.
     *
     * @param ci mixin callback info
     */
    @Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
    private void scheduledTick(CallbackInfo ci) {
        ci.cancel();
    }
}
