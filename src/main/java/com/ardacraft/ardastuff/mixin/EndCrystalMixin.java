package com.ardacraft.ardastuff.mixin;

import net.minecraft.entity.decoration.EndCrystalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;

/**
 * Mixin targeting EndCrystalEntity to prevent explosions and instead remove the crystal safely.
 */
@Mixin(EndCrystalEntity.class)
public abstract class EndCrystalMixin {

    @Shadow public abstract void kill();

    /**
     * On damage, immediately kill the crystal and report no handled damage to vanilla.
     *
     * @param source damage source
     * @param amount damage amount
     * @param cir    returnable; set to false to cancel further processing
     */
    @Inject(method = "damage", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    public void damage(net.minecraft.entity.damage.DamageSource source, float amount, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
       this.kill();
         cir.setReturnValue(false);
    }
}
