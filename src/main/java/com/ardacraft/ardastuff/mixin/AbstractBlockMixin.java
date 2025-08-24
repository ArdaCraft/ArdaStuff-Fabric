package com.ardacraft.ardastuff.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting AbstractBlock to intercept projectile hits.
 * Cancels AbstractBlock#onProjectileHit so projectiles do not trigger block-specific behavior
 * (e.g., preventing accidental block interactions from arrows).
 */
@Mixin(AbstractBlock.class)
public class AbstractBlockMixin {

    /**
     * Cancels all projectile-hit handling on blocks.
     *
     * @param world       the world
     * @param state       the block state hit
     * @param hit         the block hit result
     * @param projectile  the projectile that collided
     * @param ci          callback to cancel original method
     */
    @Inject(at = @At("HEAD"), method = "onProjectileHit", cancellable = true)
    private void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile, CallbackInfo ci) {
        ci.cancel();
    }

}
