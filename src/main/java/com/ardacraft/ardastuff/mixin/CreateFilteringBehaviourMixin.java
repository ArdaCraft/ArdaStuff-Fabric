package com.ardacraft.ardastuff.mixin;


import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.luckperms.api.LuckPermsProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting Create's BlockEntityConfigurationPacket to permission-gate config changes.
 * Requires the LuckPerms node "metatweaks.createconfig" to allow handling.
 */
@Mixin(BlockEntityConfigurationPacket.class)
abstract class CreateFilteringBehaviourMixin {

    /**
     * Intercepts packet handling and cancels if the sender lacks the required permission.
     *
     * @param context packet context (provides sender)
     * @param cir     returnable for success; set true and cancel to short-circuit
     */
    @Inject(method = "handle", at = @At(value = "HEAD"), remap = false, cancellable = true)
    private void handle(SimplePacketBase.Context context, CallbackInfoReturnable<Boolean> cir) {
        if(!LuckPermsProvider.get().getUserManager().getUser(context.getSender().getUuid()).getCachedData().getPermissionData().checkPermission("metatweaks.createconfig").asBoolean()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

}
