package com.ardacraft.ardastuff.mixin;

import com.mojang.datafixers.DataFixerBuilder;
import net.minecraft.datafixer.Schemas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Schemas.class)
public class SchemaMixin {

    /**
     * Overwrites the DFU schema builder hook to intentionally no-op.
     * This can be used to bypass vanilla datafixer schema registration in specific environments.
     *
     * @author ArdaStuff
     * @reason Avoid costly DFU schema registration during startup
     * @param builder the DataFixerBuilder provided by Minecraft
     */
    @Overwrite
    private static void build(DataFixerBuilder builder) {
        // oh, no!
    }
}
