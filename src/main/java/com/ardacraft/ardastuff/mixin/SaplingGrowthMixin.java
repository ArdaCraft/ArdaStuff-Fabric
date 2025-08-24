package com.ardacraft.ardastuff.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.SaplingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting SaplingBlock to disable natural and forced growth/generation.
 */
@Mixin(SaplingBlock.class)
public class SaplingGrowthMixin {

    /**
     * Prevents bonemeal growth by returning false.
     */
    @Inject(at = @At("HEAD"), method = "canGrow", cancellable = true)
    private void injected(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    /**
     * Cancels direct grow() calls.
     */
    @Inject(at = @At("HEAD"), method = "grow", cancellable = true)
    private void injected2(ServerWorld world, Random random, BlockPos pos, BlockState state, CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * Cancels tree generation attempts.
     */
    @Inject(at = @At("HEAD"), method = "generate", cancellable = true)
    private void injected3(ServerWorld world, BlockPos pos, BlockState state, Random random, CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * Cancels random ticking growth.
     */
    @Inject(at = @At("HEAD"), method = "randomTick", cancellable = true)
    private void injected4(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        ci.cancel();
    }
}
