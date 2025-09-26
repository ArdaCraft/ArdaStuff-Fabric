package com.ardacraft.ardastuff.mixin;

import com.ardacraft.ardastuff.ArdaStuff;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /**
     * Prevents entity collisions for players without the "ardastuff.allow.entitypush" permission.
     */
    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void preventEntityCollisions(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ServerPlayerEntity player && !ArdaStuff.hasPermission(player, "ardastuff.allow.entitypush")) {
            ci.cancel();
        } else if (other instanceof ServerPlayerEntity player && !ArdaStuff.hasPermission(player, "ardastuff.allow.entitypush")) {
            ci.cancel();
        }
    }
}
