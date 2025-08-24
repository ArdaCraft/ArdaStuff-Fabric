package com.ardacraft.ardastuff.mixin;

import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents releasing bucketed entities (e.g., fish, axolotls) by cancelling
 * EntityBucketItem#spawnEntity. This blocks unwanted mob placement.
 */
@Mixin(EntityBucketItem.class)
public class EntityBucketItemMixin {

    @Inject(method = "spawnEntity", at = @At(value = "HEAD"), cancellable = true)
    public void cancelSpawnBucket(ServerWorld world, ItemStack stack, BlockPos pos, CallbackInfo ci) {
        ci.cancel();
    }

}
