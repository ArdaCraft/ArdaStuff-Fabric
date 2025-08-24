package com.ardacraft.ardastuff.mixin;


import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;

import static net.minecraft.world.Heightmap.populateHeightmaps;

/**
 * Mixin targeting Heightmap to avoid noisy warnings and repopulate on load.
 */
@Mixin(Heightmap.class)
public class HeightMapMixin {

    /**
     * Intercepts warning log call and repopulates heightmaps for the given type instead.
     *
     * @param chunk  chunk to repopulate
     * @param type   heightmap type
     * @param values raw values
     * @param ci     mixin callback info
     */
    @Inject(method = "setTo", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;)V"), cancellable = true)
    public void suppressWarning(Chunk chunk, Heightmap.Type type, long[] values, CallbackInfo ci) {
        populateHeightmaps(chunk, EnumSet.of(type));
        ci.cancel();
    }
}
