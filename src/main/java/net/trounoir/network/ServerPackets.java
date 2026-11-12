package net.trounoir.network;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.trounoir.chronicle.ChronicleNet;
import net.trounoir.rebirth.RebirthManager;
import net.trounoir.technique.TechniqueHandler;
import net.trounoir.util.JsonUtil;

/** Traitement des messages envoyés par le client. Chaque valeur reçue est revalidée côté serveur. */
public final class ServerPackets {
    private ServerPackets() {}

    public static void handle(ServerPlayer player, String type, JsonObject json) {
        switch (type) {
            case "use" -> TechniqueHandler.use(player, JsonUtil.integer(json, "slot", -1));
            case "assign" -> TechniqueHandler.assign(player, JsonUtil.integer(json, "slot", -1), JsonUtil.str(json, "id", ""));
            case "rebirth" -> RebirthManager.onChoice(player, JsonUtil.str(json, "echo", ""),
                    JsonUtil.integer(json, "replace", -1), JsonUtil.str(json, "name", ""));
            case "chronicle" -> ChronicleNet.send(player);
            default -> { }
        }
    }
}
