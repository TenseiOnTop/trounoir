package net.trounoir.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.LifeManager;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * État du monde pour la vie en cours : personnages uniques morts, lieux bâtis, réputations,
 * alertes, deuils, événements programmés. Enregistré dans le monde, donc remis à zéro à chaque Renaissance.
 */
public final class WorldState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Événement différé (traque, vengeance, succession). */
    public static final class Scheduled {
        public long at;
        public String type = "";
        public String character = "";
        public String faction = "";
    }

    public Set<String> deadUniques = new LinkedHashSet<>();
    public Set<String> spawnedUniques = new LinkedHashSet<>();
    public Set<String> builtSites = new LinkedHashSet<>();
    public Set<String> populatedSites = new LinkedHashSet<>();
    public Map<String, Integer> reputation = new HashMap<>();
    public Map<String, Long> alertUntil = new HashMap<>();
    public Map<String, Long> mourningUntil = new HashMap<>();
    public List<Scheduled> scheduled = new ArrayList<>();
    /** identifiant d'unique -> [x, y, z] de son foyer, et sa dimension. */
    public Map<String, int[]> homes = new HashMap<>();
    public Map<String, String> homeDimensions = new HashMap<>();
    /** Génération de chaque unique : une copie d'une génération passée est retirée du monde. */
    public Map<String, Integer> generations = new HashMap<>();
    /** Époque : augmente à chaque renaissance douce, pour écarter les personnages de la vie précédente. */
    public int epoch;

    private transient Path file;
    private static WorldState current;
    private static MinecraftServer currentServer;

    public static synchronized WorldState get(MinecraftServer server) {
        if (current == null || currentServer != server) {
            current = load(LifeManager.worldDir(server).resolve("state.json"));
            currentServer = server;
        }
        return current;
    }

    public static synchronized void release(MinecraftServer server) {
        if (currentServer == server) {
            if (current != null) current.save();
            current = null;
            currentServer = null;
        }
    }

    /** Oublie l'état en mémoire sans rien écrire (le dossier du monde vient d'être effacé). */
    public static synchronized void forget(MinecraftServer server) {
        if (currentServer == server) {
            current = null;
            currentServer = null;
        }
    }

    private static WorldState load(Path file) {
        WorldState state = null;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                state = GSON.fromJson(reader, WorldState.class);
            } catch (IOException | RuntimeException e) {
                TrouNoir.LOGGER.error("[Trou-Noir] État du monde illisible, repart de zéro", e);
            }
        }
        if (state == null) state = new WorldState();
        state.file = file;
        return state;
    }

    public synchronized void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            TrouNoir.LOGGER.error("[Trou-Noir] Impossible d'enregistrer l'état du monde", e);
        }
    }

    public boolean isDead(String unique) {
        return deadUniques.contains(unique);
    }

    public int generation(String unique) {
        return generations.getOrDefault(unique, 0);
    }

    /** Renaissance douce : tout l'état lié à la vie précédente est oublié, les lieux restent bâtis. */
    public void resetForNewLife() {
        deadUniques.clear();
        spawnedUniques.clear();
        populatedSites.clear();
        reputation.clear();
        alertUntil.clear();
        mourningUntil.clear();
        scheduled.clear();
        homes.clear();
        homeDimensions.clear();
        generations.clear();
        epoch++;
        save();
    }
}
