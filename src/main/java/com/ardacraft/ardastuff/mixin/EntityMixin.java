package com.ardacraft.ardastuff.mixin;

import net.luckperms.api.LuckPermsProvider;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting Entity to prevent players from pushing entities unless permitted.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    /**
     * Cancels Entity#pushAwayFrom when the pusher is a ServerPlayerEntity without the
     * "ardastuff.allow.entitypush" permission node.
     *
     * @param entity the entity attempting to push this entity
     * @param ci     mixin callback info
     */
    @Inject(
            method = "pushAwayFrom",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventPlayerPushing(Entity entity, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayerEntity) return; // Player being pushed — fine
        if (entity instanceof ServerPlayerEntity player) {

            // Check permission
            boolean hasPermission = LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class)
                    .getPermissionData(player)
                    .checkPermission("ardastuff.allow.entitypush")
                    .asBoolean();

            if (!hasPermission) {
                ci.cancel(); // Cancel the push
            }
        }
    }
}
