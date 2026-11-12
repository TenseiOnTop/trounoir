package net.trounoir.client;

import com.google.gson.JsonObject;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.network.Net;
import net.trounoir.registry.ModRegistry;
import org.lwjgl.glfw.GLFW;

/** Entrées clavier, écrans et affichage tête haute côté client. */
public final class ClientEvents {
    private ClientEvents() {}

    private static final String CATEGORY = "key.categories.trounoir";
    public static final KeyMapping[] TECHNIQUE_KEYS = {
            new KeyMapping("key.trounoir.technique_1", GLFW.GLFW_KEY_R, CATEGORY),
            new KeyMapping("key.trounoir.technique_2", GLFW.GLFW_KEY_V, CATEGORY),
            new KeyMapping("key.trounoir.technique_3", GLFW.GLFW_KEY_G, CATEGORY),
            new KeyMapping("key.trounoir.technique_4", GLFW.GLFW_KEY_B, CATEGORY)
    };
    public static final KeyMapping OPEN_ABSORPTION = new KeyMapping("key.trounoir.absorption", GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping OPEN_CHRONICLE = new KeyMapping("key.trounoir.chronicle", GLFW.GLFW_KEY_J, CATEGORY);

    @Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            for (KeyMapping key : TECHNIQUE_KEYS) event.register(key);
            event.register(OPEN_ABSORPTION);
            event.register(OPEN_CHRONICLE);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModRegistry.CHARACTER.get(), CharacterRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            ClientState.clientTick++;
            ClientRebirthFlow.tick(mc);
            if (mc.player == null || mc.screen != null) return;

            for (int i = 0; i < TECHNIQUE_KEYS.length; i++) {
                while (TECHNIQUE_KEYS[i].consumeClick()) {
                    String id = ClientState.SLOTS[i];
                    if (id == null || ClientState.cooldownFraction(id) > 0) continue;
                    JsonObject json = new JsonObject();
                    json.addProperty("slot", i);
                    Net.toServer("use", json);
                }
            }
            // L'interface se dévoile : l'écran d'Absorption n'existe pas tant que rien n'a été absorbé,
            // la Chronique tant qu'aucune vie ne s'est achevée.
            while (OPEN_ABSORPTION.consumeClick()) {
                if (ClientState.revealed || !ClientState.ECHOES.isEmpty() || ClientState.hasAny(AbsorbableDef.Kind.KNOWLEDGE)) {
                    mc.setScreen(new AbsorptionScreen());
                }
            }
            while (OPEN_CHRONICLE.consumeClick()) {
                if (ClientState.deaths > 0) {
                    Net.toServer("chronicle", new JsonObject());
                    mc.setScreen(new ChronicleScreen());
                }
            }
        }

        /** La mort n'offre pas de bouton « Réapparaître » : elle ouvre la Renaissance. */
        @SubscribeEvent
        public static void onScreenOpening(ScreenEvent.Opening event) {
            if (event.getNewScreen() instanceof DeathScreen) {
                event.setNewScreen(Minecraft.getInstance().screen instanceof RebirthScreen current ? current : new RebirthScreen());
            }
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Hud.render(event.getGuiGraphics());
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientState.reset();
        }
    }
}
