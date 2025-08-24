package com.ardacraft.ardastuff.mixin;


import net.minecraft.entity.passive.ChickenEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting ChickenEntity to suppress random egg-laying sound/effects.
 * Cancels a specific invocation within tickMovement that triggers playSound during flap cycles.
 */
@Mixin(ChickenEntity.class)
public class ChickenMixin {

    /**
     * Cancels the targeted section of tickMovement to prevent sound and related behavior.
     *
     * @param ci mixin callback info used to cancel further execution in the slice
     */
    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/ChickenEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V"),
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/entity/passive/ChickenEntity;flapSpeed:F", ordinal = 3), to = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I")),
            cancellable = true)
    private void injected(CallbackInfo ci) {
        ci.cancel();
    }
}
