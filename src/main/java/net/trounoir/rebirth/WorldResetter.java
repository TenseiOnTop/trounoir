package net.trounoir.rebirth;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.trounoir.TrouNoir;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Le « retour dans le passé ». L'état initial d'un monde est entièrement déterminé par sa graine :
 * plutôt que de copier un instantané, on efface tout ce qui a été vécu (tronçons, entités, joueurs,
 * données du mod) et on remet level.dat à l'heure zéro. Au rechargement, le monde se régénère
 * identique à ce qu'il était au tout premier instant, et le mod y replace ses lieux aux mêmes endroits.
 * <p>
 * L'effacement a lieu une fois le serveur ARRÊTÉ (fichiers fermés, verrou relâché). Les mémoires
 * du Trou-Noir vivent hors du dossier du monde et ne sont donc pas touchées.
 */
public final class WorldResetter {
    private WorldResetter() {}

    /** Tout ce qui porte la trace d'une vie. level.dat (graine, règles), datapacks et serverconfig sont conservés. */
    private static final String[] WIPED = {
            "region", "entities", "poi", "DIM-1", "DIM1", "dimensions", "data",
            "playerdata", "advancements", "stats", "trounoir", "level.dat_old"
    };

    private static volatile Path armedRoot;

    public static void arm(MinecraftServer server) {
        armedRoot = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
    }

    public static boolean isArmed() {
        return armedRoot != null;
    }

    /** À appeler quand le serveur est arrêté. Sans effet si aucune renaissance n'est en attente. */
    public static void executeIfArmed(MinecraftServer server) {
        Path root = armedRoot;
        armedRoot = null;
        if (root == null) return;
        PersistentStore store = PersistentStore.get(server);
        try {
            for (String name : WIPED) deleteRecursively(root.resolve(name));
            rewindLevelDat(root.resolve("level.dat"));
            store.resetFailed = false;
            TrouNoir.LOGGER.info("[Trou-Noir] Le monde {} est revenu à son premier instant", root.getFileName());
        } catch (IOException | RuntimeException e) {
            // Un monde à moitié effacé reste jouable (les tronçons manquants se régénèrent) ;
            // la renaissance douce terminera le travail à la prochaine connexion.
            store.resetFailed = true;
            TrouNoir.LOGGER.error("[Trou-Noir] Retour dans le passé incomplet pour {}", root, e);
        }
        store.save();
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Remet l'horloge, la météo, le joueur solo et les combats de boss de level.dat à leur état d'origine. */
    private static void rewindLevelDat(Path levelDat) throws IOException {
        if (!Files.exists(levelDat)) return;
        CompoundTag root = NbtIo.readCompressed(levelDat, NbtAccounter.unlimitedHeap());
        CompoundTag data = root.getCompound("Data");
        data.remove("Player");
        data.remove("DragonFight");
        data.remove("CustomBossEvents");
        data.remove("ScheduledEvents");
        data.remove("WanderingTraderId");
        data.putLong("Time", 0L);
        data.putLong("DayTime", 0L);
        data.putBoolean("raining", false);
        data.putBoolean("thundering", false);
        data.putInt("rainTime", 0);
        data.putInt("thunderTime", 0);
        data.putInt("clearWeatherTime", 0);
        data.putInt("WanderingTraderSpawnDelay", 24000);
        data.putInt("WanderingTraderSpawnChance", 25);
        root.put("Data", data);
        NbtIo.writeCompressed(root, levelDat);
    }
}
