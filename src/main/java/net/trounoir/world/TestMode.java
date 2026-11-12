package net.trounoir.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.Absorption;
import net.trounoir.absorption.Bonuses;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.TNData;
import net.trounoir.network.Net;
import net.trounoir.rebirth.PersistentStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Mode test : une règle du monde (choisie à la création, onglet « Règles du jeu »).
 * Tous les lieux sont connus, la touche N y téléporte, on choisit un jeu de pouvoirs par rang,
 * et la mort ne remonte jamais le temps du monde entier (renaissance douce).
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TestMode {
    private TestMode() {}

    public static GameRules.Key<GameRules.BooleanValue> RULE;

    public static void init() {
        RULE = GameRules.register("trounoirTestMode", GameRules.Category.MISC, GameRules.BooleanValue.create(false));
    }

    public static boolean active(MinecraftServer server) {
        return server != null && RULE != null && server.getGameRules().getBoolean(RULE);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && active(player.server)) revealAll(player);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 100 != 0 || !active(server)) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) revealAll(player);
    }

    /** Tous les lieux du mod deviennent connus (et donc choisissables sur la boussole). */
    private static void revealAll(ServerPlayer player) {
        PersistentStore store = PersistentStore.get(player.server);
        PersistentStore.Memory memory = store.memory(player.getUUID());
        boolean changed = false;
        for (SimpleDefs.SiteDef def : TNData.SITES.values()) changed |= memory.places.add(def.id().toString());
        if (changed) {
            store.save();
            Net.syncLife(player);
        }
    }

    public static void teleport(ServerPlayer player, String siteId) {
        if (!active(player.server)) return;
        SimpleDefs.SiteDef def = SiteManager.site(siteId);
        ServerLevel level = def == null ? null : SiteManager.level(player.server, def.dimension());
        if (level == null) return;
        StructureBuilders.Built built = SiteManager.ensureBuilt(level, def);
        if (built == null) return;
        BlockPos at = built.arrival();
        player.teleportTo(level, at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, player.getYRot(), 0F);
    }

    /**
     * Remplace les pouvoirs de la vie en cours par un jeu adapté au rang demandé :
     * quatre techniques équipées, puis des traits jusqu'à atteindre ce rang. Rang 0 : plus rien.
     */
    public static void kit(ServerPlayer player, int rank) {
        if (!active(player.server)) return;
        LifeData life = LifeManager.get(player);
        life.xp.clear();
        java.util.Arrays.fill(life.slots, null);
        life.cooldowns.clear();
        Bonuses.recompute(player);
        if (rank > 0) {
            List<AbsorbableDef> techniques = new ArrayList<>();
            List<AbsorbableDef> traits = new ArrayList<>();
            for (AbsorbableDef def : TNData.ABSORBABLES.values()) {
                if (def.kind == AbsorbableDef.Kind.TECHNIQUE) techniques.add(def);
                else if (def.kind == AbsorbableDef.Kind.TRAIT) traits.add(def);
            }
            Random random = new Random(rank * 7919L + player.getRandom().nextInt(1000));
            Collections.shuffle(techniques, random);
            Collections.shuffle(traits, random);
            // Rangs bas : les techniques modestes ; rangs hauts : les plus fortes.
            techniques.sort((a, b) -> Double.compare(Math.abs(a.power - rank * 4.0), Math.abs(b.power - rank * 4.0)));
            for (int i = 0; i < Math.min(4, techniques.size()); i++) {
                AbsorbableDef def = techniques.get(i);
                int target = Math.min(def.maxLevel, 1 + rank / 3);
                for (int n = 0; n < AbsorbableDef.xpForLevel(target); n++) Absorption.absorb(player, def.id.toString(), false);
            }
            Bonuses.recompute(player);
            for (AbsorbableDef def : traits) {
                if (life.rank >= rank) break;
                for (int n = 0; n < AbsorbableDef.xpForLevel(def.maxLevel) && life.rank < rank; n++) {
                    Absorption.absorb(player, def.id.toString(), false);
                    Bonuses.recompute(player);
                }
            }
        }
        life.dirty = true;
        Bonuses.recompute(player);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        Net.syncLife(player);
        player.displayClientMessage(Component.translatable("trounoir.test.kit", net.trounoir.util.Text.roman(life.rank)), true);
    }
}
