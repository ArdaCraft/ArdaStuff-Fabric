package com.ardacraft.ardastuff.mixin;


import com.ardacraft.ardastuff.ArdaStuff;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.fabric.FabricAdapter;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Controls fluid spread updates. When ArdaStuff.disableWaterSpread is enabled, this mixin cancels
 * fluid scheduled ticks globally to prevent water/lava from spreading. It provides a whitelist
 * mechanism: if the fluid update position lies within a WorldEdit selection of any player present
 * in ArdaStuff.waterSpreaders, the update is allowed and not cancelled.
 *
 * Implementation details:
 * - Target: FluidState#onScheduledTick via the invocation of Fluid#onScheduledTick.
 * - Logic: Iterate whitelisted players, query their WorldEdit selection (if any), and permit
 *   spread only when the BlockPos is contained within at least one selection. Otherwise, cancel.
 */
@Mixin(FluidState.class)
public class FluidSpreadMixin {

    /**
     * Intercepts fluid scheduled tick right before delegating to Fluid#onScheduledTick. If water spread
     * is globally disabled and the position is not within a whitelisted WorldEdit selection, cancel the tick.
     */
    @Inject(method = "onScheduledTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/Fluid;onScheduledTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/fluid/FluidState;)V"), cancellable = true)
    public void onWaterSpread(World world, BlockPos pos, CallbackInfo ci) {
        if (ArdaStuff.disableWaterSpread) {
            boolean flag = true;
            for (ServerPlayerEntity player : ArdaStuff.waterSpreaders) {
                LocalSession session = WorldEdit.getInstance().getSessionManager().findByName(player.getGameProfile().getName());
                try {
                    if (session != null && session.getSelection().contains(FabricAdapter.adapt(pos))) {
                        flag = false;
                        break;
                    }
                } catch (IncompleteRegionException e) {
                    e.printStackTrace();
                }
            }
            if (flag) {
                ci.cancel();
            }
        }
    }
}
