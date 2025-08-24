package com.ardacraft.ardastuff.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.util.collection.SortedArraySet;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin targeting ChunkTicketManager to make ticket removal null-safe.
 * Prevents a potential NullPointerException when attempting to remove from a null SortedArraySet.
 */
@Mixin(ChunkTicketManager.class)
public class ChunkTicketManagerMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Redirects SortedArraySet#remove to guard against null sets.
     *
     * @param set    the set from which to remove
     * @param ticket the ticket object
     * @return false if set is null; otherwise the result of set.remove
     */
    @Redirect(
            method = "removeTicket(JLnet/minecraft/server/world/ChunkTicket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/collection/SortedArraySet;remove(Ljava/lang/Object;)Z"
            )
    )
    private boolean safeRemoveTicket(SortedArraySet<?> set, Object ticket) {
        if (set == null) {
            LOGGER.error("[TicketManager Safety] Attempted to remove a ticket from a null SortedArraySet!", new Exception("Stacktrace"));
            return false;
        }
        return set.remove(ticket);
    }
}