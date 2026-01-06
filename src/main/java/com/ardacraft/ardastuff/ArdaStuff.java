package com.ardacraft.ardastuff;

import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.fabric.player.FabricPlayer;
import com.plotsquared.fabric.util.FabricUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.projectile.ProjectileHitEvent;
import xyz.nucleoid.stimuli.event.world.*;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Main entry point for the ArdaStuff Fabric mod.
 * <p>
 * Responsibilities:
 * - Registers Stimuli world/entity event listeners to hard-disable a number of grief-prone mechanics (fire tick, TNT ignite, ice melt, wither summon, snow fall, projectile interactions with frames/paintings when bypass is enabled, etc.).
 * - Registers commands via {@link ArdaStuffCommandHandler} on dedicated servers.
 * - Provides simple permission integration via LuckPerms to gate interactions and item/block usage.
 * - Maintains shared state (e.g., water spread toggles and painting breakers list).
 * <p>
 * Permissions used (LuckPerms):
 * - metatweaks.candie: Players with this can take damage; otherwise damage is denied for players.
 * - metatweaks.create: Allows basic interaction/breaking of Create mod blocks.
 * - metatweaks.createAll: Allows all Create mod blocks; otherwise restricted to a whitelist.
 * - metatweaks.protection: General protection bypass for interactions/placements.
 */
public class ArdaStuff implements ModInitializer {

    public static ArrayList<ServerPlayerEntity> paintingBreakers;
    public static boolean disableWaterSpread = true;
    public static HashSet<ServerPlayerEntity> waterSpreaders;
    public static boolean eventBypass = false;

    public ArrayList<Identifier> allowedCreateBlocks;

    /**
     * Fabric mod initialization hook. Sets up static state, allowed Create block whitelist,
     * registers Stimuli listeners, command handlers, and protection logic.
     */
    @Override
    public void onInitialize() {
        waterSpreaders = new HashSet<>();
        paintingBreakers = new ArrayList<>();

        //initialize create block whitelist
        allowedCreateBlocks = new ArrayList<>();
        allowedCreateBlocks.add(new Identifier("create:warped_window_pane"));
        allowedCreateBlocks.add(new Identifier("create:crimson_window_pane"));
        allowedCreateBlocks.add(new Identifier("create:brown_valve_handle"));
        allowedCreateBlocks.add(new Identifier("create:turntable"));
        allowedCreateBlocks.add(new Identifier("create:black_seat"));
        allowedCreateBlocks.add(new Identifier("create:white_seat"));
        allowedCreateBlocks.add(new Identifier("create:rose_quartz_tiles"));
        allowedCreateBlocks.add(new Identifier("create:brass_block"));
        allowedCreateBlocks.add(new Identifier("create:small_rose_quartz_tiles"));
        allowedCreateBlocks.add(new Identifier("create:chute"));
        allowedCreateBlocks.add(new Identifier("create:framed_glass_trapdoor"));
        allowedCreateBlocks.add(new Identifier("create:schematic_table"));
        allowedCreateBlocks.add(new Identifier("create:train_door"));
        allowedCreateBlocks.add(new Identifier("create:yellow_valve_handle"));
        allowedCreateBlocks.add(new Identifier("create:red_valve_handle"));
        allowedCreateBlocks.add(new Identifier("create:gray_valve_handle"));
        allowedCreateBlocks.add(new Identifier("create:schematicannon"));


        /*
         * Projectile entity hit handling.
         * - Default: deny projectile collisions with entities (returns FAIL) to prevent grief (e.g., arrows breaking frames).
         * - When eventBypass is true: allow projectile hits in general (SUCCESS) but still deny impacts on paintings and item frames.
         */
        Stimuli.global().listen(ProjectileHitEvent.ENTITY, (projectileEntity, hitResult) -> {
            if (eventBypass) {
                if (hitResult.getEntity().getType().getLootTableId().equals(new Identifier("minecraft:entities/painting"))) {
                    return ActionResult.FAIL;
                }
                if (hitResult.getEntity().getType().getLootTableId().equals(new Identifier("conquest:entities/painting"))) {
                    return ActionResult.FAIL;
                }
                if (hitResult.getEntity().getType().getLootTableId().equals(new Identifier("minecraft:entities/item_frame"))) {
                    return ActionResult.FAIL;
                }

                return ActionResult.SUCCESS;
            }
            return ActionResult.FAIL;
        });


        /*
         * Disable fire tick updates globally (no natural fire spread or block ignition updates).
         */
        Stimuli.global().listen(FireTickEvent.EVENT, (world, pos) -> {
            return ActionResult.FAIL;
        });

        /*
         * Prevent ice from melting into water.
         */
        Stimuli.global().listen(IceMeltEvent.EVENT, (world, pos) -> {
            return ActionResult.FAIL;
        });

        /*
         * Block Wither summoning sequences from completing.
         */
        Stimuli.global().listen(WitherSummonEvent.EVENT, (world, pos) -> {
            return ActionResult.FAIL;
        });

        /*
         * Prevent new snow layers from forming due to snowfall.
         */
        Stimuli.global().listen(SnowFallEvent.EVENT, (world, pos) -> {
            return ActionResult.FAIL;
        });

        /*
         * Prevent TNT from being ignited by any source.
         */
        Stimuli.global().listen(TntIgniteEvent.EVENT, (world, pos, entity) -> {
            return ActionResult.FAIL;
        });


        /*
         * Explosion detonation listener: intentionally left as a no-op placeholder for future logic.
         */
        Stimuli.global().listen(ExplosionDetonatedEvent.EVENT, (explosion, particles) -> {
            return;
        });

        /*
         * Register /ardastuff commands on dedicated servers only.
         */
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            if (environment.dedicated) {
                ArdaStuffCommandHandler.ArdaStuffCommands(dispatcher, registryAccess, environment);
            }
        });

        /*
         * Damage permission gate for players.
         * - Players require `metatweaks.candie` to receive damage (returns true to allow damage).
         * - Non-player entities are unaffected (damage allowed).
         */
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (hasPermission(player, "metatweaks.candie")) {
                    return true;
                }
                return false;
            }
            return true;
        });

        /*
         * Block break: Create mod block whitelist + permissions.
         * - If the block is from the Create namespace, player must have metatweaks.create.
         * - If metatweaks.createAll is absent, only blocks on allowedCreateBlocks are permitted.
         */
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (Registries.BLOCK.getId(state.getBlock()).getNamespace().equalsIgnoreCase("create")) {
                    if (!hasPermission(serverPlayer, "metatweaks.create")) {
                        return false;
                    } else if (!hasPermission(serverPlayer, "metatweaks.createAll")) {
                        if (!allowedCreateBlocks.contains(Registries.BLOCK.getId(state.getBlock()))) {
                            return false;
                        } else {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
            return true;
        });

        /*
         * Block break: general protection gate. Requires metatweaks.protection to break any block.
         */
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!hasPermission(serverPlayer, "metatweaks.protection")) {
                    return false;
                }
            }
            return true;
        });

        /*
         * Use block callback (right-click on blocks):
         * - Proactively denies usage of certain grief-prone items (spawn eggs, buckets, boats, tridents, F&S, etc.) and logs usage attempts.
         * - Requires metatweaks.protection to proceed with most interactions.
         * - Extra Create item gating: requires metatweaks.create and optionally metatweaks.createAll or whitelist match.
         * - Finally, applies isBlockProtectedAgainstUseAction (doors/gates allowed with empty hand; otherwise permission required).
         */
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {


            if (player.getMainHandStack().getItem() instanceof SpawnEggItem) {
                Log.info(LogCategory.LOG, "Spawn egg used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()) + " by " + player.getName().getString());
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof MinecartItem) {
                Log.info(LogCategory.LOG, "Minecart used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()) + " by " + player.getName().getString());
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof BucketItem) {
                Log.info(LogCategory.LOG, "Bucket used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()) + " by " + player.getName().getString());
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof FlintAndSteelItem) {
                Log.info(LogCategory.LOG, "Flint and Steel used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()) + " by " + player.getName().getString());
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof TridentItem) {
                Log.info(LogCategory.LOG, "Trident used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()) + " by " + player.getName().getString());
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof BoatItem) {
                Log.info(LogCategory.LOG, "Boat used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()) + " by " + player.getName().getString());
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof EggItem) {
                Log.info(LogCategory.LOG, "Egg used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()) + " by " + player.getName().getString());
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof MilkBucketItem) {
                Log.info(LogCategory.LOG, "Milk Bucket used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()) + " by " + player.getName().getString());
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof ThrowablePotionItem) {
                Log.info(LogCategory.LOG, "Throwable Potion used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()) + " by " + player.getName().getString());
                return ActionResult.FAIL;
            }

            if (Registries.ITEM.getId(player.getStackInHand(hand).getItem()).toString().startsWith("create:")) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    try {
                        if (hasPermission(serverPlayer, "metatweaks.protection")) {
                            if (!hasPermission(serverPlayer, "metatweaks.create")) {
                                return ActionResult.FAIL;
                            } else if (!hasPermission(serverPlayer, "metatweaks.createAll")) {
                                if (!allowedCreateBlocks.contains(Registries.ITEM.getId(serverPlayer.getStackInHand(hand).getItem()))) {
                                    return ActionResult.FAIL;
                                }
                            }
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (player instanceof ServerPlayerEntity serverPlayer) {

                Location location = FabricUtil.adapt(
                        GlobalPos.create(serverPlayer.getServerWorld().getRegistryKey(),
                                hitResult.getBlockPos()));
                PlotArea area = location.getPlotArea();
                if (area == null) {
                    // No plot system here → guests cannot break
                    return ActionResult.FAIL;
                }

                FabricPlayer fabricPlayer = FabricUtil.adapt(serverPlayer);
                Plot plot = area.getPlot(location);
                if (plot == null) {
                    // Not inside a plot → guests cannot break
                    return ActionResult.FAIL;
                }

                if (area.notifyIfOutsideBuildArea(fabricPlayer, location.getY())) {
                    fabricPlayer.sendMessage(
                            TranslatableCaption.of("height.height_limit"),
                            TagResolver.builder()
                                    .tag("minheight", Tag.inserting(Component.text(area.getMinBuildHeight())))
                                    .tag("maxheight", Tag.inserting(Component.text(area.getMaxBuildHeight())))
                                    .build()
                    );
                    return ActionResult.FAIL;
                }

                if (!plot.hasOwner()) {
                    // Unowned plot requires admin permission
                    if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                        fabricPlayer.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver("node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED))
                        );
                        return ActionResult.FAIL;
                    }
                } else {
                    // Must be owner OR added
                    boolean isOwner = plot.getOwner() != null && plot.getOwner().equals(fabricPlayer.getUUID());
                    boolean isAdded = plot.isAdded(fabricPlayer.getUUID());
                    if (!(isOwner || isAdded)) {
                        if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                            fabricPlayer.sendMessage(
                                    TranslatableCaption.of("permission.no_permission_event"),
                                    TagResolver.resolver("node",
                                            Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER))
                            );
                            return ActionResult.FAIL;
                        }
                    }
                }

                if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                    if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                        fabricPlayer.sendMessage(TranslatableCaption.of("done.building_restricted"));
                        return ActionResult.FAIL;
                    }
                }

                // All guest conditions satisfied
                return ActionResult.PASS;
            }

            return isBlockProtectedAgainstUseAction(player, world, hand, hitResult) ? ActionResult.FAIL : ActionResult.PASS;
        });

        /*
         * Use entity callback (right-click on entities):
         * - Denies use of grief-prone items and logs attempts, similar to block use.
         * - Requires metatweaks.protection; adds Create item gating logic when interacting via Create items.
         * - If permitted, returns PASS to allow vanilla/other handlers to proceed; otherwise FAIL to block.
         */
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {


            if (player.getMainHandStack().getItem() instanceof SpawnEggItem) {
                Log.info(LogCategory.LOG, "Spawn egg used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof MinecartItem) {
                Log.info(LogCategory.LOG, "Minecart used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof BucketItem) {
                Log.info(LogCategory.LOG, "Bucket used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof FlintAndSteelItem) {
                Log.info(LogCategory.LOG, "Flint and Steel used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof TridentItem) {
                Log.info(LogCategory.LOG, "Trident used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof BoatItem) {
                Log.info(LogCategory.LOG, "Boat used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof EggItem) {
                Log.info(LogCategory.LOG, "Egg used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof MilkBucketItem) {
                Log.info(LogCategory.LOG, "Milk Bucket used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return ActionResult.FAIL;
            }

            if (player.getMainHandStack().getItem() instanceof ThrowablePotionItem) {
                Log.info(LogCategory.LOG, "Throwable Potion used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return ActionResult.FAIL;
            }
            if (!hasPermission(player, "metatweaks.protection")) {
                return ActionResult.FAIL;
            }

            if (Registries.ITEM.getId(player.getStackInHand(hand).getItem()).toString().startsWith("create:")) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    try {
                        if (hasPermission(serverPlayer, "metatweaks.protection")) {
                            if (!hasPermission(serverPlayer, "metatweaks.create")) {
                                return ActionResult.FAIL;
                            } else if (!hasPermission(serverPlayer, "metatweaks.createAll")) {
                                if (!allowedCreateBlocks.contains(Registries.ITEM.getId(serverPlayer.getStackInHand(hand).getItem()))) {
                                    return ActionResult.FAIL;
                                } else {
                                    return ActionResult.PASS;
                                }
                            } else {
                                return ActionResult.PASS;
                            }
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (player instanceof ServerPlayerEntity serverPlayer) {
                try {
                    if (hasPermission(serverPlayer, "metatweaks.protection")) {
                        return ActionResult.PASS;
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
            return ActionResult.FAIL;
        });

        /*
         * Use item callback (right-click in air):
         * - Allows Patchouli guide books to be opened normally.
         * - Denies various grief-prone items (spawn eggs, buckets, boats, tridents, F&S, throwable potions, etc.).
         * - If metatweaks.protection is missing, generally yields PASS to let other handlers potentially ignore/handle.
         * - Create items: same permission/whitelist gating as block/entity use.
         */
        UseItemCallback.EVENT.register((player, world, hand) -> {


            if (Registries.ITEM.getId(player.getStackInHand(hand).getItem()).toString().startsWith("patchouli:guide_book")) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            if (player.getMainHandStack().getItem() instanceof SpawnEggItem) {
                Log.info(LogCategory.LOG, "Spawn egg used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return TypedActionResult.fail(ItemStack.EMPTY);
            }


            if (player.getMainHandStack().getItem() instanceof MinecartItem) {
                Log.info(LogCategory.LOG, "Minecart used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return TypedActionResult.fail(ItemStack.EMPTY);
            }

            if (player.getMainHandStack().getItem() instanceof BucketItem) {
                Log.info(LogCategory.LOG, "Bucket used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return TypedActionResult.fail(ItemStack.EMPTY);
            }


            if (player.getMainHandStack().getItem() instanceof FlintAndSteelItem) {
                Log.info(LogCategory.LOG, "Flint and Steel used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return TypedActionResult.fail(ItemStack.EMPTY);
            }

            if (player.getMainHandStack().getItem() instanceof TridentItem) {
                Log.info(LogCategory.LOG, "Trident used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return TypedActionResult.fail(ItemStack.EMPTY);
            }

            if (player.getMainHandStack().getItem() instanceof BoatItem) {
                Log.info(LogCategory.LOG, "Boat used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return TypedActionResult.fail(ItemStack.EMPTY);
            }

            if (player.getMainHandStack().getItem() instanceof EggItem) {
                Log.info(LogCategory.LOG, "Egg used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return TypedActionResult.fail(ItemStack.EMPTY);
            }

            if (player.getMainHandStack().getItem() instanceof MilkBucketItem) {
                Log.info(LogCategory.LOG, "Milk Bucket used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return TypedActionResult.fail(ItemStack.EMPTY);
            }

            if (player.getMainHandStack().getItem() instanceof ThrowablePotionItem) {
                Log.info(LogCategory.LOG, "Throwable Potion used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return TypedActionResult.fail(ItemStack.EMPTY);
            }

            if (Registries.ITEM.getId(player.getStackInHand(hand).getItem()).toString().startsWith("create:")) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    try {
                        if (hasPermission(serverPlayer, "metatweaks.protection")) {
                            if (!hasPermission(serverPlayer, "metatweaks.create")) {
                                return TypedActionResult.fail(ItemStack.EMPTY);
                            } else if (!hasPermission(serverPlayer, "metatweaks.createAll")) {
                                if (!allowedCreateBlocks.contains(Registries.ITEM.getId(serverPlayer.getStackInHand(hand).getItem()))) {
                                    return TypedActionResult.fail(ItemStack.EMPTY);
                                }
                            }
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (player instanceof ServerPlayerEntity serverPlayer) {
                Location location = FabricUtil.adapt(
                        GlobalPos.create(serverPlayer.getServerWorld().getRegistryKey(), serverPlayer.getBlockPos()));
                PlotArea area = location.getPlotArea();
                if (area == null) {
                    // No plot system here → guests cannot break
                    return TypedActionResult.fail(ItemStack.EMPTY);
                }

                FabricPlayer fabricPlayer = FabricUtil.adapt(serverPlayer);
                Plot plot = area.getPlot(location);
                if (plot == null) {
                    // Not inside a plot → guests cannot break
                    return TypedActionResult.fail(ItemStack.EMPTY);
                }

                if (area.notifyIfOutsideBuildArea(fabricPlayer, location.getY())) {
                    fabricPlayer.sendMessage(
                            TranslatableCaption.of("height.height_limit"),
                            TagResolver.builder()
                                    .tag("minheight", Tag.inserting(Component.text(area.getMinBuildHeight())))
                                    .tag("maxheight", Tag.inserting(Component.text(area.getMaxBuildHeight())))
                                    .build()
                    );
                    return TypedActionResult.fail(ItemStack.EMPTY);
                }

                if (!plot.hasOwner()) {
                    // Unowned plot requires admin permission
                    if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                        fabricPlayer.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver("node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED))
                        );
                        return TypedActionResult.fail(ItemStack.EMPTY);
                    }
                } else {
                    // Must be owner OR added
                    boolean isOwner = plot.getOwner() != null && plot.getOwner().equals(fabricPlayer.getUUID());
                    boolean isAdded = plot.isAdded(fabricPlayer.getUUID());
                    if (!(isOwner || isAdded)) {
                        if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                            fabricPlayer.sendMessage(
                                    TranslatableCaption.of("permission.no_permission_event"),
                                    TagResolver.resolver("node",
                                            Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER))
                            );
                            return TypedActionResult.fail(ItemStack.EMPTY);
                        }
                    }
                }

                if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                    if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                        fabricPlayer.sendMessage(TranslatableCaption.of("done.building_restricted"));
                        return TypedActionResult.fail(ItemStack.EMPTY);
                    }
                }

                // All guest conditions satisfied
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            return TypedActionResult.pass(ItemStack.EMPTY);
        });
        
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                // Only gate server players; let others fall through normally
                return ActionResult.PASS;
            }

            Location location = FabricUtil.adapt(
                    GlobalPos.create(serverPlayer.getServerWorld().getRegistryKey(), pos));
            PlotArea area = location.getPlotArea();
            if (area == null) {
                // No plot system here → guests cannot break
                return ActionResult.FAIL;
            }

            FabricPlayer fabricPlayer = FabricUtil.adapt(serverPlayer);
            Plot plot = area.getPlot(location);
            if (plot == null) {
                // Not inside a plot → guests cannot break
                return ActionResult.FAIL;
            }

            if (area.notifyIfOutsideBuildArea(fabricPlayer, location.getY())) {
                fabricPlayer.sendMessage(
                        TranslatableCaption.of("height.height_limit"),
                        TagResolver.builder()
                                .tag("minheight", Tag.inserting(Component.text(area.getMinBuildHeight())))
                                .tag("maxheight", Tag.inserting(Component.text(area.getMaxBuildHeight())))
                                .build()
                );
                return ActionResult.FAIL;
            }

            if (!plot.hasOwner()) {
                // Unowned plot requires admin permission
                if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                    fabricPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver("node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED))
                    );
                    return ActionResult.FAIL;
                }
            } else {
                // Must be owner OR added
                boolean isOwner = plot.getOwner() != null && plot.getOwner().equals(fabricPlayer.getUUID());
                boolean isAdded = plot.isAdded(fabricPlayer.getUUID());
                if (!(isOwner || isAdded)) {
                    if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                        fabricPlayer.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver("node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER))
                        );
                        return ActionResult.FAIL;
                    }
                    return ActionResult.PASS;
                }
                return ActionResult.PASS;
            }

            if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                    fabricPlayer.sendMessage(TranslatableCaption.of("done.building_restricted"));
                    return ActionResult.FAIL;
                }
                return ActionResult.PASS;
            }

            // All guest conditions satisfied
            return ActionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
                if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                    // Only gate server players; let others fall through normally
                    return ActionResult.PASS;
                }

                Location location = FabricUtil.adapt(
                        GlobalPos.create(serverPlayer.getServerWorld().getRegistryKey(),
                                entity.getBlockPos()));
                PlotArea area = location.getPlotArea();
                if (area == null) {
                    // No plot system here → guests cannot break
                    return ActionResult.FAIL;
                }

                FabricPlayer fabricPlayer = FabricUtil.adapt(serverPlayer);
                Plot plot = area.getPlot(location);
                if (plot == null) {
                    // Not inside a plot → guests cannot break
                    return ActionResult.FAIL;
                }

                if (area.notifyIfOutsideBuildArea(fabricPlayer, location.getY())) {
                    fabricPlayer.sendMessage(
                            TranslatableCaption.of("height.height_limit"),
                            TagResolver.builder()
                                    .tag("minheight", Tag.inserting(Component.text(area.getMinBuildHeight())))
                                    .tag("maxheight", Tag.inserting(Component.text(area.getMaxBuildHeight())))
                                    .build()
                    );
                    return ActionResult.FAIL;
                }

                if (!plot.hasOwner()) {
                    // Unowned plot requires admin permission
                    if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                        fabricPlayer.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver("node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED))
                        );
                        return ActionResult.FAIL;
                    }
                } else {
                    // Must be owner OR added
                    boolean isOwner = plot.getOwner() != null && plot.getOwner().equals(fabricPlayer.getUUID());
                    boolean isAdded = plot.isAdded(fabricPlayer.getUUID());
                    if (!(isOwner || isAdded)) {
                        if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                            fabricPlayer.sendMessage(
                                    TranslatableCaption.of("permission.no_permission_event"),
                                    TagResolver.resolver("node",
                                            Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER))
                            );
                            return ActionResult.FAIL;
                        }
                        return ActionResult.PASS;
                    }
                    return ActionResult.PASS;
                }

                if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                    if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                        fabricPlayer.sendMessage(TranslatableCaption.of("done.building_restricted"));
                        return ActionResult.FAIL;
                    }
                    return ActionResult.PASS;
                }

                // All guest conditions satisfied
                return ActionResult.PASS;
        });
    }

    /**
     * Checks if both hands are empty.
     *
     * @param player the player to check
     * @return true if both main hand and offhand are empty, false otherwise
     */
    public boolean isHandEmpty(PlayerEntity player) {
        return player.getMainHandStack().isEmpty() && player.getOffHandStack().isEmpty();
    }

    /**
     * Checks a LuckPerms permission for the given player. Only evaluated for server players.
     *
     * @param player     the player
     * @param permission the permission node to check
     * @return true if permitted; false otherwise or if not a server player/adapter unavailable
     */
    public boolean hasPermission(PlayerEntity player, String permission) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            try {
                if (LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(serverPlayer).getCachedData().getPermissionData().checkPermission(permission).asBoolean()) {
                    return true;
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * Determines whether a block use action should be treated as protected/denied for a player.
     * Allows bare-hand interaction with doors/gates; otherwise requires metatweaks.protection.
     *
     * @param player    the player
     * @param world     the world
     * @param hand      the hand used
     * @param hitResult the targeted block hit
     * @return true if use action is protected (should be denied), false otherwise
     */
    public boolean isBlockProtectedAgainstUseAction(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {

        var blockName = Registries.BLOCK.getId(world.getBlockState(hitResult.getBlockPos()).getBlock()).toString().toLowerCase();

        if (isHandEmpty(player) && (blockName.endsWith("_door") || blockName.endsWith("_gate"))) return false;
        return !hasPermission(player, "metatweaks.protection");
    }
}
