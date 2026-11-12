package net.trounoir.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.TNData;
import net.trounoir.item.ArtifactItem;
import net.trounoir.network.Net;
import net.trounoir.rebirth.PersistentStore;
import net.trounoir.registry.ModRegistry;
import net.trounoir.util.Fx;
import net.trounoir.util.Lookup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Les reliques des Dieux. Chacune repose dans un coffre, dans un lieu du mod : jamais deux au même endroit,
 * jamais deux fois la même. La répartition dépend de la graine : elle est donc identique à chaque vie,
 * et ce que le joueur a trouvé une fois reste un souvenir (onglet Savoirs).
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RelicManager {
    private RelicManager() {}

    private static Map<String, String> assignment;   // lieu -> relique
    private static long assignedSeed;

    /** Relique cachée dans ce lieu, ou nul. */
    public static synchronized String relicOf(MinecraftServer server, String siteId) {
        long seed = server.getWorldData().worldGenOptions().seed();
        if (assignment == null || assignedSeed != seed) {
            List<String> sites = new ArrayList<>();
            for (SimpleDefs.SiteDef def : TNData.SITES.values()) if (def.relicSpot()) sites.add(def.id().toString());
            Collections.sort(sites);
            Collections.shuffle(sites, new Random(seed ^ 0x7E11C5L));
            List<String> relics = new ArrayList<>();
            for (ArtifactItem.Spec spec : ModRegistry.ARTIFACTS) if (spec.relic()) relics.add(spec.id());
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < Math.min(sites.size(), relics.size()); i++) map.put(sites.get(i), relics.get(i));
            assignment = map;
            assignedSeed = seed;
        }
        return assignment.get(siteId);
    }

    /** Pose le coffre de la relique quand le lieu vient d'être bâti. */
    public static void placeChest(ServerLevel level, SimpleDefs.SiteDef site, BlockPos pos) {
        String relic = relicOf(level.getServer(), site.id().toString());
        if (relic == null || pos == null) return;
        level.setBlock(pos, Lookup.block(ResourceLocation.withDefaultNamespace("chest")), 3);
        if (level.getBlockEntity(pos) instanceof Container chest) {
            chest.setItem(13, new ItemStack(ModRegistry.ARTIFACT_ITEMS.get(relic).get()));
        }
        WorldState state = WorldState.get(level.getServer());
        state.relicChests.put(relic, new int[]{pos.getX(), pos.getY(), pos.getZ()});
        state.relicSites.put(relic, site.id().toString());
    }

    /** Ouvrir le coffre d'une relique en fait un souvenir : le lieu est retenu pour les vies suivantes. */
    @SubscribeEvent
    public static void onOpen(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WorldState state = WorldState.get(player.server);
        BlockPos pos = event.getPos();
        for (Map.Entry<String, int[]> e : state.relicChests.entrySet()) {
            int[] p = e.getValue();
            if (p[0] != pos.getX() || p[1] != pos.getY() || p[2] != pos.getZ()) continue;
            String site = state.relicSites.get(e.getKey());
            if (site == null || SiteManager.site(site) == null || !SiteManager.site(site).dimension().equals(player.level().dimension().location())) continue;
            PersistentStore store = PersistentStore.get(player.server);
            PersistentStore.Memory memory = store.memory(player.getUUID());
            if (memory.relics == null) memory.relics = new HashMap<>();
            if (!site.equals(memory.relics.put(e.getKey(), site))) {
                store.save();
                LifeData life = LifeManager.getOrNull(player);
                if (life != null) life.feat("feat.trounoir.relic", "@item.trounoir." + e.getKey());
                player.displayClientMessage(Component.translatable("trounoir.relic.found"), true);
                Fx.playTo(player, "block.amethyst_block.chime", 1.0F, 0.5F);
                Net.syncLife(player);
            }
        }
    }
}
