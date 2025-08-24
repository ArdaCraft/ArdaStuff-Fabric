package com.ardacraft.ardastuff.mixin;

import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting ItemFrameEntity to ensure frames remain attached regardless of backing block changes.
 */
@Mixin(ItemFrameEntity.class)
public class ItemFrameEntityMixin {

    /**
     * Forces canStayAttached to return true.
     */
    @Inject(method = "canStayAttached", at = @At("HEAD"), cancellable = true)
    public void onCanStayAttached(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
