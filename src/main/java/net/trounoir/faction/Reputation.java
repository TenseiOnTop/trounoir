package net.trounoir.faction;

import net.minecraft.server.MinecraftServer;
import net.trounoir.config.TNConfig;
import net.trounoir.data.TNData;
import net.trounoir.world.WorldState;

import java.util.ArrayList;
import java.util.List;

/** Réputation du Trou-Noir auprès de chaque faction, pour la vie en cours. */
public final class Reputation {
    private Reputation() {}

    public static int get(MinecraftServer server, String faction) {
        return WorldState.get(server).reputation.getOrDefault(faction, 0);
    }

    public static void add(MinecraftServer server, String faction, int delta) {
        if (faction == null || faction.isEmpty() || delta == 0) return;
        WorldState state = WorldState.get(server);
        int value = Math.max(-100, Math.min(100, state.reputation.getOrDefault(faction, 0) + delta));
        state.reputation.put(faction, value);
    }

    public static boolean onAlert(MinecraftServer server, String faction) {
        Long until = WorldState.get(server).alertUntil.get(faction);
        return until != null && until > server.overworld().getGameTime();
    }

    public static boolean inMourning(MinecraftServer server, String faction) {
        Long until = WorldState.get(server).mourningUntil.get(faction);
        return until != null && until > server.overworld().getGameTime();
    }

    /** La faction attaque-t-elle le joueur à vue ? */
    public static boolean hostile(MinecraftServer server, String faction) {
        if (faction == null || faction.isEmpty()) return false;
        return get(server, faction) <= TNConfig.HOSTILE_REPUTATION.get() || onAlert(server, faction);
    }

    public static List<String> allies(MinecraftServer server) {
        List<String> out = new ArrayList<>();
        for (String faction : TNData.FACTIONS.keySet()) {
            if (get(server, faction) >= TNConfig.ALLY_REPUTATION.get()) out.add(faction);
        }
        return out;
    }
}
