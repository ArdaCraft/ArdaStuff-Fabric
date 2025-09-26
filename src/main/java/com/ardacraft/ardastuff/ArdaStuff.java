package com.ardacraft.ardastuff;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.Texts;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.projectile.ProjectileHitEvent;
import xyz.nucleoid.stimuli.event.world.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Main entrypoint for the ArdaStuff Fabric mod.
 * <p>
 * Responsibilities:
 * - Registers server lifecycle, tick, and player/world events.
 * - Integrates with Stimuli for world/entity protection rules.
 * - Schedules periodic world saves and handles graceful shutdown.
 * - Provides helper methods for permission checks and protection logic.
 * </p>
 */
public class ArdaStuff implements ModInitializer {
    public static boolean disableWaterSpread = true;
    public static HashSet<ServerPlayerEntity> waterSpreaders;
    public ArrayList<Identifier> allowedCreateBlocks;
    public static boolean eventBypass = false;

    /**
     * Initializes the mod: registers events, schedules auto-saving, and sets up protection rules.
     * Invoked once when the server starts and Fabric initializes this mod.
     */
    @Override
    public void onInitialize() {
        waterSpreaders = new HashSet<>();

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

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Create a scheduled executor service with a daemon thread
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "AutoSave-Thread");
                thread.setDaemon(true);  // This is crucial for auto-cleanup on crash
                return thread;
            });

            // Schedule the save-all command to run every 5 minutes
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (server.isRunning()) {
                        // Schedule the command to be executed on the main server thread
                        server.execute(() -> {
                            server.getCommandManager().executeWithPrefix(server.getCommandSource(), "save-all");
                            Logger.getLogger("ARDASTUFF").info("World automatically saved");
                        });
                    }
                } catch (Exception e) {
                    Logger.getLogger("ARDASTUFF").warning("Error during automatic world save: " + e.getMessage());
                }
            }, 30, 40, TimeUnit.MINUTES);

            // For normal shutdown scenarios
            ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> {
                if (stoppingServer == server) {
                    scheduler.shutdown();
                    try {
                        if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                            scheduler.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        scheduler.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                }
            });

            Logger.getLogger("ARDASTUFF").info("Automatic world saving enabled (every 5 minutes)");
        });


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

                return ActionResult.PASS;
            }
            return ActionResult.FAIL;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getBlockPos().getY() < -64 && player.getServerWorld().equals(player.getServer().getOverworld())) {
                    server.getCommandManager().executeWithPrefix(player.getCommandSource(), "/warp spawn");
                }
            }
        });

        Stimuli.global().listen(FireTickEvent.EVENT, (world, pos) -> {
            return ActionResult.FAIL;
        });

        Stimuli.global().listen(IceMeltEvent.EVENT, (world, pos) -> {
            return ActionResult.FAIL;
        });

        Stimuli.global().listen(WitherSummonEvent.EVENT, (world, pos) -> {
            return ActionResult.FAIL;
        });

        Stimuli.global().listen(SnowFallEvent.EVENT, (world, pos) -> {
            return ActionResult.FAIL;
        });

        Stimuli.global().listen(TntIgniteEvent.EVENT, (world, pos, entity) -> {
            return ActionResult.FAIL;
        });


        Stimuli.global().listen(ExplosionDetonatedEvent.EVENT, (explosion, particles) -> {
            return;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            if (environment.dedicated) {
                ArdaStuffCommandHandler.ArdaStuffCommands(dispatcher, registryAccess, environment);
            }
        });


        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(player).getCachedData().getPermissionData().checkPermission("metatweaks.candie").asBoolean()) {
                    return true;
                }
                return false;
            }
            return true;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(serverPlayer).getCachedData().getPermissionData().checkPermission("metatweaks.create").asBoolean() && Registries.BLOCK.getId(state.getBlock()).getNamespace().equalsIgnoreCase("create")) {
                    return false;
                }
            }
            return true;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(serverPlayer).getCachedData().getPermissionData().checkPermission("metatweaks.protection").asBoolean()) {
                    return false;
                }
            }
            return true;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player.getMainHandStack().getItem() instanceof net.minecraft.item.SpawnEggItem) {
                Log.info(LogCategory.LOG, "Spawn egg used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return ActionResult.FAIL;
            }

            if (!hasPermission(player, "metatweaks.protection")) {
                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.MinecartItem) {
                    Log.info(LogCategory.LOG, "Minecart used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.BucketItem) {
                    Log.info(LogCategory.LOG, "Bucket used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.FlintAndSteelItem) {
                    Log.info(LogCategory.LOG, "Flint and Steel used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.TridentItem) {
                    Log.info(LogCategory.LOG, "Trident used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.BoatItem) {
                    Log.info(LogCategory.LOG, "Boat used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.EggItem) {
                    Log.info(LogCategory.LOG, "Egg used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.MilkBucketItem) {
                    Log.info(LogCategory.LOG, "Milk Bucket used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.ThrowablePotionItem) {
                    Log.info(LogCategory.LOG, "Throwable Potion used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

            }

            if (Registries.ITEM.getId(player.getStackInHand(hand).getItem()).toString().startsWith("create:")) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    try {
                        if (!LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(serverPlayer).getCachedData().getPermissionData().checkPermission("metatweaks.create").asBoolean()) {
                            return ActionResult.FAIL;
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }

            return isBlockProtectedAgainstUseAction(player, world, hand, hitResult) ? ActionResult.FAIL : ActionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player.getMainHandStack().getItem() instanceof net.minecraft.item.SpawnEggItem) {
                Log.info(LogCategory.LOG, "Spawn egg used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return ActionResult.FAIL;
            }

            if (!hasPermission(player, "metatweaks.protection")) {

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.MinecartItem) {
                    Log.info(LogCategory.LOG, "Minecart used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.BucketItem) {
                    Log.info(LogCategory.LOG, "Bucket used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.FlintAndSteelItem) {
                    Log.info(LogCategory.LOG, "Flint and Steel used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.TridentItem) {
                    Log.info(LogCategory.LOG, "Trident used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.BoatItem) {
                    Log.info(LogCategory.LOG, "Boat used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.EggItem) {
                    Log.info(LogCategory.LOG, "Egg used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.MilkBucketItem) {
                    Log.info(LogCategory.LOG, "Milk Bucket used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.ThrowablePotionItem) {
                    Log.info(LogCategory.LOG, "Throwable Potion used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return ActionResult.FAIL;
                }
            }

            if (player instanceof ServerPlayerEntity serverPlayer) {
                try {
                    if (LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(serverPlayer).getCachedData().getPermissionData().checkPermission("metatweaks.protection").asBoolean()) {
                        /*TODO ADD PLOT RESTRICTIONS HERE*/
                        return ActionResult.PASS;
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
            return ActionResult.FAIL;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (Registries.ITEM.getId(player.getStackInHand(hand).getItem()).toString().startsWith("patchouli:guide_book")) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            if (Registries.ITEM.getId(player.getStackInHand(hand).getItem()).toString().startsWith("ardapaths:path_revealer")) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            if (player.getMainHandStack().getItem() instanceof net.minecraft.item.SpawnEggItem) {
                Log.info(LogCategory.LOG, "Spawn egg used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                return TypedActionResult.fail(ItemStack.EMPTY);
            }

            if (!hasPermission(player, "metatweaks.protection")) {

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.MinecartItem) {
                    Log.info(LogCategory.LOG, "Minecart used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return TypedActionResult.fail(ItemStack.EMPTY);
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.BucketItem) {
                    Log.info(LogCategory.LOG, "Bucket used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return TypedActionResult.fail(ItemStack.EMPTY);
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.FlintAndSteelItem) {
                    Log.info(LogCategory.LOG, "Flint and Steel used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return TypedActionResult.fail(ItemStack.EMPTY);
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.TridentItem) {
                    Log.info(LogCategory.LOG, "Trident used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return TypedActionResult.fail(ItemStack.EMPTY);
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.BoatItem) {
                    Log.info(LogCategory.LOG, "Boat used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return TypedActionResult.fail(ItemStack.EMPTY);
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.EggItem) {
                    Log.info(LogCategory.LOG, "Egg used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return TypedActionResult.fail(ItemStack.EMPTY);
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.MilkBucketItem) {
                    Log.info(LogCategory.LOG, "Milk Bucket used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return TypedActionResult.fail(ItemStack.EMPTY);
                }

                if (player.getMainHandStack().getItem() instanceof net.minecraft.item.ThrowablePotionItem) {
                    Log.info(LogCategory.LOG, "Throwable Potion used " + Registries.ITEM.getId(player.getStackInHand(hand).getItem()));
                    return TypedActionResult.fail(ItemStack.EMPTY);
                }

                return TypedActionResult.fail(ItemStack.EMPTY);

            }

            if (Registries.ITEM.getId(player.getStackInHand(hand).getItem()).toString().startsWith("create:")) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    try {
                        if (!LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(serverPlayer).getCachedData().getPermissionData().checkPermission("metatweaks.create").asBoolean()) {
                            return TypedActionResult.fail(ItemStack.EMPTY);
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (player instanceof ServerPlayerEntity serverPlayer) {
                try {
                    if (LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(serverPlayer).getCachedData().getPermissionData().checkPermission("metatweaks.protection").asBoolean()) {
                        if (LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(serverPlayer).getPrimaryGroup().equalsIgnoreCase("default")) {
                            /* TODO ADD PLOT RESTRICTIONS HERE */
                                return TypedActionResult.fail(player.getMainHandStack());
                        }
                        return TypedActionResult.pass(player.getMainHandStack());
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }


            return TypedActionResult.fail(ItemStack.EMPTY);
        });


        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(serverPlayer).getCachedData().getPermissionData().checkPermission("metatweaks.guestPaintingBreaking").asBoolean()) {
                  /* TODO ADD PLOT RESTRICTIONS HERE */
                }

                if (LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(serverPlayer).getCachedData().getPermissionData().checkPermission("metatweaks.paintingBreaking").asBoolean()) {
                    return ActionResult.PASS;
                }
            }
            return ActionResult.FAIL;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                try {
                    if (!LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(serverPlayer).getCachedData().getPermissionData().checkPermission("metatweaks.protection").asBoolean()) {
                        return ActionResult.FAIL;
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
            return ActionResult.PASS;
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ServerPlayerEntity player) {
                try {
                    if (!LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(player).getCachedData().getPermissionData().checkPermission("metatweaks.hasJoined").asBoolean()) {
                        world.getServer().getPlayerManager().broadcast(Texts.setStyleIfAbsent(Text.literal("Welcome to ArdaCraft, " + player.getDisplayName().getString() + "! Please check out your guide book!"), Style.EMPTY.withColor(TextColor.parse("#416cba"))), false);
                        ItemStack guideBook = Registries.ITEM.get(new Identifier("patchouli", "guide_book")).getDefaultStack();
                        ItemStack pathfinder = Registries.ITEM.get(new Identifier("ardapaths", "path_revealer")).getDefaultStack();

                        guideBook.getOrCreateNbt().putString("patchouli:book", "patchouli:ac_guide");
                        player.giveItemStack(pathfinder);
                        player.giveItemStack(guideBook);


                        LuckPermsProvider.get().getUserManager().modifyUser(player.getUuid(), user -> user.data().add(Node.builder("metatweaks.hasJoined").build()));
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
/*
                try {
                    String group = LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(player).getPrimaryGroup().toLowerCase();

                    var teamName = switch (group) {
                        case "admin" -> "Admin";
                        case "steward" -> "Steward";
                        case "developer" -> "Developer";
                        case "overseer" -> "Overseer";
                        case "landscaperplus" -> "Landscaper+";
                        case "landscaper" -> "Landscaper";
                        case "builderplus" -> "Builder+";
                        case "builder" -> "Builder";
                        case "community_manager" -> "Community_Manager";
                        case "apprentice" -> "Apprentice";
                        case "media_manager" -> "Media_Manager";
                        case "patron" -> "Patron";
                        default -> "Guest";
                    };

                    var team = player.getScoreboard().getTeam(teamName);

                    if (team != null) {
                        player.getScoreboard().addPlayerToTeam(player.getEntityName(), team);
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }*/
            }
        });
    }

    /**
     * Checks if the given player's hands (main-hand and off-hand) are both empty.
     *
     * @param player the player to check
     * @return true if both hands are empty; false otherwise
     */
    public boolean isHandEmpty(PlayerEntity player) {
        return player.getMainHandStack().isEmpty() && player.getOffHandStack().isEmpty();
    }

    /**
     * Checks whether a player has a specific LuckPerms permission node.
     * Only works reliably for server-side players; client-only PlayerEntity instances will return false.
     *
     * @param player     the player to check (expected to be a ServerPlayerEntity)
     * @param permission the permission node to check, e.g. "metatweaks.protection"
     * @return true if the permission is granted; false otherwise
     */
    public static boolean hasPermission(PlayerEntity player, String permission) {
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
     * Determines whether a block interaction should be blocked by protection logic.
     * Allows door/gate use with empty hands, but otherwise requires a specific permission.
     *
     * @param player    the player attempting to use a block
     * @param world     the world containing the block
     * @param hand      the hand used for the interaction
     * @param hitResult the hit result providing the target block position
     * @return true if the block is protected against the use action; false if allowed
     */
    public boolean isBlockProtectedAgainstUseAction(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {

        var blockName = Registries.BLOCK.getId(world.getBlockState(hitResult.getBlockPos()).getBlock()).toString().toLowerCase();

        if (isHandEmpty(player) && (blockName.endsWith("_door") || blockName.endsWith("_gate"))) return false;
        return !hasPermission(player, "metatweaks.protection");
    }

}
