package com.ardacraft.ardastuff.mixin;

import net.minecraft.block.CropBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disables crop growth by forcing CropBlock#canGrow to return false.
 * Prevents random and bonemeal growth to enforce build server rules.
 */
@Mixin(CropBlock.class)
public class CropGrowthMixin {


    @Inject(at = @At("HEAD"), method = "canGrow", cancellable = true)
    private void injected(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
