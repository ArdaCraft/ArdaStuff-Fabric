package com.ardacraft.ardastuff.mixin;

import net.minecraft.block.CropBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting CropBlock to disable crop growth via bonemeal or random ticks.
 */
@Mixin(CropBlock.class)
public class CropGrowthMixin {

    /**
     * Forces canGrow to return false, preventing growth attempts.
     */
    @Inject(at = @At("HEAD"), method = "canGrow", cancellable = true)
    private void injected(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
