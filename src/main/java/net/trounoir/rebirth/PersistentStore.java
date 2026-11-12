package net.trounoir.rebirth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import net.trounoir.TrouNoir;
import net.trounoir.chronicle.LifeRecord;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tout ce qui survit à la Renaissance : Chronique, Échos, souvenirs.
 * Enregistré HORS de la sauvegarde du monde (&lt;dossier du jeu&gt;/trounoir/memoires/&lt;monde&gt;.json),
 * afin de ne pas être effacé lorsque le monde est ramené à son état initial.
 */
public final class PersistentStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class Echo {
        public String id = "";
        public int level = 1;
    }

    /** Mémoire d'un joueur à travers ses vies. */
    public static final class Memory {
        public int deaths;
        public int victories;
        public List<LifeRecord> chronicle = new ArrayList<>();
        public List<Echo> echoes = new ArrayList<>();
        public Set<String> knowledge = new LinkedHashSet<>();
        public Set<String> places = new LinkedHashSet<>();
        public Set<String> weaknesses = new LinkedHashSet<>();
        /** relique -> lieu où son coffre a été trouvé. */
        public Map<String, String> relics = new HashMap<>();
        /** Nom choisi pour la prochaine vie (vide pour la toute première). */
        public String nextName = "";
        /** Vrai entre la mort et le choix de l'Écho/du nom. */
        public boolean awaitingChoice;
    }

    public Map<String, Memory> players = new HashMap<>();
    /** Positions de lieux déjà résolues (déterministes pour une graine donnée). */
    public Map<String, int[]> sitePositions = new HashMap<>();
    public long seed;
    public int[] origin;
    /** La dernière réinitialisation du monde a échoué : une renaissance douce prendra le relais. */
    public boolean resetFailed;
    /**
     * Vrai si ce monde est né avec le mod : lui seul peut être ramené à son premier instant.
     * Un monde préexistant auquel on ajoute le mod n'est JAMAIS effacé (renaissance douce uniquement).
     */
    public Boolean bound;

    private transient Path file;

    private static PersistentStore current;
    private static MinecraftServer currentServer;

    public static synchronized PersistentStore get(MinecraftServer server) {
        if (current == null || currentServer != server) {
            Path file = fileFor(server);
            current = load(file);
            currentServer = server;
            long seed = server.getWorldData().worldGenOptions().seed();
            if (current.seed != seed) {
                if (current.seed != 0L || !current.players.isEmpty()) {
                    // Même nom de dossier mais autre graine : c'est un autre univers.
                    // Les mémoires de l'ancien sont archivées, celui-ci repart d'une page blanche.
                    current.file = file.resolveSibling(levelId(server) + "-" + current.seed + ".json");
                    current.save();
                    current = new PersistentStore();
                    current.file = file;
                }
                current.seed = seed;
            }
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

    public static String levelId(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        return root.getFileName().toString();
    }

    private static Path fileFor(MinecraftServer server) {
        return FMLPaths.GAMEDIR.get().resolve("trounoir").resolve("memoires").resolve(levelId(server) + ".json");
    }

    private static PersistentStore load(Path file) {
        PersistentStore store = null;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                store = GSON.fromJson(reader, PersistentStore.class);
            } catch (IOException | RuntimeException e) {
                TrouNoir.LOGGER.error("[Trou-Noir] Mémoires illisibles ({}), une copie .corrompu est conservée", file, e);
                try {
                    Files.copy(file, file.resolveSibling(file.getFileName() + ".corrompu"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                    // rien de plus à faire
                }
            }
        }
        if (store == null) store = new PersistentStore();
        if (store.players == null) store.players = new HashMap<>();
        if (store.sitePositions == null) store.sitePositions = new HashMap<>();
        store.file = file;
        return store;
    }

    public synchronized void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            TrouNoir.LOGGER.error("[Trou-Noir] Impossible d'enregistrer les mémoires dans {}", file, e);
        }
    }

    public synchronized Memory memory(UUID player) {
        Memory memory = players.computeIfAbsent(player.toString(), k -> new Memory());
        if (memory.chronicle == null) memory.chronicle = new ArrayList<>();
        if (memory.echoes == null) memory.echoes = new ArrayList<>();
        if (memory.knowledge == null) memory.knowledge = new LinkedHashSet<>();
        if (memory.places == null) memory.places = new LinkedHashSet<>();
        if (memory.weaknesses == null) memory.weaknesses = new LinkedHashSet<>();
        if (memory.relics == null) memory.relics = new HashMap<>();
        if (memory.nextName == null) memory.nextName = "";
        return memory;
    }
}
