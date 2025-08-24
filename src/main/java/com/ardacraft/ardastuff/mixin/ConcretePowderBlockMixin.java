package com.ardacraft.ardastuff.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.ConcretePowderBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting ConcretePowderBlock to prevent hardening into solid concrete.
 */
@Mixin(ConcretePowderBlock.class)
public class ConcretePowderBlockMixin
{
    /**
     * Forces shouldHarden to return false, disabling water-triggered hardening.
     */
    @Inject(method = "shouldHarden", at = @At("HEAD"), cancellable = true)
    private static void shouldHarden(BlockView world, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    /**
     * Forces hardensOnAnySide to return false, preventing adjacency checks from hardening.
     */
    @Inject(method = "hardensOnAnySide", at = @At("HEAD"), cancellable = true)
    private static void hardensOnAnySide(BlockView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
