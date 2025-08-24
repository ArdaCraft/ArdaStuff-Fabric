package com.ardacraft.ardastuff.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting LilyPadBlock to prevent boats from breaking lily pads.
 */
@Mixin(LilyPadBlock.class)
public abstract class LilyPadBlockMixin {

    /**
     * Cancels onEntityCollision for boats to preserve lily pads.
     */
    @Inject(
            method = "onEntityCollision",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventBoatBreakingLilyPad(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (entity instanceof BoatEntity) {
            // Prevent the default behavior that breaks the block
            ci.cancel();
        }
    }
}
