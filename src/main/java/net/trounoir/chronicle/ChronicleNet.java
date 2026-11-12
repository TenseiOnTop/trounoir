package net.trounoir.chronicle;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.network.Net;
import net.trounoir.rebirth.PersistentStore;

/** Envoi de la Chronique des Vies au client qui ouvre l'écran des constellations. */
public final class ChronicleNet {
    private ChronicleNet() {}

    private static final Gson GSON = new Gson();

    public static void send(ServerPlayer player) {
        PersistentStore.Memory memory = PersistentStore.get(player.server).memory(player.getUUID());
        JsonObject root = new JsonObject();
        root.add("lives", GSON.toJsonTree(memory.chronicle));
        root.addProperty("deaths", memory.deaths);
        root.addProperty("victories", memory.victories);
        LifeData life = LifeManager.getOrNull(player);
        if (life != null) {
            root.addProperty("currentNumber", life.lifeNumber);
            root.addProperty("currentName", life.name);
            root.addProperty("currentTicks", life.ticksLived);
        }
        Net.toClient(player, "chronicle", root);
    }
}
