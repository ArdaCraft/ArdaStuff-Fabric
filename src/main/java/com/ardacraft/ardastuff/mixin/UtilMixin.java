package com.ardacraft.ardastuff.mixin;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Util.class)
public class UtilMixin {

    /**
     * Overwrites Util#getChoiceType to return null in order to disable DFU choice type lookup.
     * This is part of the mod's strategy to avoid datafixer processing in certain cases.
     *
     * @author ArdaStuff
     * @reason Skip DFU choice type resolution to reduce startup overhead
     * @param typeReference the DFU type reference
     * @param id            the choice id
     * @return always null in this overwrite
     */
    @Overwrite
    @Nullable
    public static Type<?> getChoiceType(DSL.TypeReference typeReference, String id) {
        return null;
    }

    /**
     * @author magistermaks
     * @reason DataBreaker 2: Electric Boogaloo
     */
    @Overwrite
    @Nullable
    private static Type<?> getChoiceTypeInternal(DSL.TypeReference typeReference, String id) {
        return null;
    }

}
