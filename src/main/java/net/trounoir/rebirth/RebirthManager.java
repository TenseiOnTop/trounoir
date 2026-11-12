package net.trounoir.rebirth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.Bonuses;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.character.Characters;
import net.trounoir.chronicle.ChronicleRecorder;
import net.trounoir.chronicle.LifeRecord;
import net.trounoir.config.TNConfig;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.TNData;
import net.trounoir.network.Net;
import net.trounoir.technique.TimeStop;
import net.trounoir.util.Fx;
import net.trounoir.world.SiteManager;
import net.trounoir.world.WorldState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * La boucle de Renaissance : mort -> Chronique -> choix de l'Écho et du nom -> retour au premier instant.
 * La puissance se perd, la connaissance reste.
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RebirthManager {
    private RebirthManager() {}

    private static final Gson GSON = new Gson();
    /** Vies achevées en attente du choix du joueur (pour valider l'Écho demandé). */
    private static final Map<UUID, LifeData> ENDED = new HashMap<>();
    /** Joueurs dont la prochaine réapparition ouvre une nouvelle vie (renaissance douce). */
    private static final Set<UUID> AWAITING_RESPAWN = new HashSet<>();

    // ------------------------------------------------------------------ mort

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        LifeData life = LifeManager.getOrNull(player);
        if (life == null) return;
        PersistentStore store = PersistentStore.get(player.server);
        PersistentStore.Memory memory = store.memory(player.getUUID());

        LifeRecord record = ChronicleRecorder.finish(player, life, event.getSource(), memory);
        memory.chronicle.add(record);
        memory.deaths++;
        memory.awaitingChoice = true;
        // Mourir enseigne : la faiblesse de celui qui vous a tué est désormais un souvenir.
        if (!record.killerId.isEmpty()) memory.weaknesses.add(record.killerId);
        store.save();

        LifeManager.save(player);
        ENDED.put(player.getUUID(), life);
        sendDeathSummary(player, life, memory, record);
    }

    private static void sendDeathSummary(ServerPlayer player, LifeData life, PersistentStore.Memory memory, LifeRecord record) {
        JsonObject root = new JsonObject();
        if (record != null) root.add("record", GSON.toJsonTree(record));
        JsonArray candidates = new JsonArray();
        for (String id : life.xp.keySet()) {
            AbsorbableDef def = TNData.absorbable(id);
            int level = life.level(id);
            if (def == null || level <= 0 || def.kind == AbsorbableDef.Kind.KNOWLEDGE) continue;
            JsonObject c = new JsonObject();
            c.addProperty("id", id);
            c.addProperty("level", Math.min(level, TNConfig.ECHO_MAX_LEVEL.get()));
            candidates.add(c);
        }
        root.add("candidates", candidates);
        JsonArray echoes = new JsonArray();
        for (PersistentStore.Echo echo : memory.echoes) {
            JsonObject e = new JsonObject();
            e.addProperty("id", echo.id);
            e.addProperty("level", echo.level);
            echoes.add(e);
        }
        root.add("echoes", echoes);
        root.addProperty("slots", EchoLogic.slotsFor(memory.deaths));
        root.addProperty("deaths", memory.deaths);
        Net.toClient(player, "death", root);
    }

    // ------------------------------------------------------------------ choix

    public static void onChoice(ServerPlayer player, String echoId, int replace, String name) {
        MinecraftServer server = player.server;
        PersistentStore store = PersistentStore.get(server);
        PersistentStore.Memory memory = store.memory(player.getUUID());
        if (!memory.awaitingChoice) return;

        LifeData ended = ENDED.remove(player.getUUID());
        if (ended != null) EchoLogic.choose(memory, ended, echoId, replace);
        memory.nextName = cleanName(name);
        memory.awaitingChoice = false;
        store.save();

        if (worldResetApplies(server)) {
            WorldResetter.arm(server);
            if (server.isDedicatedServer()) {
                for (ServerPlayer other : new ArrayList<>(server.getPlayerList().getPlayers())) {
                    other.connection.disconnect(Component.translatable("trounoir.rebirth.server_rewind"));
                }
                server.halt(false);
            } else {
                JsonObject go = new JsonObject();
                go.addProperty("worldReset", true);
                go.addProperty("level", PersistentStore.levelId(server));
                Net.toClient(player, "go", go);
            }
        } else {
            softRebirth(player);
        }
    }

    private static String cleanName(String raw) {
        StringBuilder out = new StringBuilder();
        for (char ch : raw.trim().toCharArray()) {
            if (ch >= ' ' && ch != '§' && out.length() < 24) out.append(ch);
        }
        return out.length() == 0 ? "?" : out.toString();
    }

    /** Le monde entier doit-il revenir à son premier instant ? */
    public static boolean worldResetApplies(MinecraftServer server) {
        PersistentStore store = PersistentStore.get(server);
        if (store.resetFailed || !Boolean.TRUE.equals(store.bound)) return false;
        return switch (TNConfig.REBIRTH_MODE.get()) {
            case WORLD_RESET -> true;
            case SOFT -> false;
            case AUTO -> !server.isDedicatedServer() && server.getPlayerList().getPlayerCount() <= 1;
        };
    }

    // ------------------------------------------------------------------ renaissance douce

    /**
     * Réinitialisation contrôlée de tout l'état géré par le mod, sans toucher au terrain :
     * uniques ressuscités, réputations et événements oubliés, horloge remise à l'aube.
     */
    public static void softRebirth(ServerPlayer player) {
        MinecraftServer server = player.server;
        resetWorldState(server);
        LifeManager.put(player, new LifeData());
        AWAITING_RESPAWN.add(player.getUUID());
        JsonObject go = new JsonObject();
        go.addProperty("worldReset", false);
        Net.toClient(player, "go", go);
        // Si le joueur est encore en vie (commande de debug), la nouvelle vie commence tout de suite.
        if (player.isAlive()) {
            AWAITING_RESPAWN.remove(player.getUUID());
            beginLife(player);
        }
    }

    private static void resetWorldState(MinecraftServer server) {
        WorldState.get(server).resetForNewLife();
        TimeStop.clearAll();
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> stale = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && Characters.defOf(living) != null) stale.add(entity);
            }
            stale.forEach(Entity::discard);
        }
        server.overworld().setDayTime(0L);
        server.overworld().setWeatherParameters(12000, 0, false, false);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (AWAITING_RESPAWN.remove(player.getUUID()) || LifeManager.getOrNull(player) == null) {
            beginLife(player);
        } else {
            // Réapparition sans renaissance (ne devrait pas arriver) : on rétablit au moins les bonus.
            Bonuses.recompute(player);
            Net.syncLife(player);
        }
    }

    // ------------------------------------------------------------------ naissance

    /** Ouvre une nouvelle vie : capacités de base, Échos, point de renaissance. */
    public static void beginLife(ServerPlayer player) {
        MinecraftServer server = player.server;
        PersistentStore store = PersistentStore.get(server);
        PersistentStore.Memory memory = store.memory(player.getUUID());

        LifeData life = new LifeData();
        life.lifeNumber = memory.deaths + 1;
        life.name = memory.deaths == 0 ? "" : memory.nextName;      // la première vie est sans nom
        int slots = EchoLogic.slotsFor(memory.deaths);
        for (int i = 0; i < Math.min(slots, memory.echoes.size()); i++) life.echoes.add(memory.echoes.get(i));
        LifeManager.put(player, life);

        // Sur un monde préexistant, la toute première vie ne dépouille pas le joueur de ce qu'il possédait.
        if (memory.deaths > 0 || Boolean.TRUE.equals(store.bound)) {
            player.getInventory().clearContent();
            player.getEnderChestInventory().clearContent();
            player.setExperienceLevels(0);
            player.setExperiencePoints(0);
        }
        player.removeAllEffects();
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        player.clearFire();
        giveGuide(player);

        BlockPos origin = SiteManager.origin(server);
        ServerLevel overworld = server.overworld();
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
        player.teleportTo(overworld, origin.getX() + 0.5, y, origin.getZ() + 0.5, 0F, 0F);
        player.setRespawnPosition(Level.OVERWORLD, new BlockPos(origin.getX(), y, origin.getZ()), 0F, true, false);

        Bonuses.recompute(player);
        player.setHealth(player.getMaxHealth());
        LifeManager.save(player);
        Net.syncDefs(player);
        Net.syncLife(player);

        if (life.lifeNumber > 1) {
            Fx.title(player, Component.literal(life.name), Component.translatable("trounoir.rebirth.life_number", life.lifeNumber), 60);
            Fx.playTo(player, "block.respawn_anchor.set_spawn", 0.8F, 0.5F);
        }
    }

    /** Le carnet remis à chaque naissance : l'essentiel pour se repérer, sans rien dévoiler du monde. */
    private static void giveGuide(ServerPlayer player) {
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int i = 1; i <= GUIDE_PAGES; i++) pages.add(Filterable.passThrough(Component.translatable("trounoir.book.page" + i)));
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Trou-Noir"), "?", 0, pages, true));
        player.getInventory().add(book);
    }

    private static final int GUIDE_PAGES = 7;

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.server;
        PersistentStore store = PersistentStore.get(server);
        PersistentStore.Memory memory = store.memory(player.getUUID());
        Net.syncDefs(player);

        if (store.resetFailed) {
            // Le dernier retour dans le passé n'a pas abouti : on termine en douceur.
            store.resetFailed = false;
            store.save();
            memory.awaitingChoice = false;
            TrouNoir.LOGGER.warn("[Trou-Noir] Reprise en renaissance douce après un retour dans le passé incomplet");
            LifeManager.put(player, new LifeData());
            softRebirth(player);
            return;
        }
        if (!LifeManager.hasSavedLife(player)) {
            memory.awaitingChoice = false;
            beginLife(player);
            return;
        }
        LifeData life = LifeManager.load(player);
        Bonuses.recompute(player);
        Net.syncLife(player);
        if (memory.awaitingChoice && player.isDeadOrDying()) {
            // Déconnexion entre la mort et le choix : on repose la question.
            ENDED.put(player.getUUID(), life);
            LifeRecord last = memory.chronicle.isEmpty() ? null : memory.chronicle.get(memory.chronicle.size() - 1);
            sendDeathSummary(player, life, memory, last);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Pendant un retour dans le passé, rien de la vie achevée ne doit être réécrit dans le monde.
        if (!WorldResetter.isArmed()) LifeManager.unload(player);
        Bonuses.forget(player);
    }

    // ------------------------------------------------------------------ cycle du serveur

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        PersistentStore store = PersistentStore.get(server);
        if (store.bound == null) {
            // Un monde tout neuf appartient à la boucle ; un monde déjà vécu ne sera jamais effacé par le mod.
            store.bound = server.overworld().getGameTime() < 600L;
            if (!store.bound) {
                TrouNoir.LOGGER.warn("[Trou-Noir] Monde préexistant détecté : le retour dans le passé complet est désactivé pour lui (renaissance douce).");
            }
            store.save();
        }
        WorldState.get(server);
        SiteManager.origin(server);
    }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (WorldResetter.isArmed()) return;
        MinecraftServer server = level.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) LifeManager.save(player);
        WorldState.get(server).save();
        PersistentStore.get(server).save();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (WorldResetter.isArmed()) return;
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) LifeManager.save(player);
        WorldState.get(server).save();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MinecraftServer server = event.getServer();
        boolean rewinding = WorldResetter.isArmed();
        if (!rewinding) WorldState.release(server);
        WorldResetter.executeIfArmed(server);
        if (rewinding) WorldState.forget(server);
        PersistentStore.release(server);
        LifeManager.clearAll();
        TimeStop.clearAll();
        ENDED.clear();
        AWAITING_RESPAWN.clear();
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        for (ServerPlayer player : event.getPlayers()) {
            Net.syncDefs(player);
            LifeData life = LifeManager.getOrNull(player);
            if (life != null) life.dirty = true;
        }
    }
}
