package com.ardacraft.ardastuff.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderPearlEntity.class)
public class EnderPearlEntityMixin {
    @Unique
    private Entity mount;

    /**
     * Captures a player's mount when a thrown ender pearl lands.
     */
    @Inject(method = "onCollision", at = @At("HEAD"))
    private void captureMount(HitResult hitResult, CallbackInfo ci) {
        Entity entity = ((EnderPearlEntity) (Object) this).getOwner();
        if (entity instanceof ServerPlayerEntity && entity.hasVehicle()) {
            Entity mount = entity.getVehicle();
            Text text = mount != null ? mount.getCustomName() : null;
            if (text != null && text.getString().equals("deleteme")) {
                this.mount = mount;
            }
        }
    }

    /**
     * Discards a player's mount, if necessary, when a thrown ender pearl lands.
     */
    @Inject(
            method = "onCollision",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;requestTeleportAndDismount(DDD)V",
                    shift = At.Shift.AFTER
            )
    )
    private void afterTeleport(CallbackInfo ci, @Local Entity entity) {
        if (mount != null) {
            mount.discard();
            this.mount = null;
        }
    }
}
