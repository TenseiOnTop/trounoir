package net.trounoir.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.trounoir.TrouNoir;

/**
 * Côté client du retour dans le passé (solo) : quitter le monde, laisser le serveur intégré
 * s'arrêter et effacer ce qui a été vécu, puis rouvrir le même monde, revenu à son premier instant.
 */
public final class ClientRebirthFlow {
    private ClientRebirthFlow() {}

    private static String pendingLevel;
    private static boolean pendingRespawn;
    private static boolean running;

    public static void requestReopen(String levelId) {
        if (!levelId.isEmpty()) pendingLevel = levelId;
    }

    public static void requestRespawn() {
        pendingRespawn = true;
    }

    /** Appelé à chaque tick client, hors de tout traitement de paquet. */
    public static void tick(Minecraft mc) {
        if (running) return;
        if (pendingRespawn && mc.player != null) {
            pendingRespawn = false;
            ClientState.death = null;
            // L'écran de Renaissance se referme de lui-même dès que le joueur est de nouveau en vie.
            if (mc.player.isDeadOrDying()) mc.player.respawn();
        }
        if (pendingLevel == null) return;
        String levelId = pendingLevel;
        pendingLevel = null;
        running = true;
        try {
            ClientState.reset();
            if (mc.level != null) mc.level.disconnect();
            // disconnect() attend l'arrêt complet du serveur intégré : à son retour, le monde a été effacé.
            mc.disconnect(new BlackScreen(Component.translatable("trounoir.rebirth.rewinding")));
            mc.createWorldOpenFlows().openWorld(levelId, () -> mc.setScreen(new TitleScreen()));
        } catch (RuntimeException e) {
            TrouNoir.LOGGER.error("[Trou-Noir] Impossible de rouvrir le monde {}", levelId, e);
            mc.setScreen(new TitleScreen());
        } finally {
            running = false;
        }
    }
}
