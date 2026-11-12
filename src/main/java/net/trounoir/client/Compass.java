package net.trounoir.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Barre de boussole discrète en haut de l'écran ; la route choisie y est marquée d'un losange doré. */
public final class Compass {
    private Compass() {}

    private static final int HALF = 100;          // demi-largeur en pixels
    private static final float SPAN = 110F;       // degrés visibles de chaque côté
    /** Bas des barres de boss du cadre en cours : la boussole se glisse dessous. */
    public static int bossBottom;

    public static void render(GuiGraphics g, Minecraft mc, Font font, int width) {
        int cx = width / 2;
        int y = bossBottom > 0 ? bossBottom + 2 : 3;
        bossBottom = 0;
        float heading = Mth.wrapDegrees(mc.player.getYRot() + 180F);   // 0 = nord, 90 = est

        g.fill(cx - HALF, y + 9, cx + HALF, y + 10, 0x30FFFFFF);
        g.fill(cx, y + 7, cx + 1, y + 12, 0x90FFFFFF);
        boolean fr = mc.getLanguageManager().getSelected().startsWith("fr");
        String[] names = fr ? new String[]{"N", "NE", "E", "SE", "S", "SO", "O", "NO"} : new String[]{"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        for (int deg = 0; deg < 360; deg += 15) {
            float diff = Mth.wrapDegrees(deg - heading);
            if (Math.abs(diff) > SPAN) continue;
            int x = cx + Math.round(diff / SPAN * HALF);
            int alpha = fade(diff);
            if (deg % 45 == 0) {
                String label = names[deg / 45];
                int color = (alpha << 24) | (deg == 0 ? 0xFF8A80 : 0xE8E0FF);
                small(g, font, label, x, y + 1, color);
            } else {
                g.fill(x, y + 8, x + 1, y + 10, (alpha / 2 << 24) | 0xFFFFFF);
            }
        }

        // Route : losange doré, distance dessous ; collé au bord s'il est hors champ.
        ClientState.PlaceInfo road = ClientState.roadPlace();
        if (road != null) {
            double dx = road.x() + 0.5 - mc.player.getX();
            double dz = road.z() + 0.5 - mc.player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 12) {
                ClientState.road = null;
                return;
            }
            float bearing = (float) Math.toDegrees(Math.atan2(dx, -dz));   // 0 = nord, 90 = est
            float diff = Mth.wrapDegrees(bearing - heading);
            float clamped = Mth.clamp(diff, -SPAN, SPAN);
            int x = cx + Math.round(clamped / SPAN * HALF);
            int color = 0xFFE6C14A;
            g.fill(x - 1, y + 6, x + 2, y + 13, color);
            g.fill(x - 2, y + 8, x + 3, y + 11, color);
            String text = Math.abs(diff) > SPAN ? (diff < 0 ? "◀ " : "") + Math.round(dist) + " m" + (diff > 0 ? " ▶" : "") : Math.round(dist) + " m";
            small(g, font, text, x, y + 14, 0xD0E6C14A);
        }
    }

    private static int fade(float diff) {
        float t = Math.abs(diff) / SPAN;
        return Math.max(0x10, Math.round(0xE0 * (1 - t * t)));
    }

    private static void small(GuiGraphics g, Font font, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0F);
        g.pose().scale(0.75F, 0.75F, 1F);
        g.drawString(font, text, -font.width(text) / 2, 0, color, false);
        g.pose().popPose();
    }
}
