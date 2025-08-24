package com.ardacraft.ardastuff.mixin;


import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting ProjectileEntity to suppress collision handling.
 * Prevents projectiles from applying their normal effects on collision (e.g., breaking frames).
 */
@Mixin(ProjectileEntity.class)
public class ProjectileEntityMixin {

    /**
     * Cancels ProjectileEntity#onCollision for all projectiles.
     *
     * @param hitResult the collision result
     * @param ci        mixin callback info
     */
    @Inject(at = @At("HEAD"), method = "onCollision", cancellable = true)
    public void onCollision(HitResult hitResult, CallbackInfo ci) {
        ci.cancel();
    }

}
