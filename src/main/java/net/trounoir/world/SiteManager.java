package net.trounoir.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.character.Characters;
import net.trounoir.config.TNConfig;
import net.trounoir.data.CharacterDef;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.TNData;
import net.trounoir.network.Net;
import net.trounoir.rebirth.PersistentStore;
import net.trounoir.util.Fx;

import java.util.List;

/**
 * Les lieux du monde : où ils se trouvent, quand ils sont bâtis et peuplés, quand le joueur les découvre.
 * Les positions sont déduites de la graine (donc identiques à chaque vie) et mises en cache hors du monde.
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SiteManager {
    private SiteManager() {}

    private static final int[][] UNIQUE_OFFSETS = {{0, 0}, {3, 0}, {-3, 0}, {3, 3}, {-3, 3}, {0, 3}};
    public static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");

    public static SimpleDefs.SiteDef site(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : TNData.SITES.get(rl);
    }

    public static ServerLevel level(MinecraftServer server, ResourceLocation dimension) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimension);
        return server.getLevel(key);
    }

    /** Point de renaissance : toujours le même, vie après vie. */
    public static BlockPos origin(MinecraftServer server) {
        PersistentStore store = PersistentStore.get(server);
        if (store.origin == null || store.origin.length != 3) {
            BlockPos spawn = server.overworld().getSharedSpawnPos();
            store.origin = new int[]{spawn.getX(), spawn.getY(), spawn.getZ()};
            store.save();
        }
        return new BlockPos(store.origin[0], store.origin[1], store.origin[2]);
    }

    /** Position d'un lieu [x, y, z], résolue à la demande ; nul si sa dimension n'existe pas. */
    public static int[] positionOf(MinecraftServer server, String siteId) {
        PersistentStore store = PersistentStore.get(server);
        int[] cached = store.sitePositions.get(siteId);
        if (cached != null && cached.length == 3) return cached;
        SimpleDefs.SiteDef def = site(siteId);
        if (def == null) return null;
        ServerLevel level = level(server, def.dimension());
        if (level == null) return null;
        int[] resolved = resolve(server, level, def);
        store.sitePositions.put(siteId, resolved);
        store.save();
        return resolved;
    }

    private static int surface(ServerLevel level, int x, int z) {
        return level.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
    }

    private static boolean suitable(ServerLevel level, int x, int z) {
        int sea = level.getSeaLevel();
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int[][] probes = {{0, 0}, {14, 0}, {-14, 0}, {0, 14}, {0, -14}};
        for (int[] p : probes) {
            int h = surface(level, x + p[0], z + p[1]);
            if (h <= sea) return false;
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        return max - min <= 9;
    }

    /** Cherche, autour de la position théorique, un terrain émergé et à peu près plat. Sans générer de tronçon. */
    private static int[] resolve(MinecraftServer server, ServerLevel level, SimpleDefs.SiteDef def) {
        BlockPos base = def.dimension().equals(OVERWORLD) ? origin(server) : BlockPos.ZERO;
        double distance = def.distance() * TNConfig.SITE_DISTANCE_SCALE.get();
        double angle = Math.toRadians(def.angle());
        int nx = base.getX() + (int) Math.round(Math.cos(angle) * distance);
        int nz = base.getZ() + (int) Math.round(Math.sin(angle) * distance);
        for (int ring = 0; ring <= 10; ring++) {
            int steps = ring == 0 ? 1 : 8;
            for (int i = 0; i < steps; i++) {
                double a = Math.PI * 2 * i / steps + ring * 0.37;
                int x = nx + (int) Math.round(Math.cos(a) * ring * 32);
                int z = nz + (int) Math.round(Math.sin(a) * ring * 32);
                if (suitable(level, x, z)) return new int[]{x, surface(level, x, z), z};
            }
        }
        // Aucun terrain convenable : le lieu sera bâti sur ses propres fondations (îlot).
        return new int[]{nx, Math.max(surface(level, nx, nz), level.getSeaLevel() + 1), nz};
    }

    /** Bâtit le lieu s'il ne l'est pas, et le peuple s'il ne l'est pas pour cette vie. */
    public static StructureBuilders.Built ensureBuilt(ServerLevel level, SimpleDefs.SiteDef def) {
        MinecraftServer server = level.getServer();
        String id = def.id().toString();
        int[] pos = positionOf(server, id);
        if (pos == null) return null;
        BlockPos center = new BlockPos(pos[0], pos[1], pos[2]);
        WorldState state = WorldState.get(server);
        StructureBuilders.Built built;
        if (state.builtSites.add(id)) {
            built = StructureBuilders.build(level, def, center);
            RelicManager.placeChest(level, def, built.chest());
            TrouNoir.LOGGER.info("[Trou-Noir] Lieu bâti : {} en {}", id, center.toShortString());
        } else {
            built = StructureBuilders.layout(level, def, center);
        }
        if (state.populatedSites.add(id)) populate(level, def, built);
        state.save();
        return built;
    }

    private static void populate(ServerLevel level, SimpleDefs.SiteDef def, StructureBuilders.Built built) {
        int next = 0;
        int uniques = 0;
        for (SimpleDefs.SquadEntry entry : def.residents()) {
            CharacterDef character = TNData.CHARACTERS.get(entry.character());
            if (character == null) {
                TrouNoir.LOGGER.warn("[Trou-Noir] Habitant inconnu {} dans {}", entry.character(), def.id());
                continue;
            }
            for (int i = 0; i < entry.count(); i++) {
                BlockPos pos;
                if (character.unique) {
                    // Les uniques se tiennent au point focal, légèrement écartés les uns des autres.
                    int[] o = UNIQUE_OFFSETS[uniques++ % UNIQUE_OFFSETS.length];
                    pos = built.focus().offset(o[0], 0, o[1]);
                } else if (!built.spots().isEmpty()) {
                    pos = built.spots().get(next++ % built.spots().size());
                } else {
                    double a = Math.PI * 2 * next++ / 6.0;
                    pos = built.focus().offset((int) Math.round(Math.cos(a) * 5), 0, (int) Math.round(Math.sin(a) * 5));
                }
                Characters.spawn(level, character, pos, pos);
            }
        }
    }

    /** Lieu le plus proche du joueur dans sa dimension, s'il est à portée ; sinon nul. */
    public static SimpleDefs.SiteDef nearest(ServerPlayer player, double maxDistance) {
        SimpleDefs.SiteDef best = null;
        double bestSq = maxDistance * maxDistance;
        ResourceLocation here = player.level().dimension().location();
        PersistentStore store = PersistentStore.get(player.server);
        for (SimpleDefs.SiteDef def : TNData.SITES.values()) {
            if (!def.dimension().equals(here)) continue;
            int[] pos = store.sitePositions.get(def.id().toString());
            if (pos == null) continue;
            double dx = pos[0] - player.getX();
            double dz = pos[2] - player.getZ();
            if (dx * dx + dz * dz < bestSq) {
                bestSq = dx * dx + dz * dz;
                best = def;
            }
        }
        return best;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        PersistentStore store = PersistentStore.get(server);

        // Résout au plus une position par seconde pour étaler le coût.
        for (SimpleDefs.SiteDef def : TNData.SITES.values()) {
            if (!store.sitePositions.containsKey(def.id().toString())) {
                positionOf(server, def.id().toString());
                break;
            }
        }

        double buildRadius = TNConfig.SITE_BUILD_RADIUS.get();
        for (ServerPlayer player : players) {
            if (!player.isAlive()) continue;
            ResourceLocation here = player.level().dimension().location();
            for (SimpleDefs.SiteDef def : TNData.SITES.values()) {
                if (!def.dimension().equals(here)) continue;
                String id = def.id().toString();
                int[] pos = store.sitePositions.get(id);
                if (pos == null) continue;
                double dx = pos[0] - player.getX();
                double dz = pos[2] - player.getZ();
                double distSq = dx * dx + dz * dz;
                if (distSq > buildRadius * buildRadius) continue;
                WorldState state = WorldState.get(server);
                if (!state.builtSites.contains(id) || !state.populatedSites.contains(id)) {
                    ensureBuilt(player.serverLevel(), def);
                }
                if (distSq < 64 * 64) discover(player, def);
            }
        }
    }

    private static void discover(ServerPlayer player, SimpleDefs.SiteDef def) {
        PersistentStore store = PersistentStore.get(player.server);
        PersistentStore.Memory memory = store.memory(player.getUUID());
        String id = def.id().toString();
        LifeData life = LifeManager.getOrNull(player);
        boolean firstEver = memory.places.add(id);
        if (firstEver) {
            store.save();
            // Notification brève : le nom du lieu, rien de plus.
            Fx.title(player, Component.translatable(def.nameKey()), Component.translatable("trounoir.site.discovered"), 50);
            Fx.playTo(player, "block.amethyst_block.chime", 1.0F, 0.7F);
            Net.syncLife(player);
        }
        if (life != null) life.feat("feat.trounoir.visited", "@" + def.nameKey());
    }
}
