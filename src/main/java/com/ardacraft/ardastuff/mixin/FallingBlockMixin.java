package com.ardacraft.ardastuff.mixin;

import net.minecraft.block.FallingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables falling block physics by cancelling FallingBlock#scheduledTick.
 * Prevents blocks like sand and gravel from updating into falling entities.
 */
@Mixin(FallingBlock.class)
public class FallingBlockMixin {

    /**
     * Cancel at method head to stop any further falling logic.
     */
    @Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
    private void scheduledTick(CallbackInfo ci) {
        ci.cancel();
    }
}
