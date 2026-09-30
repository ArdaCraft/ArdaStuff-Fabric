package com.ardacraft.ardastuff.ardamaps;

import net.fabricmc.loader.api.FabricLoader;
import net.william278.huskhomes.api.FabricHuskHomesAPI;

import java.util.List;
import java.util.concurrent.CompletableFuture;

final class HuskHomesWarpResolver {

    private HuskHomesWarpResolver() {}

    static boolean isAvailable() {

        return FabricLoader.getInstance().isModLoaded("huskhomes");
    }

    static CompletableFuture<List<ResolvedWarp>> warps() {

        return FabricHuskHomesAPI.getInstance()
                .getWarps()
                .thenApply(warps -> warps.stream()
                        .map(warp -> new ResolvedWarp(
                                warp.getName(),
                                warp.getWorld().getName(),
                                warp.getX(),
                                warp.getY(),
                                warp.getZ()))
                        .toList());
    }

    record ResolvedWarp(String name, String world, double x, double y, double z) {}
}
