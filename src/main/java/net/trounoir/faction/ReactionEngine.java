package net.trounoir.faction;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.character.CharacterEntity;
import net.trounoir.character.Characters;
import net.trounoir.config.TNConfig;
import net.trounoir.data.CharacterDef;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.TNData;
import net.trounoir.network.Net;
import net.trounoir.rebirth.PersistentStore;
import net.trounoir.util.Fx;
import net.trounoir.util.JsonUtil;
import net.trounoir.world.SiteManager;
import net.trounoir.world.WorldState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Le monde réagit. À la mort d'un personnage, les réactions décrites dans son JSON s'appliquent :
 * deuil, alerte, traque, vengeance, succession (vide de pouvoir), réputation, annonce.
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ReactionEngine {
    private ReactionEngine() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || victim instanceof Player) return;
        MinecraftServer server = level.getServer();
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer p ? p : null;
        CharacterDef def = Characters.defOf(victim);

        if (def == null) {
            if (killer == null) return;
            // Créature ordinaire : réputation éventuelle (villageois…), ou service rendu près d'un lieu habité.
            SimpleDefs.ProfileDef profile = TNData.PROFILES.get(EntityType.getKey(victim.getType()));
            if (profile != null && profile.reputation() != 0) Reputation.add(server, profile.faction(), profile.reputation());
            if (victim instanceof Enemy) {
                SimpleDefs.SiteDef near = SiteManager.nearest(killer, 64);
                if (near != null && !near.faction().isEmpty()) Reputation.add(server, near.faction(), 1);
            }
            return;
        }

        WorldState state = WorldState.get(server);
        String id = def.id.toString();
        if (def.unique) state.deadUniques.add(id);

        if (killer != null) {
            def.killReputation.forEach((faction, delta) -> Reputation.add(server, faction, (int) Math.round(delta)));
            LifeData life = LifeManager.getOrNull(killer);
            if (life != null) {
                life.notoriety = Math.min(100, life.notoriety + def.notoriety);
                if (def.unique) {
                    life.uniqueKills.add(id);
                    life.feat("feat.trounoir.slew", "@" + def.nameKey());
                    if (def.id.getPath().equals("higgs")) victory(killer, life);
                }
                Net.syncLife(killer);
            }
        }

        long now = server.overworld().getGameTime();
        for (JsonObject reaction : def.onDeath) {
            // Certaines réactions n'ont de sens que si le joueur est le meurtrier.
            if (JsonUtil.bool(reaction, "only_if_player", true) && killer == null) continue;
            apply(server, state, now, reaction);
        }
        state.save();
    }

    private static void victory(ServerPlayer player, LifeData life) {
        PersistentStore store = PersistentStore.get(player.server);
        store.memory(player.getUUID()).victories++;
        store.save();
        life.feat("feat.trounoir.theomachy");
        Fx.title(player, Component.translatable("trounoir.victory.title"), Component.translatable("trounoir.victory.subtitle"), 160);
        Fx.playTo(player, "ui.toast.challenge_complete", 1.0F, 0.6F);
    }

    private static void apply(MinecraftServer server, WorldState state, long now, JsonObject reaction) {
        String faction = JsonUtil.str(reaction, "faction", "");
        switch (JsonUtil.str(reaction, "type", "")) {
            case "reputation" -> Reputation.add(server, faction, JsonUtil.integer(reaction, "amount", -10));
            case "alert" -> state.alertUntil.put(faction, now + JsonUtil.integer(reaction, "duration", 24000));
            case "mourning" -> state.mourningUntil.put(faction, now + JsonUtil.integer(reaction, "duration", 72000));
            case "hunt" -> {
                int waves = JsonUtil.integer(reaction, "waves", 2);
                int interval = JsonUtil.integer(reaction, "interval", 3600);
                int delay = JsonUtil.integer(reaction, "delay", 1200);
                for (int i = 0; i < waves; i++) schedule(state, now + delay + (long) i * interval, "hunt", "", faction);
            }
            case "vengeance" -> schedule(state, now + JsonUtil.integer(reaction, "delay", 2400), "vengeance", JsonUtil.str(reaction, "character", ""), faction);
            case "successor" -> schedule(state, now + JsonUtil.integer(reaction, "delay", 24000), "successor",
                    JsonUtil.str(reaction, "character", "") + "|" + JsonUtil.str(reaction, "replaces", ""), faction);
            case "message" -> {
                Component text = Component.translatable(JsonUtil.str(reaction, "key", ""));
                for (ServerPlayer player : server.getPlayerList().getPlayers()) player.displayClientMessage(text, false);
            }
            default -> { }
        }
    }

    private static void schedule(WorldState state, long at, String type, String character, String faction) {
        WorldState.Scheduled s = new WorldState.Scheduled();
        s.at = at;
        s.type = type;
        s.character = character;
        s.faction = faction;
        state.scheduled.add(s);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        WorldState state = WorldState.get(server);
        long now = server.overworld().getGameTime();

        if (!state.scheduled.isEmpty()) {
            List<WorldState.Scheduled> due = new ArrayList<>();
            Iterator<WorldState.Scheduled> it = state.scheduled.iterator();
            while (it.hasNext()) {
                WorldState.Scheduled s = it.next();
                if (s.at <= now) {
                    due.add(s);
                    it.remove();
                }
            }
            for (WorldState.Scheduled s : due) run(server, state, s);
            if (!due.isEmpty()) state.save();
        }

        // Le deuil s'entend : le glas sonne près des lieux de la faction endeuillée.
        if (server.getTickCount() % 200 == 0) {
            for (Map.Entry<String, Long> mourning : state.mourningUntil.entrySet()) {
                if (mourning.getValue() <= now) continue;
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    SimpleDefs.SiteDef near = SiteManager.nearest(player, 96);
                    if (near != null && near.faction().equals(mourning.getKey())) Fx.playTo(player, "block.bell.use", 0.7F, 0.5F);
                }
            }
        }
    }

    private static void run(MinecraftServer server, WorldState state, WorldState.Scheduled s) {
        switch (s.type) {
            case "hunt" -> {
                if (!TNConfig.HUNTS_ENABLED.get()) return;
                SimpleDefs.FactionDef faction = TNData.FACTIONS.get(s.faction);
                if (faction == null) return;
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
                    spawnSquad(player, faction);
                }
            }
            case "vengeance" -> {
                CharacterDef def = TNData.character(s.character);
                if (def == null || state.isDead(s.character)) return;
                ServerPlayer player = firstTarget(server);
                if (player == null) return;
                // L'unique quitte son foyer : l'ancienne copie est retirée, une nouvelle surgit près du joueur.
                state.generations.merge(s.character, 1, Integer::sum);
                removeLoaded(server, s.character);
                state.spawnedUniques.remove(s.character);
                BlockPos pos = around(player, 20);
                LivingEntity avenger = Characters.spawn(player.serverLevel(), def, pos, pos);
                if (avenger instanceof CharacterEntity character) character.provoke(player);
                player.displayClientMessage(Component.translatable("trounoir.reaction.vengeance", Component.translatable(def.nameKey())), false);
                Fx.playTo(player, "entity.wither.ambient", 0.6F, 0.6F);
            }
            case "successor" -> {
                String[] parts = s.character.split("\\|", -1);
                CharacterDef def = TNData.character(parts[0]);
                if (def == null || parts.length < 2) return;
                int[] home = state.homes.get(parts[1]);
                String dimension = state.homeDimensions.get(parts[1]);
                if (home == null || dimension == null) return;
                ResourceLocation dim = ResourceLocation.tryParse(dimension);
                ServerLevel level = dim == null ? null : SiteManager.level(server, dim);
                if (level == null) return;
                BlockPos pos = new BlockPos(home[0], home[1], home[2]);
                Characters.spawn(level, def, pos, pos);
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    player.displayClientMessage(Component.translatable("trounoir.reaction.successor", Component.translatable(def.nameKey())), false);
                }
            }
            default -> { }
        }
    }

    private static ServerPlayer firstTarget(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isAlive() && !player.isCreative() && !player.isSpectator()) return player;
        }
        return null;
    }

    private static void removeLoaded(MinecraftServer server, String uniqueId) {
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> found = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living) {
                    CharacterDef def = Characters.defOf(living);
                    if (def != null && def.id.toString().equals(uniqueId)) found.add(entity);
                }
            }
            found.forEach(Entity::discard);
        }
    }

    private static BlockPos around(ServerPlayer player, int distance) {
        double a = player.getRandom().nextDouble() * Math.PI * 2;
        int x = (int) Math.floor(player.getX() + Math.cos(a) * distance);
        int z = (int) Math.floor(player.getZ() + Math.sin(a) * distance);
        int y = player.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    /** Une escouade de traque surgit à distance du joueur et marche sur lui. */
    public static void spawnSquad(ServerPlayer player, SimpleDefs.FactionDef faction) {
        ServerLevel level = player.serverLevel();
        BlockPos anchor = around(player, 30);
        int index = 0;
        for (SimpleDefs.SquadEntry entry : faction.huntSquad()) {
            CharacterDef def = TNData.CHARACTERS.get(entry.character());
            if (def == null || def.unique) continue;
            for (int i = 0; i < entry.count(); i++) {
                BlockPos pos = anchor.offset(index % 3 - 1, 0, index / 3);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
                BlockPos ground = new BlockPos(pos.getX(), y, pos.getZ());
                LivingEntity spawned = Characters.spawn(level, def, ground, ground);
                if (spawned instanceof CharacterEntity character) character.provoke(player);
                index++;
            }
        }
        if (index > 0) {
            player.displayClientMessage(Component.translatable("trounoir.reaction.hunt", Component.translatable(faction.nameKey())), true);
            Fx.playTo(player, "event.raid.horn", 1.0F, 1.0F);
        }
    }
}
