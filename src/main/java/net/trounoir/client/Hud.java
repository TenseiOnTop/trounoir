package net.trounoir.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.util.Lookup;
import net.trounoir.util.Text;
import net.trounoir.world.Crossing;

/**
 * Affichage tête haute. Rien n'est montré avant d'avoir un sens : le rang apparaît à la première
 * absorption, les emplacements à la première technique, l'œil de Suke à la première notoriété.
 */
public final class Hud {
    private Hud() {}

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen instanceof RebirthScreen) return;
        Font font = mc.font;
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        Compass.render(g, mc, font, width);

        if (ClientState.revealed) {
            g.drawString(font, I18n.get("trounoir.hud.rank", Text.roman(ClientState.rank), (int) Math.round(ClientState.power)), 6, 6, 0xC9B8FF);
            if (!ClientState.lifeName.isEmpty()) g.drawString(font, ClientState.lifeName, 6, 17, 0x8F86A8);
        }

        boolean anyTechnique = false;
        for (String slot : ClientState.SLOTS) anyTechnique |= slot != null;
        if (anyTechnique) {
            int x0 = width / 2 + 91 + 10;
            int y = height - 22;
            for (int i = 0; i < ClientState.SLOTS.length; i++) {
                int x = x0 + i * 22;
                g.fill(x, y, x + 20, y + 20, 0x90000000);
                g.fill(x, y, x + 20, y + 1, 0x60C9B8FF);
                String id = ClientState.SLOTS[i];
                if (id != null) {
                    AbsorbableDef def = ClientState.ABSORBABLES.get(id);
                    if (def != null) g.renderItem(Lookup.stack(def.icon), x + 2, y + 2);
                    float cooldown = ClientState.cooldownFraction(id);
                    if (cooldown > 0) {
                        g.pose().pushPose();
                        g.pose().translate(0F, 0F, 200F);
                        g.fill(x, y + Math.round(20 * (1 - cooldown)), x + 20, y + 20, 0xB0101020);
                        g.pose().popPose();
                    }
                }
                String key = ClientEvents.TECHNIQUE_KEYS[i].getTranslatedKeyMessage().getString();
                g.pose().pushPose();
                g.pose().translate(0F, 0F, 210F);
                g.drawString(font, key.length() > 2 ? key.substring(0, 2) : key, x + 1, y + 1, 0xA0A0B0);
                g.pose().popPose();
            }
        }

        // L'œil de Suke : il s'ouvre à mesure que la notoriété grandit.
        if (ClientState.sukeAlive && ClientState.notoriety >= 1 && mc.level.dimension().location().equals(Crossing.DIVINIARTH)) {
            int x = width - 70;
            int y = 8;
            float t = (float) Math.min(1.0, ClientState.notoriety / 100.0);
            int red = 0x60 + Math.round(0x9F * t);
            int color = 0xFF000000 | (red << 16) | (0x40 << 8) | 0x40;
            g.fill(x, y + 4, x + 60, y + 6, 0x60000000);
            g.fill(x, y + 4, x + Math.round(60 * t), y + 6, color);
            g.drawString(font, "◉", x - 10, y, color);
        }
    }
}
