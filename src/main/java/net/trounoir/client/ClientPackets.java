package net.trounoir.client;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.trounoir.util.JsonUtil;

/** Traitement des messages reçus du serveur. */
public final class ClientPackets {
    private ClientPackets() {}

    public static void handle(String type, JsonObject json) {
        Minecraft mc = Minecraft.getInstance();
        switch (type) {
            case "defs" -> ClientState.readDefs(json);
            case "hit" -> {
                ClientState.shakeStrength = (float) net.trounoir.util.JsonUtil.num(json, "power", 0.3);
                ClientState.shakeTicks = 4 + Math.round(ClientState.shakeStrength * 6);
            }
            case "life" -> ClientState.readLife(json);
            case "death" -> {
                ClientState.death = json;
                if (!(mc.screen instanceof RebirthScreen)) mc.setScreen(new RebirthScreen());
            }
            case "go" -> {
                if (JsonUtil.bool(json, "worldReset", false)) {
                    ClientRebirthFlow.requestReopen(JsonUtil.str(json, "level", ""));
                } else {
                    ClientRebirthFlow.requestRespawn();
                }
            }
            case "chronicle" -> {
                ClientState.chronicle = json;
                if (mc.screen instanceof ChronicleScreen screen) screen.reload();
            }
            default -> { }
        }
    }
}
