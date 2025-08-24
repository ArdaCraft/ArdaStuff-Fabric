package com.ardacraft.ardastuff.mixin;


import net.minecraft.entity.passive.ChickenEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the chicken egg-laying sound by cancelling the relevant invocation
 * within ChickenEntity#tickMovement. This reduces ambient noise from chickens.
 */
@Mixin(ChickenEntity.class)
public class ChickenMixin {

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/ChickenEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V"),
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/entity/passive/ChickenEntity;flapSpeed:F", ordinal = 3), to = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I")),
            cancellable = true)
    private void injected(CallbackInfo ci) {
        ci.cancel();
    }



}
