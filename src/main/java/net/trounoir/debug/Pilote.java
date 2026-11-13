package net.trounoir.debug;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Pilote de test : s'il existe un fichier pilote.txt dans le dossier du jeu, le client crée un monde
 * et déroule le scénario (commandes, attentes, captures d'écran) puis s'éteint. Sert aux captures en CI.
 * Lignes : wait N | cmd <commande sans /> | shot <nom> | view first|third|front | quit
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Pilote {
    private Pilote() {}

    private static boolean checked;
    private static List<String> lines;
    private static int index;
    private static int waitTicks;
    private static boolean creating;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!checked) {
            checked = true;
            Path file = mc.gameDirectory.toPath().resolve("pilote.txt");
            if (Files.exists(file)) {
                try {
                    lines = Files.readAllLines(file);
                    TrouNoir.LOGGER.info("[Pilote] Scénario de {} lignes", lines.size());
                } catch (Exception e) {
                    TrouNoir.LOGGER.error("[Pilote] Lecture impossible", e);
                }
            }
        }
        if (lines == null) return;

        if (!creating) {
            if (mc.screen instanceof TitleScreen) {
                creating = true;
                TrouNoir.LOGGER.info("[Pilote] Création du monde");
                LevelSettings settings = new LevelSettings("pilote", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                        new GameRules(), WorldDataConfiguration.DEFAULT);
                WorldOptions options = new WorldOptions(20261113L, true, false);
                mc.createWorldOpenFlows().createFreshLevel("pilote", settings, options, WorldPresets::createNormalWorldDimensions, mc.screen);
            }
            return;
        }
        if (mc.player == null || mc.level == null) return;
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        while (index < lines.size()) {
            String line = lines.get(index++).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            TrouNoir.LOGGER.info("[Pilote] {}", line);
            if (line.startsWith("wait ")) {
                waitTicks = Integer.parseInt(line.substring(5).trim());
                return;
            } else if (line.startsWith("cmd ")) {
                mc.player.connection.sendCommand(line.substring(4).trim());
            } else if (line.startsWith("shot ")) {
                String name = line.substring(5).trim() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), component -> TrouNoir.LOGGER.info("[Pilote] capture {}", name));
                waitTicks = 2;
                return;
            } else if (line.startsWith("view ")) {
                String view = line.substring(5).trim();
                mc.options.setCameraType(view.equals("third") ? CameraType.THIRD_PERSON_BACK
                        : view.equals("front") ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON);
            } else if (line.equals("quit")) {
                TrouNoir.LOGGER.info("[Pilote] Fin du scénario");
                lines = null;
                mc.stop();
                return;
            }
        }
        // Scénario épuisé sans quit : on coupe quand même.
        lines = null;
        mc.stop();
    }
}
