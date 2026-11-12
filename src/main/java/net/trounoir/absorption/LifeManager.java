package net.trounoir.absorption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.trounoir.TrouNoir;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Accès aux données de vie des joueurs connectés, et lecture/écriture dans le dossier du monde. */
public final class LifeManager {
    private LifeManager() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, LifeData> LIVE = new ConcurrentHashMap<>();

    public static Path worldDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("trounoir");
    }

    private static Path file(ServerPlayer player) {
        return worldDir(player.server).resolve("players").resolve(player.getUUID() + ".json");
    }

    /** Données de vie du joueur ; nul s'il n'a pas encore été initialisé (voir RebirthManager). */
    public static LifeData getOrNull(ServerPlayer player) {
        return LIVE.get(player.getUUID());
    }

    public static LifeData get(ServerPlayer player) {
        return LIVE.computeIfAbsent(player.getUUID(), k -> {
            LifeData fresh = new LifeData();
            fresh.sanitize();
            return fresh;
        });
    }

    public static boolean hasSavedLife(ServerPlayer player) {
        return Files.exists(file(player));
    }

    public static LifeData load(ServerPlayer player) {
        Path file = file(player);
        LifeData life = null;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                life = GSON.fromJson(reader, LifeData.class);
            } catch (IOException | RuntimeException e) {
                TrouNoir.LOGGER.error("[Trou-Noir] Données de vie illisibles pour {}", player.getGameProfile().getName(), e);
            }
        }
        if (life == null) life = new LifeData();
        life.sanitize();
        LIVE.put(player.getUUID(), life);
        return life;
    }

    public static void put(ServerPlayer player, LifeData life) {
        life.sanitize();
        LIVE.put(player.getUUID(), life);
    }

    public static void save(ServerPlayer player) {
        LifeData life = LIVE.get(player.getUUID());
        if (life == null) return;
        Path file = file(player);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(life, writer);
            }
        } catch (IOException e) {
            TrouNoir.LOGGER.error("[Trou-Noir] Impossible d'enregistrer la vie de {}", player.getGameProfile().getName(), e);
        }
    }

    public static void unload(ServerPlayer player) {
        save(player);
        LIVE.remove(player.getUUID());
    }

    public static void clearAll() {
        LIVE.clear();
    }
}
