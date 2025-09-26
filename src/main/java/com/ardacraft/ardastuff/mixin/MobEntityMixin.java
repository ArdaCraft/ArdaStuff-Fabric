package com.ardacraft.ardastuff.mixin;

import com.ardacraft.ardastuff.ArdaStuff;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public class MobEntityMixin {
    /**
     * Prevents leashing of mobs by players without the "ardastuff.allow.leashing" permission.
     */
    @Inject(method = "attachLeash", at = @At("HEAD"), cancellable = true)
    private void preventLeashing(Entity entity, boolean sendPacket, CallbackInfo ci) {
        if (entity instanceof ServerPlayerEntity player && !ArdaStuff.hasPermission(player, "ardastuff.allow.leashing")) {
            ci.cancel();

            player.networkHandler.sendPacket(new EntityAttachS2CPacket((Entity) (Object) this, null));
        }
    }
}
