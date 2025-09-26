package com.ardacraft.ardastuff.mixin;

import com.ardacraft.ardastuff.ArdaStuff;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    /**
     * Prevents damage to entities by players without the "ardastuff.allow.entitydamage" permission.
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void preventDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity attacker = source.getAttacker();
        if (attacker instanceof PlayerEntity player && !ArdaStuff.hasPermission(player, "ardastuff.allow.entitydamage")) {
            cir.setReturnValue(false);
        }
    }
}
